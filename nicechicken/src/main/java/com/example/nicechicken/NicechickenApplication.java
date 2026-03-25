package com.example.nicechicken;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@EnableAsync
@EnableScheduling
@SpringBootApplication
public class NicechickenApplication {

	public static void main(String[] args) {
		SpringApplication.run(NicechickenApplication.class, args);
	}

}
