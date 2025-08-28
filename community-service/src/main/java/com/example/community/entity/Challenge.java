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
@Document(collection = "challenges")
public class Challenge {

    @Id
    private String id;

    private String title;
    private String description;
    private String category; // "Frontend", "Backend", "Programming", etc.
    private String type; // "external", "interactive"
    private String difficulty; // "Easy", "Medium", "Hard"
    private Integer points; // Points awarded for completion

    @DBRef
    private UserEntity creator; // Who created this challenge

    @CreatedDate
    private LocalDateTime createdAt;
}