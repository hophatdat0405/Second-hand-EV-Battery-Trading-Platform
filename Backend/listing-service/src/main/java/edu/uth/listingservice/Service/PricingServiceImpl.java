package edu.uth.listingservice.Service; // Gói của bạn (dựa trên các file đã gửi)

import edu.uth.listingservice.DTO.PricingRequestDTO;
import edu.uth.listingservice.DTO.PricingResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class PricingServiceImpl implements PricingService {
    private final Logger logger = LoggerFactory.getLogger(PricingServiceImpl.class);
    private final RestTemplate restTemplate = new RestTemplate();

    // ⭐⭐⭐ ĐÃ SỬA LỖI TẠI ĐÂY ⭐⭐⭐
    // URL này trỏ đến service Python (app.py) đang chạy trên máy local của bạn
    private final String mlApiUrl = "http://localhost:5000/predict";

    private static final long MINIMUM_PRICE = 200_000L;

    @Override
    public PricingResponseDTO getSuggestedPrice(PricingRequestDTO dto) {
        HttpEntity<PricingRequestDTO> request = new HttpEntity<>(dto);
        try {
            // Service Java sẽ gọi đến "http://localhost:5000/predict"
            PricingResponseDTO response = restTemplate.postForObject(mlApiUrl, request, PricingResponseDTO.class);

            if (response != null && response.getSuggestedPrice() != null) {
                if (response.getSuggestedPrice() < MINIMUM_PRICE) {
                    logger.warn("ML API trả về giá quá thấp ({}). Điều chỉnh về giá tối thiểu.", response.getSuggestedPrice());
                    return new PricingResponseDTO(MINIMUM_PRICE);
                }
                // Nếu gọi thành công, trả về giá từ AI
                return response;
            } else {
                logger.warn("ML API không trả về dữ liệu. Dùng giá tạm.");
                // (Tôi cũng đã sửa lỗi chính tả 'PricingDtpTO' thành 'PricingResponseDTO' ở đây cho bạn)
                return new PricingResponseDTO(MINIMUM_PRICE * 2); // 400_000L
            }

        } catch (Exception e) {
            // Nếu service Python không chạy, nó sẽ báo lỗi và trả về 400k
            logger.error("LỖI khi gọi ML API: {}", e.getMessage());
            return new PricingResponseDTO(MINIMUM_PRICE * 2); // 400_000L
        }
    }
}