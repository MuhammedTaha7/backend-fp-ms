package com.example.edusphere.dto.request;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadRequest {

    private String name;
    private String description;
    private String category;
    private String accessType;
    private String accessBy;
    private String accessValue;

    private List<String> recipientIds;
}