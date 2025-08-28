package com.example.community.entity;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Getter
@Setter
@Document(collection = "attached_files")
public class AttachedFile {

    @Id
    private String id;

    private String fileName;
    private String originalFileName;
    private String filePath;
    private String fileUrl;
    private String fileType; // "image", "document", "video", etc.
    private String mimeType;
    private Long fileSize; // in bytes

    private String uploadedBy; // User ID who uploaded

    @CreatedDate
    private LocalDateTime uploadedAt;
}