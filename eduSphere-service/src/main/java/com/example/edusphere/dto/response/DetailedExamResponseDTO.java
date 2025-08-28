package com.example.edusphere.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetailedExamResponseDTO {

    // Basic response info
    private String id;
    private String examId;
    private String studentId;
    private String courseId;

    // Response data
    private Map<String, String> answers;
    private Map<String, Integer> questionScores;

    // Timing
    private LocalDateTime startedAt;
    private LocalDateTime submittedAt;
    private Integer timeSpent;
    private String timeSpentFormatted;

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

    // Instructor feedback
    private String instructorFeedback;
    private String gradedBy;
    private LocalDateTime gradedAt;

    // Flags
    private Boolean flaggedForReview;
    private String flagReason;
    private Boolean lateSubmission;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Computed properties for grading
    private Boolean isCompleted;
    private Boolean needsManualGrading;
    private String gradingStatus;

    // Exam context
    private String examTitle;
    private Double examPassPercentage;

    // Student info
    private String studentName;
    private String studentEmail;
}