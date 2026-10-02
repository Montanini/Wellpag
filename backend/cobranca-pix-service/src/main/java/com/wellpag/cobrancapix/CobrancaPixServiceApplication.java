package com.wellpag.cobrancapix;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableRetry
public class CobrancaPixServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CobrancaPixServiceApplication.class, args);
    }
}
