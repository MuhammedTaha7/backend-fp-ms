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
@Document(collection = "user_challenges")
public class UserChallenge {

    @Id
    private String id;

    @DBRef
    private UserEntity user;

    @DBRef
    private Challenge challenge;

    private String status; // "NOT_STARTED", "IN_PROGRESS", "COMPLETED"
    private String submissionLink; // GitHub, Drive link, etc.
    private String notes; // Optional notes from user

    @CreatedDate
    private LocalDateTime startedAt;

    private LocalDateTime completedAt;
}