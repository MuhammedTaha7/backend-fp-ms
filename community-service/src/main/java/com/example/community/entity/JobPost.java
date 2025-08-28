package com.example.community.entity;

import com.example.common.entity.UserEntity;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DBRef;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Document(collection = "job_posts")
public class JobPost {

    @Id
    private String id;

    private String title;
    private String company;
    private String location;
    private String description;
    private String type; // "Full-time", "Part-time", "Contract", "Internship"
    private List<String> tags; // Required skills
    private String salary;
    private String remote; // "Yes", "No", "Hybrid"
    private String experience; // "Entry Level", "Mid Level", "Senior Level", "Executive"
    private LocalDate deadline;
    private List<String> benefits;
    private String requirements;
    private String status; // "Active", "Closed"

    @DBRef
    private UserEntity poster; // User who posted the job

    private Integer applicationCount = 0; // Cache for performance

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}