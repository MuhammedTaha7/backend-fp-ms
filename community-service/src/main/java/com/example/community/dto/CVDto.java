package com.example.community.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class CVDto {
    private String id;
    private String name;
    private String title;
    private String summary;
    private String education;
    private String experience;
    private String skills;
    private String links;
    private String fileName;
    private String filePath;
    private String fileUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}