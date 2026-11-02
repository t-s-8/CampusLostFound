package com.campuslostfound.api;

import com.campuslostfound.database.DatabaseConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.campuslostfound")
public class CampusLostFoundWebApplication {
    public static void main(String[] args) {
        DatabaseConfig.init();
        SpringApplication.run(CampusLostFoundWebApplication.class, args);
    }
}
