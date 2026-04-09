package com.mypropertyfact.estate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class MyPropertyFactApplication {

	public static void main(String[] args) {
		SpringApplication.run(MyPropertyFactApplication.class, args);
	}

}

