package com.testbyte.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableAsync;

// UserDetailsServiceAutoConfiguration is excluded because auth is handled with our own
// JWT issuing/parsing (AuthService/JwtService), not Spring Security's UserDetailsService.
// EnableAsync backs EmailService's @Async methods, so a slow/unreachable SMTP server
// can never block the request thread that triggered the email.
@EnableAsync
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class BackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}

}
