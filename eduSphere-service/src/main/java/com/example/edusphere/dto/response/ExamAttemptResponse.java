package com.example.edusphere.dto.response;

import lombok.Data;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class ExamAttemptResponse {
    private String id;
    private String examId;
    private String examTitle;
    private String studentId;
    private String studentName;
    private String courseId;

    // Response data
    private Map<String, String> answers;
    private Map<String, Integer> questionScores;

    // Timing
    private LocalDateTime startedAt;
    private LocalDateTime submittedAt;
    private Integer timeSpent;

    // Status and scoring
    private String status;
    private Integer totalScore;
    private Integer maxScore;
    private Double percentage;
    private Boolean passed;
    private Boolean graded;
    private Boolean autoGraded;

    // Attempt tracking
    private Integer attemptNumber;
    private Integer maxAttempts;

    // Instructor feedback
    private String instructorFeedback;
    private String gradedBy;
    private LocalDateTime gradedAt;

    // Flags
    private Boolean flaggedForReview;
    private Boolean lateSubmission;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}