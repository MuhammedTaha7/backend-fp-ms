package com.example.edusphere.dto.response;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TemplateResponse {

    private String id;
    private String name;
    private String category;
    private String subject;
    private String content;
    private List<String> variables;
    private String targetAudience;
    private String status;
    private String creatorId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}