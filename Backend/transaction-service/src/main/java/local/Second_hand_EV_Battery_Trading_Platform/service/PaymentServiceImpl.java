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

import local.Second_hand_EV_Battery_Trading_Platform.entity.Cart;
import local.Second_hand_EV_Battery_Trading_Platform.entity.Customer;
import local.Second_hand_EV_Battery_Trading_Platform.entity.Payment;
import local.Second_hand_EV_Battery_Trading_Platform.model.CustomerDTO;
import local.Second_hand_EV_Battery_Trading_Platform.model.PaymentInfoResponse;
import local.Second_hand_EV_Battery_Trading_Platform.model.PaymentRequest;
import local.Second_hand_EV_Battery_Trading_Platform.model.PaymentResponse;
import local.Second_hand_EV_Battery_Trading_Platform.repository.CartRepository;
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
    private final CartRepository cartRepo;
    private final CustomerRepository customerRepo;

    // ===== VNPay Config =====
    @Value("${vnpay.url:https://sandbox.vnpayment.vn/paymentv2/vpcpay.html}")
    private String vnp_Url;

    @Value("${vnpay.returnUrl:http://localhost:8080/api/payments/callback}")
    private String vnp_ReturnUrl;

    @Value("${vnpay.tmnCode:YOUR_TMN_CODE}")
    private String vnp_TmnCode;

    @Value("${vnpay.hashSecret:YOUR_SECRET_KEY}")
    private String vnp_HashSecret;

    // ===== MoMo Config =====
    @Value("${momo.endpoint:https://test-payment.momo.vn/v2/gateway/api/create}")
    private String momoEndpoint;

    @Value("${momo.partnerCode:MOMO}")
    private String momoPartnerCode;

    @Value("${momo.accessKey:F8BBA842ECF85}")
    private String momoAccessKey;

    @Value("${momo.secretKey:K951B6PE1waDMi640xX08PD3vg6EkVlz}")
    private String momoSecretKey;

    @Value("${momo.returnUrl:http://localhost:5501/payment_success.html}")
    private String momoReturnUrl;

    @Value("${momo.notifyUrl:https://productional-wendell-nonexotic.ngrok-free.dev/api/payments/callback}")
    private String momoNotifyUrl;

    // ======================= TẠO GIAO DỊCH =======================
    @Override
    public PaymentResponse createPayment(PaymentRequest request) {
        log.info("🔹 [DEBUG] PaymentRequest nhận được từ frontend: {}", request);

        if (request == null || request.getCustomer() == null)
            throw new RuntimeException("Dữ liệu PaymentRequest không hợp lệ");

        Cart cart = cartRepo.findById(request.getCartId())
                .orElseThrow(() -> new RuntimeException("Cart not found: ID = " + request.getCartId()));

        CustomerDTO dto = request.getCustomer();
        Customer customer = new Customer();
        customer.setFullName(dto.getFullName());
        customer.setPhone(dto.getPhone());
        customer.setEmail(dto.getEmail());
        customer.setAddress(dto.getAddress());
        customerRepo.save(customer);

        Payment p = new Payment();
        p.setCustomer(customer);
        p.setCart(cart);
        p.setAmount(cart.getTotal());
        p.setMethod(request.getPaymentMethod().toUpperCase());
        p.setStatus("PENDING");
        p.setTransactionId(UUID.randomUUID().toString());
        p.setCreatedAt(LocalDateTime.now());
        p.setUpdatedAt(LocalDateTime.now());
        paymentRepo.save(p);

        String redirectUrl = switch (request.getPaymentMethod().toLowerCase()) {
            case "vnpay" -> buildVNPayUrl(p);
            case "momo" -> buildMoMoUrl(p);
            default -> throw new RuntimeException("Phương thức thanh toán không hợp lệ!");
        };

        log.info("✅ [API] Giao dịch khởi tạo thành công → Redirect: {}", redirectUrl);
        return new PaymentResponse("PENDING", "Redirect to payment gateway", p.getTransactionId(), redirectUrl);
    }

    // ======================= TẠO URL VNPay =======================
    private String buildVNPayUrl(Payment payment) {
        try {
            Map<String, String> vnp_Params = new HashMap<>();
            vnp_Params.put("vnp_Version", "2.1.0");
            vnp_Params.put("vnp_Command", "pay");
            vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
            long amount = Math.round(payment.getAmount() * 100);
            vnp_Params.put("vnp_Amount", String.valueOf(amount));
            vnp_Params.put("vnp_CurrCode", "VND");
            vnp_Params.put("vnp_TxnRef", payment.getTransactionId());
            vnp_Params.put("vnp_OrderInfo", "Thanh toan don hang " + payment.getTransactionId());
            vnp_Params.put("vnp_OrderType", "other");
            vnp_Params.put("vnp_Locale", "vn");
            vnp_Params.put("vnp_ReturnUrl", vnp_ReturnUrl);
            vnp_Params.put("vnp_IpAddr", "127.0.0.1");
            vnp_Params.put("vnp_CreateDate",
                    LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));

            List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
            Collections.sort(fieldNames);

            StringBuilder hashData = new StringBuilder();
            StringBuilder query = new StringBuilder();

            for (Iterator<String> itr = fieldNames.iterator(); itr.hasNext();) {
                String fieldName = itr.next();
                String fieldValue = vnp_Params.get(fieldName);
                if (fieldValue != null && !fieldValue.isEmpty()) {
                    hashData.append(fieldName).append('=')
                            .append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                    query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII))
                            .append('=')
                            .append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                    if (itr.hasNext()) {
                        hashData.append('&');
                        query.append('&');
                    }
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

            String extraData = Base64.getEncoder().encodeToString("SecondHandEV".getBytes(StandardCharsets.UTF_8));

            JSONObject body = new JSONObject();
            body.put("partnerCode", momoPartnerCode);
            body.put("accessKey", momoAccessKey);
            body.put("requestId", requestId);
            body.put("amount", amount);
            body.put("orderId", orderId);
            body.put("orderInfo", "Thanh toan don hang " + orderId);
            body.put("redirectUrl", momoReturnUrl);
            body.put("ipnUrl", momoNotifyUrl);
            body.put("extraData", extraData);
            body.put("requestType", "captureWallet");
            body.put("lang", "vi");

            String rawSignature = "accessKey=" + momoAccessKey
                    + "&amount=" + amount
                    + "&extraData=" + extraData
                    + "&ipnUrl=" + momoNotifyUrl
                    + "&orderId=" + orderId
                    + "&orderInfo=Thanh toan don hang " + orderId
                    + "&partnerCode=" + momoPartnerCode
                    + "&redirectUrl=" + momoReturnUrl
                    + "&requestId=" + requestId
                    + "&requestType=captureWallet";

            String signature = hmacSHA256(rawSignature, momoSecretKey);
            body.put("signature", signature);

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(momoEndpoint))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JSONObject json = new JSONObject(response.body());
            log.info("💳 [MoMo] Response: {}", json.toString(2));

            if (json.has("payUrl")) return json.getString("payUrl");
            throw new RuntimeException("Không nhận được payUrl từ MoMo: " + json);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi tạo URL MoMo: " + e.getMessage());
        }
    }

    private String hmacSHA256(String data, String key) throws Exception {
        Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secret_key = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        sha256_HMAC.init(secret_key);
        byte[] hash = sha256_HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder result = new StringBuilder();
        for (byte b : hash) result.append(String.format("%02x", b));
        return result.toString();
    }

    // ======================= CALLBACK =======================
    @Override
    public void handleCallback(Map<String, Object> data) {
        log.info("📬 [CALLBACK] Dữ liệu nhận từ gateway: {}", data);

        String tmpTransactionId = null;
        String method = "UNKNOWN";

        // Xác định cổng thanh toán
        if (data.containsKey("vnp_TxnRef")) {
            tmpTransactionId = String.valueOf(data.get("vnp_TxnRef"));
            method = "VNPAY";
        } else if (data.containsKey("orderId")) {
            tmpTransactionId = String.valueOf(data.get("orderId"));
            method = "MOMO";
        } else if (data.containsKey("orderid")) {
            tmpTransactionId = String.valueOf(data.get("orderid"));
            method = "MOMO";
        }

        final String transactionId = tmpTransactionId;
        if (transactionId == null || transactionId.isEmpty()) {
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
                log.warn("❌ [CALLBACK] MoMo thất bại - resultCode = {}", resultCode);
            }
        } else if ("VNPAY".equalsIgnoreCase(method)) {
            String responseCode = String.valueOf(data.getOrDefault("vnp_ResponseCode", "99"));
            success = "00".equals(responseCode);
            if (!success) {
                log.warn("❌ [CALLBACK] VNPay thất bại - vnp_ResponseCode = {}", responseCode);
            }
        }

        // 🟢 Nếu thành công
        if (success) {
            p.setStatus("SUCCESS");
            p.setUpdatedAt(LocalDateTime.now());
            p.setMethod(method);
            paymentRepo.save(p);

            Cart c = p.getCart();
            c.setStatus("PAID");
            cartRepo.save(c);

            log.info("✅ [CALLBACK] Giao dịch {} thành công qua {}", transactionId, method);
        } 
        // 🔴 Nếu thất bại hoặc bị hủy
        else {
            p.setStatus("FAILED");
            p.setUpdatedAt(LocalDateTime.now());
            p.setMethod(method);
            paymentRepo.save(p);

            Cart c = p.getCart();
            c.setStatus("FAILED");
            cartRepo.save(c);

            log.warn("❌ [CALLBACK] Giao dịch {} thất bại hoặc bị hủy qua {}", transactionId, method);
        }
    }



    @Override
    public Payment findByTransactionId(String transactionId) {
        return paymentRepo.findByTransactionId(transactionId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giao dịch với mã: " + transactionId));
    }

    // ======================= 🔍 API LẤY THÔNG TIN THANH TOÁN =======================
    public PaymentInfoResponse getPaymentInfo(String transactionId) {
        Payment payment = paymentRepo.findByTransactionId(transactionId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giao dịch: " + transactionId));

        Customer customer = payment.getCustomer();
        Cart cart = payment.getCart();

        double serviceFee = 5_000_000;
        double total = cart.getPrice() + serviceFee;

        return PaymentInfoResponse.builder()
                .transactionId(payment.getTransactionId())
                .status(payment.getStatus())
                .method(payment.getMethod())
                .fullName(customer.getFullName())
                .phone(customer.getPhone())
                .email(customer.getEmail())
                .address(customer.getAddress())
                .productName(cart.getProductName())
                .price(cart.getPrice())
                .totalAmount(total)
                .build();
    }

}
