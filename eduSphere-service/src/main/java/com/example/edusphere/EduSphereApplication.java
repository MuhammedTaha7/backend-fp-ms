// eduSphere-service/src/main/java/com/example/edusphere/EduSphereApplication.java
package com.example.edusphere;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
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
public class EduSphereApplication {
  public static void main(String[] args) {
    SpringApplication.run(EduSphereApplication.class, args);
  }
}
