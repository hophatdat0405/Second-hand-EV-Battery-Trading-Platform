package com.example.compare_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class CompareServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(CompareServiceApplication.class, args);
	}

}
