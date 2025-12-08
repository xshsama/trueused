package com.xsh.trueused;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TrueusedApplication {

	public static void main(String[] args) {
		SpringApplication.run(TrueusedApplication.class, args);
	}
}
