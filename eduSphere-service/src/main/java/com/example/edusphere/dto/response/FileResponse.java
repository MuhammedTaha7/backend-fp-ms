package com.example.edusphere.dto.response;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FileResponse {

    private String id;
    private String name;
    private String type;
    private String size;
    private String category;
    private String description;
    private String fileUrl;
    private String uploadedByUserId;
    private String uploadedByUserName;
    private LocalDateTime uploadDate;
    private int downloadCount;
    private String accessType;
    private String accessBy;
    private String accessValue;
}