package com.example.community.entity;

import com.example.common.entity.UserEntity;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DBRef;
import java.time.LocalDateTime;

@Getter
@Setter
@Document(collection = "notifications")
public class Notification {

    @Id
    private String id;

    @DBRef
    private UserEntity recipient;

    @DBRef
    private UserEntity sender; // Optional: who triggered the notification

    private String type; // "JOB_APPLICATION_ACCEPTED", "JOB_APPLICATION_REJECTED", etc.
    private String title;
    private String message;
    private String relatedEntityId; // jobId, applicationId, etc.
    private String relatedEntityType; // "JOB", "APPLICATION", etc.

    private boolean isRead = false;
    private boolean isDeleted = false;

    @CreatedDate
    private LocalDateTime createdAt;

    private LocalDateTime readAt;
}