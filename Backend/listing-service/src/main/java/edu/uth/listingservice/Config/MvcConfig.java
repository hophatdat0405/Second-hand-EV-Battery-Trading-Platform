package edu.uth.listingservice.Config; // Đảm bảo package này đúng

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class MvcConfig implements WebMvcConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(MvcConfig.class);

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Lấy đường dẫn đến thư mục gốc của project một cách tự động
        Path projectDir = Paths.get(System.getProperty("user.dir"));
        // Tạo đường dẫn tuyệt đối đến thư mục 'uploads'
        Path uploadDir = projectDir.resolve("uploads");
        
        // Chuyển đổi sang định dạng URI mà Spring Boot hiểu được (ví dụ: "file:/D:/path/to/uploads/")
        String resourceLocation = uploadDir.toUri().toString();

        // In ra log để kiểm tra lần cuối
        logger.info("================== MvcConfig FINAL CHECK ==================");
        logger.info("Đường dẫn vật lý đang được đăng ký: " + uploadDir.toAbsolutePath());
        logger.info("Resource Location URI được sử dụng: " + resourceLocation);
        logger.info("==========================================================");

        // Cấu hình resource handler
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(resourceLocation);
    }
}