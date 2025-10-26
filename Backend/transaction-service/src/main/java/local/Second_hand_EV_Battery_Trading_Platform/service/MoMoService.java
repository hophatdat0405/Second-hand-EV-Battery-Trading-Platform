package local.Second_hand_EV_Battery_Trading_Platform.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class MoMoService {

    @Value("${momo.partnerCode}")
    private String partnerCode;

    @Value("${momo.accessKey}")
    private String accessKey;

    @Value("${momo.secretKey}")
    private String secretKey;

    @Value("${momo.endpoint}")
    private String endpoint;

    @Value("${momo.returnUrl}")
    private String returnUrl;

    @Value("${momo.notifyUrl}")
    private String notifyUrl;

    @Value("${momo.requestType}")
    private String requestType;

    public String createPaymentUrl(long amount, String orderId) {
        try {
            // === 1️⃣ Tạo requestId duy nhất mỗi lần gửi ===
            String requestId = UUID.randomUUID().toString();

            // === 2️⃣ Chuẩn bị dữ liệu gửi đi ===
            JSONObject body = new JSONObject();
            body.put("partnerCode", partnerCode);
            body.put("accessKey", accessKey);
            body.put("requestId", requestId);
            body.put("amount", amount);
            body.put("orderId", orderId);
            body.put("orderInfo", "Thanh toán đơn hàng " + orderId);
            body.put("redirectUrl", returnUrl);
            body.put("ipnUrl", notifyUrl);
            body.put("requestType", requestType);
            body.put("lang", "vi");
            body.put("extraData", "");

            // === 3️⃣ Tạo chữ ký HMAC SHA256 ===
            String rawSignature =
                    "accessKey=" + accessKey +
                    "&amount=" + amount +
                    "&extraData=" + "" +
                    "&ipnUrl=" + notifyUrl +
                    "&orderId=" + orderId +
                    "&orderInfo=Thanh toán đơn hàng " + orderId +
                    "&partnerCode=" + partnerCode +
                    "&redirectUrl=" + returnUrl +
                    "&requestId=" + requestId +
                    "&requestType=" + requestType;

            String signature = hmacSHA256(rawSignature, secretKey);
            body.put("signature", signature);

            // === 4️⃣ Gửi request tới MoMo sandbox ===
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JSONObject json = new JSONObject(response.body());

            // === 5️⃣ Kiểm tra phản hồi từ MoMo ===
            if (json.has("resultCode") && json.getInt("resultCode") != 0) {
                throw new RuntimeException("MoMo trả lỗi: resultCode=" + json.getInt("resultCode") +
                        ", message=" + json.optString("message"));
            }

            if (json.has("payUrl")) {
                return json.getString("payUrl");
            } else {
                throw new RuntimeException("Không nhận được payUrl từ MoMo: " + json);
            }

        } catch (Exception e) {
            throw new RuntimeException("❌ Không thể tạo URL thanh toán MoMo", e);
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
}
