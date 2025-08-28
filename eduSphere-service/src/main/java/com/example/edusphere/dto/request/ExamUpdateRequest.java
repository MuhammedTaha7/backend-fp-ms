package com.example.edusphere.dto.request;

import lombok.Data;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

@Data
public class ExamUpdateRequest {

    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    @Size(max = 2000, message = "Instructions must not exceed 2000 characters")
    private String instructions;

    @Min(value = 5, message = "Duration must be at least 5 minutes")
    @Max(value = 480, message = "Duration must not exceed 480 minutes")
    private Integer duration;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime publishTime;

    @Min(value = 1, message = "Max attempts must be at least 1")
    @Max(value = 10, message = "Max attempts must not exceed 10")
    private Integer maxAttempts;

    private Boolean showResults;
    private Boolean shuffleQuestions;
    private Boolean shuffleOptions;
    private Boolean allowNavigation;
    private Boolean showTimer;
    private Boolean autoSubmit;
    private Boolean requireSafeBrowser;
    private Boolean visibleToStudents;

    @DecimalMin(value = "0.0", message = "Pass percentage must be at least 0")
    @DecimalMax(value = "100.0", message = "Pass percentage must not exceed 100")
    private Double passPercentage;

    private String status; // DRAFT, PUBLISHED, CANCELLED
}