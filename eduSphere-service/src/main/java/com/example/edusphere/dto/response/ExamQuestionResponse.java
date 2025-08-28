package com.example.edusphere.dto.response;

import lombok.Data;
import lombok.Builder;

import java.util.List;

@Data
@Builder
public class ExamQuestionResponse {
    private String id;
    private String type;
    private String question;
    private List<String> options;
    private String correctAnswer; // Only included for instructors
    private Integer correctAnswerIndex; // Only included for instructors
    private Integer points;
    private String explanation;
    private Boolean required;
    private Integer timeLimit;
    private Integer displayOrder;
    private Boolean caseSensitive;
    private Integer maxLength;
    private List<String> acceptableAnswers; // Only included for instructors
}