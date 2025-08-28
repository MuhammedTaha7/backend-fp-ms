package com.example.community.entity;

import com.example.common.entity.UserEntity;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DBRef;
import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@Document(collection = "job_applications")
public class JobApplication {

    @Id
    private String id;

    @DBRef
    private JobPost jobPost;

    @DBRef
    private UserEntity applicant;

    private String status; // "PENDING", "REVIEWED", "ACCEPTED", "REJECTED"
    private String applicationLink;
    private String message;

    // CV data attached to application
    private Map<String, String> cvData; // CV form data
    private String cvFileName; // If CV is a file
    private String cvFilePath; // Path to CV file

    @CreatedDate
    private LocalDateTime appliedAt;

    private LocalDateTime reviewedAt;
    private String reviewNotes; // Internal notes from reviewer
}