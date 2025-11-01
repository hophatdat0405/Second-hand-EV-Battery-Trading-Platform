package local.contract.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import local.contract.entity.Contract;
import local.contract.model.ContractRequest;
import local.contract.model.ContractResponse;
import local.contract.repository.ContractRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContractServiceImpl implements ContractService {

    private final ContractRepository contractRepo;

    // 🔗 URL gốc của transaction-service
    @Value("${transaction.service.url:http://localhost:8083}")
    private String transactionServiceBaseUrl;

    // ============================================================
    // 1️⃣ Dùng cho MQ (tự động tạo hợp đồng khi order.paid)
    // ============================================================
    @Override
    public ContractResponse createContract(ContractRequest request) {
        try {
            log.info("📩 [ContractService] Nhận yêu cầu tạo hợp đồng tự động cho transactionId={}", request.getTransactionId());

            // Lấy thông tin giao dịch từ transaction-service
            String apiUrl = transactionServiceBaseUrl + "/api/payments/info/" + request.getTransactionId();
            log.info("🔗 [ContractService] Gọi API transaction-service: {}", apiUrl);

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .timeout(Duration.ofSeconds(10))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            log.info("📥 Phản hồi từ transaction-service [{}]: {}", response.statusCode(), response.body());

            if (response.statusCode() != 200) {
                throw new RuntimeException("Không thể lấy thông tin thanh toán từ transaction-service (" +
                        response.statusCode() + ")");
            }

            JSONObject json = new JSONObject(response.body());

            // Tạo entity contract
            Contract ct = new Contract();
            ct.setTransactionId(json.optString("transactionId"));
            ct.setCustomerName(json.optString("fullName"));
            ct.setCustomerPhone(json.optString("phone"));
            ct.setCustomerEmail(json.optString("email"));
            ct.setCustomerAddress(json.optString("address"));
            ct.setPaymentMethod(request.getMethod());
            ct.setPdfUrl("https://example.com/contracts/" + UUID.randomUUID() + ".pdf");

            contractRepo.save(ct);
            log.info("✅ [ContractService] Đã tạo hợp đồng tự động cho transactionId={}", request.getTransactionId());

            String now = java.time.LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            return ContractResponse.builder()
                    .message("✅ Hợp đồng tự động tạo thành công (MQ event).")
                    .transactionId(request.getTransactionId())
                    .pdfUrl(ct.getPdfUrl())
                    .signedAt(now)
                    .build();

        } catch (Exception e) {
            log.error("❌ [ContractService] Lỗi khi tạo hợp đồng tự động: {}", e.getMessage(), e);
            throw new RuntimeException("Lỗi khi tạo hợp đồng tự động: " + e.getMessage(), e);
        }
    }

    // ============================================================
    // 2️⃣ Dùng cho người dùng ký hợp đồng thủ công
    // ============================================================
    @Override
    public ContractResponse signContract(ContractRequest request) {
        try {
            // 🧾 Gọi API transaction-service để lấy thông tin thanh toán
            String apiUrl = transactionServiceBaseUrl + "/api/payments/info/" + request.getTransactionId();
            log.info("🔗 [ContractService] Gọi API transaction-service: {}", apiUrl);

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .timeout(Duration.ofSeconds(10))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            log.info("📥 Phản hồi từ transaction-service [{}]: {}", response.statusCode(), response.body());

            if (response.statusCode() != 200) {
                throw new RuntimeException("Không thể lấy thông tin thanh toán từ transaction-service (" +
                        response.statusCode() + ")");
            }

            JSONObject json = new JSONObject(response.body());
            ContractRequest info = new ContractRequest();
            info.setTransactionId(json.optString("transactionId"));
            info.setStatus(json.optString("status"));
            info.setMethod(json.optString("method"));
            info.setFullName(json.optString("fullName"));
            info.setPhone(json.optString("phone"));
            info.setEmail(json.optString("email"));
            info.setAddress(json.optString("address"));
            info.setSignature(request.getSignature());

            if (!"SUCCESS".equalsIgnoreCase(info.getStatus())) {
                log.warn("⚠️ Giao dịch {} chưa hoàn tất — trạng thái: {}", info.getTransactionId(), info.getStatus());
                return ContractResponse.builder()
                        .message("❌ Không thể ký hợp đồng — thanh toán chưa hoàn tất.")
                        .pdfUrl("https://example.com/contracts/failed/" + UUID.randomUUID())
                        .transactionId(info.getTransactionId())
                        .signedAt(null)
                        .build();
            }

            Contract ct = new Contract();
            ct.setTransactionId(info.getTransactionId());
            ct.setSignature(info.getSignature());
            ct.setCustomerName(info.getFullName());
            ct.setCustomerPhone(info.getPhone());
            ct.setCustomerEmail(info.getEmail());
            ct.setCustomerAddress(info.getAddress());
            ct.setPaymentMethod(info.getMethod());
            ct.setPdfUrl("https://example.com/contracts/" + UUID.randomUUID() + ".pdf");

            contractRepo.save(ct);
            log.info("✅ Hợp đồng đã được lưu thành công cho khách hàng: {}", info.getFullName());

            String now = java.time.LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            return ContractResponse.builder()
                    .message("✅ Hợp đồng ký thành công cho " + info.getFullName() +
                            " (Phương thức: " + info.getMethod().toUpperCase() + ")")
                    .pdfUrl(ct.getPdfUrl())
                    .transactionId(info.getTransactionId())
                    .signedAt(now)
                    .build();

        } catch (Exception e) {
            log.error("❌ Lỗi khi ký hợp đồng: {}", e.getMessage(), e);
            throw new RuntimeException("Lỗi khi ký hợp đồng: " + e.getMessage(), e);
        }
    }
}
