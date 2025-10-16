package edu.uth.listingservice.Config; // Đảm bảo package này đúng

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**") // Áp dụng cho tất cả các endpoint bắt đầu bằng /api
            .allowedOrigins("http://127.0.0.1:5501", "http://localhost:3000") // Cho phép các địa chỉ frontend này
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // Cho phép tất cả các phương thức cần thiết
            .allowedHeaders("*") // Cho phép tất cả các header
            .allowCredentials(true);
    }
}
