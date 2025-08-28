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
@Document(collection = "comments")
public class Comment {

    @Id
    private String id;

    private String postId; // ID of the post this comment belongs to

    @DBRef
    private UserEntity user; // User who made the comment

    private String text;
    private String username; // Cache for performance
    private String profilePicture; // Cache for performance

    @CreatedDate
    private LocalDateTime createdAt;
}