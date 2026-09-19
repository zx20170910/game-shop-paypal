package com.unicorn.gameshop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class GameShopApplication {

    public static void main(String[] args) {
        SpringApplication.run(GameShopApplication.class, args);
    }
}
