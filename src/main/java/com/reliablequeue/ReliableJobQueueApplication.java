package com.reliablequeue;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ReliableJobQueueApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReliableJobQueueApplication.class, args);
    }
}
