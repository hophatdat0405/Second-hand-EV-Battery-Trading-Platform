package com.example.compare_service;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    //WebMvcConfigurer: interface trong Spring MVC,
    //cho phép bạn tùy chỉnh cấu hình mặc định của Spring Web MVC
    @Override
    //hàm khai báo quy tắc CORS cho server
    //quy định những domain nào được phép gọi API từ bên ngoài
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**") // Áp dụng cho tất cả các endpoint bắt đầu bằng /api
            .allowedOrigins("http://127.0.0.1:5500", "http://127.0.0.1:5501" ) // Cho phép các địa chỉ frontend này
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // Cho phép tất cả các phương thức cần thiết
            .allowedHeaders("*") // Cho phép tất cả các header
            .allowCredentials(false);
    }
}
