package com.javaisland.bank_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BankBackendApplication {
	public static void main(String[] args) {
		SpringApplication.run(BankBackendApplication.class, args);
	}
}