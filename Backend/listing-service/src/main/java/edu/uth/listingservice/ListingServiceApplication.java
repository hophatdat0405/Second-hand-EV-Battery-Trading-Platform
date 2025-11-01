package edu.uth.listingservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean; 
import org.springframework.web.reactive.function.client.WebClient; // Thay đổi import

@SpringBootApplication
public class ListingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ListingServiceApplication.class, args);
    }

    /**
     * Cung cấp một WebClient.Builder
     * để có thể tiêm (inject) vào các service.
     */
    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}