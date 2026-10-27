package com.example.recruitment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@ConfigurationPropertiesScan
public class RecruitmentBeApplication {

	public static void main(String[] args) {
		SpringApplication.run(RecruitmentBeApplication.class, args);
	}

}
