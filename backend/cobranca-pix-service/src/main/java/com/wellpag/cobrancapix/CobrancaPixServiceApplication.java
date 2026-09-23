package com.wellpag.cobrancapix;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CobrancaPixServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CobrancaPixServiceApplication.class, args);
    }
}
