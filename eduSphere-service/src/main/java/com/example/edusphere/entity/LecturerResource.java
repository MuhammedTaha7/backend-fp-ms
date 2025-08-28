package com.example.edusphere.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Data
@Document(collection = "lecturer_resources")
public class LecturerResource {

    @Id
    private String id;

    @Field("lecturer_id")
    private String lecturerId;

    private String title;
    private String type; // e.g., 'cv', 'research', 'publication'
    private String description;
    private String date;
    private String institution;
    private String url;
    private String tags;

    private String fileName;
    private String filePath; // The path where the file is stored
    private String mimeType;
    private long size;

    @CreatedDate
    private LocalDateTime uploadDate;

    private Integer downloadCount = 0;
}