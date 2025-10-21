package com.example.like_service;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry){
        registry.addMapping("/api/**") //Áp dụng cho tất cả các endpoint bắt đầu bằng /api
        .allowedOrigins("http://127.0.0.1:5500", "http://127.0.0.1:5501")
        .allowedMethods("GET", "POST", "PUT","DELETE","OPTIONS") //cho phép tất cả phương thức cần thiết
        .allowedHeaders("*") //cho phép tất cả header
        .allowCredentials(false);
    }

}
