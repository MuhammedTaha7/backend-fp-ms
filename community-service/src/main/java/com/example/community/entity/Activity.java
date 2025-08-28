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
@Document(collection = "activities")
public class Activity {

    @Id
    private String id;

    @DBRef
    private UserEntity user; // User who performed the activity

    private String type; // "post", "comment", "like", "join_group", "friend_request", etc.
    private String action; // Human readable action description
    private String targetId; // ID of the target (post, group, user, etc.)
    private String targetType; // "post", "group", "user", etc.

    @CreatedDate
    private LocalDateTime createdAt;
}