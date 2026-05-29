package com.hud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@SuppressWarnings("PMD.UseUtilityClass")
public class HudBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(HudBackendApplication.class, args);
	}

}
