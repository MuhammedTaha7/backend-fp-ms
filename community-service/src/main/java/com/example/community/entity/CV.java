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

@Getter
@Setter
@Document(collection = "cvs")
public class CV {

    @Id
    private String id;

    @DBRef
    private UserEntity user;

    // Form-based CV fields
    private String name;
    private String title;
    private String summary;
    private String education;
    private String experience;
    private String skills;
    private String links;

    // File-based CV fields
    private String fileName;
    private String filePath;
    private String fileUrl;

    // Metadata fields
    private String templateUsed;
    private String generatedBy; // "manual", "ai", "upload"
    private Integer completenessScore;
    private LocalDateTime lastAccessedAt;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public String getUserId() {
        return user != null ? user.getId() : null;
    }

    // Helper methods
    public boolean hasFormData() {
        return (name != null && !name.isEmpty()) ||
                (title != null && !title.isEmpty()) ||
                (summary != null && !summary.isEmpty());
    }

    public boolean hasFileData() {
        return fileName != null && filePath != null;
    }
}