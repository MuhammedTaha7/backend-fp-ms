package com.example.edusphere.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseHistoryDTO {

    private String id;
    private String examId;
    private String studentId;
    private Integer attemptNumber;
    private String status;
    private LocalDateTime startedAt;
    private LocalDateTime submittedAt;
    private Integer timeSpent;
    private String timeSpentFormatted;
    private Integer totalScore;
    private Integer maxScore;
    private Double percentage;
    private Boolean passed;
    private Boolean graded;
    private Boolean autoGraded;
    private Boolean flaggedForReview;

    // Computed properties
    private Boolean isCurrentAttempt;
    private Boolean isCompleted;
    private String gradingStatus;
}