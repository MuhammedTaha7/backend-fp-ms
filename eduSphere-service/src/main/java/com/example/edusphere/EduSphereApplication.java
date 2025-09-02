package com.example.edusphere;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication(
        scanBasePackages = {
                "com.example.edusphere",   // this service's code
                "com.example.common"       // shared things
        }
)
@EnableMongoRepositories(
        basePackages = {
                "com.example.common.repository",
                "com.example.edusphere.repository"
        }
)
@EnableMongoAuditing
public class EduSphereApplication {
    public static void main(String[] args) {
        SpringApplication.run(EduSphereApplication.class, args);
    }
}