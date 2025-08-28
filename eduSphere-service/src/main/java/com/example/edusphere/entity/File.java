package com.example.edusphere.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "files")
public class File {

    @Id
    private String id;

    private String name;
    private String type;
    private String size;
    private String category;
    private String description;

    // File path/URL on the server
    private String fileUrl;
    private String filename; // The unique filename on the server

    // User who uploaded the file
    private String uploadedByUserId;
    private String uploadedByUserName;

    // Access control
    private String accessType; // e.g., "students", "lecturers", "public"
    private String accessBy; // e.g., "Department", "all"
    private String accessValue; // e.g., "Computer Science", "all-users"

    // Audit info
    @CreatedDate
    private LocalDateTime uploadDate;
    private int downloadCount;

    private List<String> recipientIds;
}