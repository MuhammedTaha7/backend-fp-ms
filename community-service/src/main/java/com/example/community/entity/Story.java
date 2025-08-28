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
@Document(collection = "stories")
public class Story {

    @Id
    private String id;

    @DBRef
    private UserEntity user;

    private String text; // Text content
    private String img; // Image/video URL
    private String type; // "text", "image", "video"

    // Cached user info
    private String name;
    private String profilePic;

    @CreatedDate
    private LocalDateTime createdAt;

    // Stories expire after 24 hours
    private LocalDateTime expiresAt;
}