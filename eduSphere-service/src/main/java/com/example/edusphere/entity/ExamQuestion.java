package com.example.edusphere.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;

import java.util.List;
import java.util.ArrayList;

@Data
public class ExamQuestion {

    @Id
    private String id;

    private String type; // "multiple-choice", "true-false", "text", "essay"
    private String question;
    private List<String> options = new ArrayList<>(); // For multiple choice
    private String correctAnswer; // For auto-grading
    private Integer correctAnswerIndex; // For multiple choice
    private Integer points = 5;
    private String explanation; // Optional explanation for correct answer
    private Boolean required = true;
    private Integer timeLimit; // Optional time limit for this question in seconds
    private Integer displayOrder;

    // For different question types
    private Boolean caseSensitive = false; // For text questions
    private Integer maxLength; // For text/essay questions
    private List<String> acceptableAnswers = new ArrayList<>(); // Multiple acceptable text answers

    public boolean isMultipleChoice() {
        return "multiple-choice".equals(type);
    }

    public boolean isTrueFalse() {
        return "true-false".equals(type);
    }

    public boolean isTextQuestion() {
        return "text".equals(type);
    }

    public boolean isEssayQuestion() {
        return "essay".equals(type);
    }

    public boolean canAutoGrade() {
        return isMultipleChoice() || isTrueFalse() ||
                (isTextQuestion() && acceptableAnswers != null && !acceptableAnswers.isEmpty());
    }
}