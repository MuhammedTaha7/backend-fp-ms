package com.example.edusphere.dto.request;

import com.example.edusphere.entity.ExamQuestion;
import lombok.Data;
import jakarta.validation.constraints.*;
import jakarta.validation.Valid;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

@Data
public class ExamCreateRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    @Size(max = 2000, message = "Instructions must not exceed 2000 characters")
    private String instructions;

    @NotBlank(message = "Course ID is required")
    private String courseId;

    @NotNull(message = "Duration is required")
    @Min(value = 5, message = "Duration must be at least 5 minutes")
    @Max(value = 480, message = "Duration must not exceed 480 minutes (8 hours)")
    private Integer duration;

    @NotNull(message = "Start time is required")
    private LocalDateTime startTime;

    @NotNull(message = "End time is required")
    private LocalDateTime endTime;

    private LocalDateTime publishTime;

    @Min(value = 1, message = "Max attempts must be at least 1")
    @Max(value = 10, message = "Max attempts must not exceed 10")
    private Integer maxAttempts = 1;

    private Boolean showResults = true;
    private Boolean shuffleQuestions = false;
    private Boolean shuffleOptions = false;
    private Boolean allowNavigation = true;
    private Boolean showTimer = true;
    private Boolean autoSubmit = true;
    private Boolean requireSafeBrowser = false;
    private Boolean visibleToStudents = false;

    @DecimalMin(value = "0.0", message = "Pass percentage must be at least 0")
    @DecimalMax(value = "100.0", message = "Pass percentage must not exceed 100")
    private Double passPercentage = 60.0;

    @Valid
    private List<ExamQuestion> questions = new ArrayList<>();
}