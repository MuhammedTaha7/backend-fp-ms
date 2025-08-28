package com.example.edusphere.dto.request;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class ResourceRequestDto {
    private String lecturerId;
    private String title;
    private String type;
    private String description;
    private String date; // Document date (e.g., publication date)
    private String institution;
    private String url; // External link
    private String tags;
    private MultipartFile file; // The actual file to be uploaded
}