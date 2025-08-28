package com.example.edusphere.dto.response;

import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class ExamStatsResponse {
    private String examId;
    private String examTitle;
    private Long totalStudents;
    private Long totalResponses;
    private Long submittedResponses;
    private Long gradedResponses;
    private Long passedResponses;
    private Double averageScore;
    private Double passRate;
    private Double completionRate;
    private Integer highestScore;
    private Integer lowestScore;
    private Double standardDeviation;
}