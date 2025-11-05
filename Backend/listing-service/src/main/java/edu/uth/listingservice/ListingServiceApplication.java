package edu.uth.listingservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching; // <-- 1. THÊM IMPORT
import org.springframework.context.annotation.Bean;


@SpringBootApplication
@EnableCaching // <-- 2. THÊM ANNOTATION NÀY
public class ListingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ListingServiceApplication.class, args);
    }
}