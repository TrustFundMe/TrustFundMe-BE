package com.trustfund;

import io.github.cdimascio.dotenv.Dotenv;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
@OpenAPIDefinition(info = @Info(title = "Identity Service API", version = "1.0", description = "Authentication and Authorization Service for TrustFundME"))
@SecurityScheme(
    name = "bearerAuth",
    scheme = "bearer",
    type = SecuritySchemeType.HTTP,
    bearerFormat = "JWT",
    in = SecuritySchemeIn.HEADER
)
public class IdentityServiceApplication {
    public static void main(String[] args) {
        // Load .env file từ thư mục root của dự án
        // Dotenv sẽ tự động tìm file .env từ thư mục hiện tại lên đến root
        try {
            Dotenv dotenv = Dotenv.configure()
                    .directory("../") // Tìm .env ở thư mục cha (root của dự án)
                    .ignoreIfMissing() // Không báo lỗi nếu không tìm thấy
                    .load();
            
            // Load tất cả các biến vào System properties để Spring Boot có thể đọc
            dotenv.entries().forEach(entry -> {
                System.setProperty(entry.getKey(), entry.getValue());
            });
        } catch (Exception e) {
            System.err.println("Warning: Could not load .env file: " + e.getMessage());
            System.err.println("Make sure .env file exists in the project root directory.");
        }
        
        SpringApplication.run(IdentityServiceApplication.class, args);
    }
}


