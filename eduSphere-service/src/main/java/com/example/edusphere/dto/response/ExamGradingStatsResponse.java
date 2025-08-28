package com.example.edusphere.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamGradingStatsResponse {

    private String examId;
    private String examTitle;

    // Response counts
    private Long totalResponses;
    private Long submittedResponses;
    private Long gradedResponses;
    private Long autoGradedResponses;
    private Long manuallyGradedResponses;
    private Long needsGrading;
    private Long flaggedResponses;
    private Long passedResponses;
    private Long inProgressResponses;

    // Percentages
    private Double gradingProgress;
    private Double autoGradingEfficiency;
    private Double passRate;
    private Double completionRate;

    // Averages
    private Double averageScore;
    private Double averageTimeSpent;
    private Long averageGradingTimeSeconds;
    private Long totalGradingTimeSeconds;

    // Status breakdown
    private StatusBreakdown statusBreakdown;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusBreakdown {
        private Long inProgress;
        private Long submitted;
        private Long graded;
        private Long partiallyGraded;
        private Long flagged;
        private Long autoGradeFailed;
    }
}