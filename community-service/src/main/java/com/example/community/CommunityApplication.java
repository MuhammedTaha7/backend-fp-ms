// community-service/src/main/java/com/example/community/CommunityApplication.java
package com.example.community;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication(
  scanBasePackages = {
    "com.example.community",   // this service's code
    "com.example.common"       // shared things
  }
)
@EnableMongoRepositories(
        basePackages = {
                "com.example.common.repository",
                "com.example.community.repository"
        }
)

public class CommunityApplication {
  public static void main(String[] args) {
    SpringApplication.run(CommunityApplication.class, args);
  }
}