package com.chanyong.gunpla;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GunplaApplication {

    public static void main(String[] args) {
        SpringApplication.run(GunplaApplication.class, args);
    }
}
