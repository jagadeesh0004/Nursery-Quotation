package com.nursery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

// ✅ Enable async processing for background tasks (e.g., plant initialization)
@EnableAsync
@SpringBootApplication
public class NurseryApplication {

    public static void main(String[] args) {
        // ✅ spring-dotenv automatically loads .env file from classpath
        // No manual configuration needed - it's handled by application.properties
        SpringApplication.run(NurseryApplication.class, args);
    }
}
