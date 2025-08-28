package com.example.edusphere.dto.request;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class TaskCreateRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotBlank(message = "Course ID is required")
    private String courseId;

    private String type = "homework";

    @NotNull(message = "Due date is required")
    private LocalDate dueDate;

    @NotNull(message = "Due time is required")
    private LocalTime dueTime;

    @Min(value = 1, message = "Max points must be at least 1")
    @Max(value = 1000, message = "Max points cannot exceed 1000")
    private Integer maxPoints = 100;

    private String instructions;
    private String priority = "medium";
    private String difficulty = "medium";
    private String category = "individual";
    private Boolean allowSubmissions = true;
    private Boolean allowLateSubmissions = false;
    private Double latePenaltyPerDay = 0.0;
    private Boolean visibleToStudents = true;
    private Boolean requiresSubmission = true;
    private Integer maxAttempts = 1;
    private Integer estimatedDuration; // in minutes
    private LocalDateTime publishDate;
    private List<String> tags;
    private List<String> prerequisiteTasks;

    // File attachment info (optional)
    private String fileUrl;
    private String fileName;
    private Long fileSize;
}