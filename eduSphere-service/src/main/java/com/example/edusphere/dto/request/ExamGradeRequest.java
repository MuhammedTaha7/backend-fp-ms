package com.example.edusphere.dto.request;

import lombok.Data;
import jakarta.validation.constraints.*;

import java.util.Map;
import java.util.HashMap;

@Data
public class ExamGradeRequest {

    @NotBlank(message = "Response ID is required")
    private String responseId;

    @NotEmpty(message = "Question scores are required")
    private Map<String, Integer> questionScores = new HashMap<>();

    @Size(max = 2000, message = "Feedback must not exceed 2000 characters")
    private String instructorFeedback;

    private Boolean flaggedForReview = false;
}
