package com.example.edusphere.dto.request;

import lombok.Data;
import jakarta.validation.constraints.*;

import java.util.Map;
import java.util.HashMap;

@Data
public class ExamResponseRequest {

    @NotBlank(message = "Exam ID is required")
    private String examId;

    @NotEmpty(message = "Answers are required")
    private Map<String, String> answers = new HashMap<>();

    private Integer timeSpent; // in seconds
    private Boolean isSubmission = false; // true for final submission, false for saving progress
}