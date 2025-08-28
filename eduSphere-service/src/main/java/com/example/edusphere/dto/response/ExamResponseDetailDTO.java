package com.example.edusphere.dto.response;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamResponseDetailDTO {
    private String id;
    private String examId;
    private String studentId;
    private String examTitle;
    private Integer totalScore;
    private Integer maxScore;
    private Double percentage;
    private Boolean passed;
    private Integer timeSpent;
    private LocalDateTime submittedAt;
    private Map<String, String> answers;
    private Map<String, Integer> questionScores;
    private Double examPassPercentage;
    private Boolean graded;
    private String status;
}