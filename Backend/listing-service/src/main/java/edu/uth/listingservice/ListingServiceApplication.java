package edu.uth.listingservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean; // ✅ THÊM IMPORT
import org.springframework.web.client.RestTemplate; // ✅ THÊM IMPORT

@SpringBootApplication
public class ListingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ListingServiceApplication.class, args);
    }

    // ✅ THÊM BEAN NÀY
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}