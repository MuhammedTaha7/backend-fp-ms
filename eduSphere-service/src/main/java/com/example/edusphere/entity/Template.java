package com.example.edusphere.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "templates")
public class Template {

    @Id
    private String id;
    private String creatorId;
    private String name;
    private String category;
    private String subject;
    private String content;
    private List<String> variables;
    private String targetAudience; // e.g., "student", "lecturer"
    private String status; // e.g., "active", "draft"

    @CreatedDate
    private LocalDateTime createdAt;
    @LastModifiedDate
    private LocalDateTime updatedAt;
}