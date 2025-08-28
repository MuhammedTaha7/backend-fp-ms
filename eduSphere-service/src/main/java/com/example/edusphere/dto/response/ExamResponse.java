package com.example.edusphere.dto.response;

import lombok.Data;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ExamResponse {
    private String id;
    private String courseId;
    private String courseName;
    private String instructorId;
    private String instructorName;
    private String title;
    private String description;
    private String instructions;

    // Timing
    private Integer duration;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime publishTime;

    // Settings
    private Integer maxAttempts;
    private Boolean showResults;
    private Boolean shuffleQuestions;
    private Boolean shuffleOptions;
    private Boolean allowNavigation;
    private Boolean showTimer;
    private Boolean autoSubmit;
    private Boolean requireSafeBrowser;
    private Boolean visibleToStudents;

    // Grading
    private Integer totalPoints;
    private Double passPercentage;

    // Status
    private String status;
    private Boolean isActive;
    private Boolean isUpcoming;
    private Boolean isCompleted;

    // Questions
    private List<ExamQuestionResponse> questions;
    private Integer questionCount;

    // Statistics
    private Long totalResponses;
    private Long submittedResponses;
    private Long gradedResponses;
    private Long passedResponses;
    private Double averageScore;
    private Double passRate;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}