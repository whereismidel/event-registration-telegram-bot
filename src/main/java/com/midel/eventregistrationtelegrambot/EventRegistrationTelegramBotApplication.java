package com.midel.eventregistrationtelegrambot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EventRegistrationTelegramBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(EventRegistrationTelegramBotApplication.class, args);

        try {
            Thread.sleep(5000);
            System.exit(1);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
