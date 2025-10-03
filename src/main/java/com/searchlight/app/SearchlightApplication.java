package com.searchlight.app;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@Slf4j
@SpringBootApplication
@ComponentScan(basePackages = "com.searchlight")
public class SearchlightApplication {
    
    public static void main(String[] args) {
        log.info("Starting Searchlight Application...");
        SpringApplication.run(SearchlightApplication.class, args);
    }
}
