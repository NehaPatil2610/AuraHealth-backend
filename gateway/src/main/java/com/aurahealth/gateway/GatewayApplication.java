package com.aurahealth.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        // Explicitly force the underlying Java network stack to prefer standard IPv4 routes
        System.setProperty("java.net.preferIPv4Stack", "true");

        SpringApplication.run(GatewayApplication.class, args);
    }
}