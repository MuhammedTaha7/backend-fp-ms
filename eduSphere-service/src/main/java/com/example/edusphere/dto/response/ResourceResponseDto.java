package com.example.edusphere.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ResourceResponseDto {
    private String id;
    private String lecturerId;
    private String title;
    private String type;
    private String description;
    private String date;
    private String institution;
    private String url;
    private String tags;
    private String fileName;
    private String size;
    private LocalDateTime uploadDate;

    private String mimeType;
}