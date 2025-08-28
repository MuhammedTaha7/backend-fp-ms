package com.example.edusphere.dto.request;

import lombok.Data;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class TaskUpdateRequest {

    private String title;
    private String description;
    private String type;
    private LocalDate dueDate;
    private LocalTime dueTime;

    @Min(value = 1, message = "Max points must be at least 1")
    @Max(value = 1000, message = "Max points cannot exceed 1000")
    private Integer maxPoints;

    private String instructions;
    private String status;
    private String priority;
    private String difficulty;
    private String category;
    private Boolean allowSubmissions;
    private Boolean allowLateSubmissions;
    private Double latePenaltyPerDay;
    private Boolean visibleToStudents;
    private Boolean requiresSubmission;
    private Integer maxAttempts;
    private Integer estimatedDuration;
    private LocalDateTime publishDate;
    private List<String> tags;
    private List<String> prerequisiteTasks;

    // File attachment info
    private String fileUrl;
    private String fileName;
    private Long fileSize;
}
