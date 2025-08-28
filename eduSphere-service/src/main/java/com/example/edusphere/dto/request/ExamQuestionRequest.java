package com.example.edusphere.dto.request;

import lombok.Data;
import jakarta.validation.constraints.*;

import java.util.List;
import java.util.ArrayList;

@Data
public class ExamQuestionRequest {

    @Pattern(regexp = "multiple-choice|true-false|short-answer|text|essay", message = "Invalid question type")
    private String type;

    @NotBlank(message = "Question text is required")
    @Size(max = 2000, message = "Question text must not exceed 2000 characters")
    private String question;

    private List<String> options = new ArrayList<>();

    private String correctAnswer;
    private Integer correctAnswerIndex;

    @NotNull(message = "Points are required")
    @Min(value = 1, message = "Points must be at least 1")
    @Max(value = 100, message = "Points must not exceed 100")
    private Integer points;

    @Size(max = 1000, message = "Explanation must not exceed 1000 characters")
    private String explanation;

    private Boolean required = true;
    private Integer timeLimit;
    private Boolean caseSensitive = false;
    private Integer maxLength;
    private List<String> acceptableAnswers = new ArrayList<>();
}