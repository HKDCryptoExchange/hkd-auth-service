package com.hkd.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * HKD Auth Service Application
 * 提供HTTP REST API (8013) 和 gRPC Server (9013)
 *
 * @author HKD Team
 * @since 1.0.0
 */
@SpringBootApplication
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
