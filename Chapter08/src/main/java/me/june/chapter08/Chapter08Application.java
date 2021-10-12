package me.june.chapter08;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@EnableBatchProcessing
@SpringBootApplication
public class Chapter08Application {

    public static void main(String[] args) {
        SpringApplication.run(Chapter08Application.class, args);
    }
}
