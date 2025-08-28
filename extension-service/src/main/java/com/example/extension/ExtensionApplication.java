// extension-service/src/main/java/com/example/extension/ExtensionApplication.java
package com.example.extension;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication(
  scanBasePackages = {
    "com.example.extension",   // this service's code
    "com.example.common"       // shared things
  }
)
@EnableMongoRepositories(
        basePackages = {
                "com.example.common.repository",
                "com.example.extension.repository"
        }
)

public class ExtensionApplication {
  public static void main(String[] args) {
    SpringApplication.run(ExtensionApplication.class, args);
  }
}
