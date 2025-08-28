package com.example.edusphere.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class QuestionScoreUpdateRequest {

    @NotBlank(message = "Question ID is required")
    private String questionId;

    @NotNull(message = "Score is required")
    @Min(value = 0, message = "Score cannot be negative")
    private Integer score;

    private String feedback;
}