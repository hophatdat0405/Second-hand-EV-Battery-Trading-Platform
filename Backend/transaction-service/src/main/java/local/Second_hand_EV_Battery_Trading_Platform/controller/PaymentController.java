package local.Second_hand_EV_Battery_Trading_Platform.controller;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import local.Second_hand_EV_Battery_Trading_Platform.entity.Cart;
import local.Second_hand_EV_Battery_Trading_Platform.entity.Customer;
import local.Second_hand_EV_Battery_Trading_Platform.entity.Payment;
import local.Second_hand_EV_Battery_Trading_Platform.model.PaymentRequest;
import local.Second_hand_EV_Battery_Trading_Platform.model.PaymentResponse;
import local.Second_hand_EV_Battery_Trading_Platform.service.PaymentService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);
    private final PaymentService paymentService;
    private final ObjectMapper mapper = new ObjectMapper();

    // ========================= TẠO THANH TOÁN =========================
    @PostMapping("/create")
    public ResponseEntity<PaymentResponse> createPayment(@RequestBody PaymentRequest request) {
        log.info("🧾 [API] Nhận yêu cầu tạo thanh toán: {}", request);
        PaymentResponse response = paymentService.createPayment(request);
        log.info("✅ [API] Giao dịch khởi tạo thành công, redirect URL: {}", response.getRedirectUrl());
        return ResponseEntity.ok(response);
    }

    // ========================= CALLBACK VNPay / MoMo =========================
    @RequestMapping(value = "/callback", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<?> handleCallback(
            @RequestParam(required = false) Map<String, String> params,
            @RequestBody(required = false) String rawBody) {

        Map<String, Object> data = new HashMap<>();
        String transactionId = null;
        String method = "UNKNOWN";

        try {
            // ===== 1️⃣ Callback POST (MoMo ipnUrl) =====
            if (rawBody != null && !rawBody.isEmpty()) {
                try {
                    data.putAll(mapper.readValue(rawBody, new TypeReference<Map<String, Object>>() {}));
                } catch (Exception e) {
                    log.warn("⚠️ [CALLBACK] Body không phải JSON hợp lệ, bỏ qua parse: {}", rawBody);
                }
            }

            // ===== 2️⃣ Callback GET (VNPay / MoMo redirect) =====
            if (params != null && !params.isEmpty()) {
                data.putAll(params);
            }

            log.info("📩 [CALLBACK] Nhận dữ liệu callback từ gateway: {}", data);

            // ===== 3️⃣ Xác định loại cổng =====
            if (data.containsKey("vnp_TxnRef")) {
                transactionId = String.valueOf(data.get("vnp_TxnRef"));
                method = "VNPAY";
            } else if (data.containsKey("orderId")) {
                transactionId = String.valueOf(data.get("orderId"));
                method = "MOMO";
            } else if (data.containsKey("orderid")) {
                transactionId = String.valueOf(data.get("orderid"));
                method = "MOMO";
            } else if (params != null && params.containsKey("orderId")) {
                transactionId = params.get("orderId");
                method = "MOMO";
            }

            if (transactionId == null || transactionId.isEmpty()) {
                throw new IllegalArgumentException("Không tìm thấy transactionId trong callback!");
            }

            // ===== 4️⃣ Cập nhật trạng thái thanh toán =====
            paymentService.handleCallback(data);
            log.info("✅ [CALLBACK] Cập nhật trạng thái thành công cho transactionId={} (method={})",
                    transactionId, method);

            // ===== 5️⃣ Redirect về frontend =====
            HttpHeaders headers = new HttpHeaders();
            headers.add("Location",
                    "http://localhost:5501/payment_success.html?transactionId=" + transactionId + "&method=" + method);
            return ResponseEntity.status(302).headers(headers).build();

        } catch (IllegalArgumentException e) {
            log.error("⚠️ [CALLBACK] Thiếu dữ liệu callback: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));

        } catch (Exception e) {
            log.error("❌ [CALLBACK] Lỗi khi xử lý callback:", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }

    // ========================= LẤY THÔNG TIN THANH TOÁN =========================
    @GetMapping("/info/{transactionId}")
    public ResponseEntity<?> getPaymentInfo(@PathVariable String transactionId) {
        log.info("🔎 [INFO] Truy vấn thông tin thanh toán transactionId={}", transactionId);
        try {
            Payment payment = paymentService.findByTransactionId(transactionId);
            Customer c = payment.getCustomer();
            Cart cart = payment.getCart();

            double serviceFee = 5_000_000;
            double totalAmount = (cart.getPrice() != null ? cart.getPrice() : 0) + serviceFee;

            Map<String, Object> result = new HashMap<>();
            result.put("transactionId", payment.getTransactionId());
            result.put("status", payment.getStatus());
            result.put("method", payment.getMethod());
            result.put("amount", payment.getAmount());
            result.put("fullName", c.getFullName());
            result.put("phone", c.getPhone());
            result.put("email", c.getEmail());
            result.put("address", c.getAddress());
            result.put("productName", cart.getProductName());
            result.put("price", cart.getPrice());
            result.put("totalAmount", totalAmount);

            log.info("✅ [INFO] Lấy thông tin thanh toán thành công cho {}", transactionId);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("❌ [INFO] Lỗi khi truy vấn thanh toán:", e);
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }
}
