package com.hud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public final class HudBackendApplication {

	private HudBackendApplication() {
		// Utility class
	}

	public static void main(String[] args) {
		SpringApplication.run(HudBackendApplication.class, args);
	}

}
