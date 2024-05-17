package com.google.zetasql.toolkit.antipattern; // Use your actual package name

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AntiPatternApplication {

    public static void main(String[] args) {
        // This is the standard way to start a Spring Boot application
        SpringApplication.run(AntiPatternApplication.class, args); 
    }
}
