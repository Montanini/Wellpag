package com.wellpag;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class WellpagApplication {

    public static void main(String[] args) {
        SpringApplication.run(WellpagApplication.class, args);
    }
}
