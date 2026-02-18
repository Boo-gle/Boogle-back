package com.boogle.boogle;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class BoogleApplication {

	public static void main(String[] args) {
		SpringApplication.run(BoogleApplication.class, args);
	}

}
