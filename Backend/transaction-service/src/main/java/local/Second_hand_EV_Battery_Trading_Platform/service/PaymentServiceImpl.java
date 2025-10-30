package local.Second_hand_EV_Battery_Trading_Platform.service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import local.Second_hand_EV_Battery_Trading_Platform.client.CartServiceClient;
import local.Second_hand_EV_Battery_Trading_Platform.entity.Customer;
import local.Second_hand_EV_Battery_Trading_Platform.entity.Payment;
import local.Second_hand_EV_Battery_Trading_Platform.model.CartDTO;
import local.Second_hand_EV_Battery_Trading_Platform.model.CustomerDTO;
import local.Second_hand_EV_Battery_Trading_Platform.model.PaymentInfoResponse;
import local.Second_hand_EV_Battery_Trading_Platform.model.PaymentRequest;
import local.Second_hand_EV_Battery_Trading_Platform.model.PaymentResponse;
import local.Second_hand_EV_Battery_Trading_Platform.mq.MQPublisher;
import local.Second_hand_EV_Battery_Trading_Platform.repository.CustomerRepository;
import local.Second_hand_EV_Battery_Trading_Platform.repository.PaymentRepository;
import local.Second_hand_EV_Battery_Trading_Platform.utils.VNPayUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepo;
    private final CustomerRepository customerRepo;
    private final CartServiceClient cartClient; // NEW
    private final MQPublisher mqPublisher; // 🟢 THÊM DÒNG NÀY

    // ===== VNPay Config =====
    @Value("${vnpay.url:https://sandbox.vnpayment.vn/paymentv2/vpcpay.html}")
    private String vnp_Url;

    @Value("${vnpay.returnUrl:http://localhost:8083/api/payments/callback}")
    private String vnp_ReturnUrl;

    @Value("${vnpay.tmnCode:YOUR_TMN_CODE}")
    private String vnp_TmnCode;

    @Value("${vnpay.hashSecret:YOUR_SECRET_KEY}")
    private String vnp_HashSecret;

    // ===== MoMo Config =====
    @Value("${momo.endpoint:https://test-payment.momo.vn/v2/gateway/api/create}")
    private String momoEndpoint;

    @Value("${momo.partnerCode:MOMOQTST20251020_TEST}")
    private String momoPartnerCode;

    @Value("${momo.accessKey:Cy4ZAIlh0TwMJtMP}")
    private String momoAccessKey;

    @Value("${momo.secretKey:eSAqVmVyvDwzcj2uZxkwRjAdz3nrtNpo}")
    private String momoSecretKey;

    @Value("${momo.returnUrl:http://localhost:5501/payment_success.html}")
    private String momoReturnUrl;

    @Value("${momo.notifyUrl:https://productional-wendell-nonexotic.ngrok-free.dev/api/payments/callback}")
    private String momoNotifyUrl;

    @Value("${momo.requestType:captureWallet}")
    private String momoRequestType;

    // ======================= TẠO GIAO DỊCH =======================
    @Override
    public PaymentResponse createPayment(PaymentRequest request) {
        log.info("🔹 [DEBUG] PaymentRequest nhận: {}", request);

        if (request == null || request.getCustomer() == null)
            throw new RuntimeException("Dữ liệu PaymentRequest không hợp lệ");

        // ✅ Kiểm tra danh sách cartIds
        if (request.getCartIds() == null || request.getCartIds().isEmpty())
            throw new RuntimeException("Danh sách cartIds trống!");

        // ✅ Lưu customer
        CustomerDTO dto = request.getCustomer();
        Customer customer = new Customer();
        customer.setFullName(dto.getFullName());
        customer.setPhone(dto.getPhone());
        customer.setEmail(dto.getEmail());
        customer.setAddress(dto.getAddress());
        customerRepo.save(customer);

        // ✅ Tổng hợp thông tin sản phẩm từ cart-service
        double totalPrice = 0d;
        StringBuilder productList = new StringBuilder();

        for (Long id : request.getCartIds()) {
            try {
                CartDTO cart = cartClient.getCartById(id);
                if (cart != null) {
                    totalPrice += cart.getPrice();
                    productList.append(cart.getProductName()).append(", ");
                }
            } catch (Exception e) {
                log.warn("⚠️ Không thể lấy thông tin giỏ hàng ID={} : {}", id, e.getMessage());
            }
        }

        // Loại bỏ dấu phẩy cuối nếu có
        String products = productList.length() > 0
                ? productList.substring(0, productList.length() - 2)
                : "Không có";

        // ✅ Tạo payment và lưu thông tin ngay trong DB
        Payment p = new Payment();
        p.setCustomer(customer);
        p.setCartIdList(request.getCartIds());
        p.setAmount(totalPrice); // tổng tiền tính từ cart
        p.setProductNames(products); // danh sách sản phẩm
        p.setTotalAmount(totalPrice); // tổng tiền (không cộng phí dịch vụ)
        p.setMethod(request.getPaymentMethod().toUpperCase());
        p.setStatus("PENDING");
        p.setTransactionId(UUID.randomUUID().toString());
        p.setCreatedAt(LocalDateTime.now());
        p.setUpdatedAt(LocalDateTime.now());
        paymentRepo.save(p);

        log.info("💾 [PaymentService] Đã lưu giao dịch: {} | Sản phẩm: {} | Tổng: {}đ",
                p.getTransactionId(), products, totalPrice);

        // ✅ Chọn cổng thanh toán (VNPay / MoMo)
        String redirectUrl = switch (request.getPaymentMethod().toLowerCase()) {
            case "vnpay" -> buildVNPayUrl(p);
            case "momo"  -> buildMoMoUrl(p);
            default      -> throw new RuntimeException("Phương thức thanh toán không hợp lệ!");
        };

        return new PaymentResponse("PENDING",
                "Redirect to payment gateway",
                p.getTransactionId(),
                redirectUrl);
    }

    // ======================= TẠO URL VNPay =======================
    private String buildVNPayUrl(Payment payment) {
        try {
            Map<String, String> vnp_Params = new HashMap<>();
            vnp_Params.put("vnp_Version", "2.1.0");
            vnp_Params.put("vnp_Command", "pay");
            vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
            vnp_Params.put("vnp_CurrCode", "VND");
            vnp_Params.put("vnp_TxnRef", payment.getTransactionId());
            vnp_Params.put("vnp_OrderInfo", "Thanh toan don hang " + payment.getTransactionId());
            vnp_Params.put("vnp_OrderType", "other");
            vnp_Params.put("vnp_Locale", "vn");
            vnp_Params.put("vnp_ReturnUrl", vnp_ReturnUrl);
            vnp_Params.put("vnp_IpAddr", "127.0.0.1");
            vnp_Params.put("vnp_CreateDate",
                LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));

            long amount = Math.round((payment.getAmount() == null ? 0d : payment.getAmount()) * 100);
            vnp_Params.put("vnp_Amount", String.valueOf(amount));

            List<String> names = new ArrayList<>(vnp_Params.keySet());
            Collections.sort(names);
            StringBuilder hashData = new StringBuilder();
            StringBuilder query = new StringBuilder();
            for (Iterator<String> it = names.iterator(); it.hasNext();) {
                String name = it.next();
                String value = vnp_Params.get(name);
                if (value != null && !value.isEmpty()) {
                    hashData.append(name).append('=')
                            .append(URLEncoder.encode(value, StandardCharsets.US_ASCII));
                    query.append(URLEncoder.encode(name, StandardCharsets.US_ASCII))
                         .append('=')
                         .append(URLEncoder.encode(value, StandardCharsets.US_ASCII));
                    if (it.hasNext()) { hashData.append('&'); query.append('&'); }
                }
            }
            String secureHash = VNPayUtils.hmacSHA512(vnp_HashSecret, hashData.toString());
            query.append("&vnp_SecureHash=").append(secureHash);
            return vnp_Url + "?" + query;
        } catch (Exception e) {
            throw new RuntimeException("❌ Lỗi tạo URL VNPay: " + e.getMessage());
        }
    }

    // ======================= TẠO URL MoMo =======================
    private String buildMoMoUrl(Payment payment) {
        try {
            String orderId = payment.getTransactionId();
            String requestId = orderId;
            long amount = payment.getAmount() != null ? payment.getAmount().longValue() : 0L;

            String extraData = Base64.getEncoder()
                .encodeToString("SecondHandEV".getBytes(StandardCharsets.UTF_8));

            String amountStr = String.valueOf(amount);
            String orderInfo = "Thanh toan don hang " + orderId;

            JSONObject body = new JSONObject();
            body.put("partnerCode", momoPartnerCode);
            body.put("accessKey", momoAccessKey);
            body.put("requestId", requestId);
            body.put("amount", amountStr);
            body.put("orderId", orderId);
            body.put("orderInfo", orderInfo);
            body.put("redirectUrl", momoReturnUrl);
            body.put("ipnUrl", momoNotifyUrl);
            body.put("extraData", extraData);
            body.put("requestType", momoRequestType);
            body.put("lang", "vi");

            String rawSignature =
                    "accessKey=" + momoAccessKey +
                    "&amount=" + amountStr +
                    "&extraData=" + extraData +
                    "&ipnUrl=" + momoNotifyUrl +
                    "&orderId=" + orderId +
                    "&orderInfo=" + orderInfo +
                    "&partnerCode=" + momoPartnerCode +
                    "&redirectUrl=" + momoReturnUrl +
                    "&requestId=" + requestId +
                    "&requestType=" + momoRequestType;

            String signature = hmacSHA256(rawSignature, momoSecretKey);
            body.put("signature", signature);

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(momoEndpoint))
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
            JSONObject json = new JSONObject(res.body());
            log.info("💳 [MoMo] Response: {}", json.toString(2));

            if (json.has("payUrl")) return json.getString("payUrl");
            throw new RuntimeException("Không nhận được payUrl từ MoMo: " + json);

        } catch (Exception e) {
            throw new RuntimeException("Lỗi tạo URL MoMo: " + e.getMessage());
        }
    }

    private String hmacSHA256(String data, String key) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    // ======================= CALLBACK =======================
    @Override
    public void handleCallback(Map<String, Object> data) {
        log.info("📬 [CALLBACK] Dữ liệu nhận từ gateway: {}", data);

        // ✅ Khai báo final ngay từ đầu, không gán lại → tránh lỗi "effectively final"
        final String transactionId;
        final String method;

        if (data.containsKey("vnp_TxnRef")) {
            transactionId = String.valueOf(data.get("vnp_TxnRef"));
            method = "VNPAY";
        } else if (data.containsKey("orderId")) {
            transactionId = String.valueOf(data.get("orderId"));
            method = "MOMO";
        } else if (data.containsKey("orderid")) {
            transactionId = String.valueOf(data.get("orderid"));
            method = "MOMO";
        } else {
            throw new RuntimeException("Không tìm thấy mã giao dịch (transactionId)");
        }

        Payment p = paymentRepo.findByTransactionId(transactionId)
                .orElseThrow(() -> new RuntimeException("Payment not found in DB: " + transactionId));

        if ("SUCCESS".equalsIgnoreCase(p.getStatus())) {
            log.warn("⚠️ [CALLBACK] Giao dịch {} đã SUCCESS trước đó, bỏ qua callback trùng.", transactionId);
            return;
        }

        boolean success = false;

        // 🟣 Kiểm tra theo từng cổng thanh toán
        if ("MOMO".equalsIgnoreCase(method)) {
            int resultCode = Integer.parseInt(String.valueOf(data.getOrDefault("resultCode", "-1")));
            success = (resultCode == 0);
            if (!success) {
                log.warn("❌ [CALLBACK] MoMo thất bại - resultCode={}", resultCode);
            }
        } else if ("VNPAY".equalsIgnoreCase(method)) {
            String responseCode = String.valueOf(data.getOrDefault("vnp_ResponseCode", "99"));
            success = "00".equals(responseCode);
            if (!success) {
                log.warn("❌ [CALLBACK] VNPay thất bại - vnp_ResponseCode={}", responseCode);
            }
        }

        if (success) {
            p.setStatus("SUCCESS");
            p.setUpdatedAt(LocalDateTime.now());
            p.setMethod(method);
            paymentRepo.save(p);

            // ✅ Xóa tất cả cart sau khi thanh toán thành công
            List<Long> cartList = p.getCartIdList();
            if (cartList != null && !cartList.isEmpty()) {
                for (Long id : cartList) {
                    try {
                        cartClient.deleteCart(id);
                    } catch (Exception ex) {
                        log.warn("Không xóa được giỏ hàng ID={} : {}", id, ex.getMessage());
                    }
                }
            }



            log.info("✅ [CALLBACK] Giao dịch {} thành công qua {}", transactionId, method);
            // 📨 Gửi sự kiện MQ
            Map<String, Object> event = Map.of(
                "transactionId", transactionId,
                "status", "SUCCESS",
                "method", method,
                "amount", p.getAmount(),
                "time", LocalDateTime.now().toString()
            );
            mqPublisher.publish("order.paid", event);

        } else {
            p.setStatus("FAILED");
            p.setUpdatedAt(LocalDateTime.now());
            p.setMethod(method);
            paymentRepo.save(p);
            log.warn("❌ [CALLBACK] Giao dịch {} thất bại hoặc bị hủy qua {}", transactionId, method);
            Map<String, Object> event = Map.of(
                "transactionId", transactionId,
                "status", "FAILED",
                "method", method,
                "time", LocalDateTime.now().toString()
            );
            mqPublisher.publish("order.failed", event);
        }
    }
    
    @Override
    public Payment findByTransactionId(String transactionId) {
        return paymentRepo.findByTransactionId(transactionId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giao dịch với mã: " + transactionId));
    }



    // ======================= 🔍 INFO =======================
    @Override
    public PaymentInfoResponse getPaymentInfo(String transactionId) {
        // 🔍 Tìm giao dịch theo transactionId
        Payment payment = paymentRepo.findByTransactionId(transactionId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giao dịch: " + transactionId));

        Customer customer = payment.getCustomer();
        if (customer == null) {
            throw new RuntimeException("Không tìm thấy thông tin khách hàng cho giao dịch: " + transactionId);
        }

        // 🧾 Lấy dữ liệu trực tiếp từ bảng payment (đã lưu sẵn)
        String productList = payment.getProductNames() != null
                ? payment.getProductNames()
                : "Không có sản phẩm";
        double totalPrice = payment.getTotalAmount() != null ? payment.getTotalAmount() : 0d;

        // ✅ Trả kết quả chi tiết về giao dịch
        return PaymentInfoResponse.builder()
                .transactionId(payment.getTransactionId())
                .status(payment.getStatus())
                .method(payment.getMethod())
                .fullName(customer.getFullName())
                .phone(customer.getPhone())
                .email(customer.getEmail())
                .address(customer.getAddress())
                .productName(productList)
                .price(totalPrice)
                .totalAmount(totalPrice)
                .build();
    }



}
