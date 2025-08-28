package com.example.edusphere.dto.response;

import com.example.edusphere.entity.ExamQuestion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamForGradingDTO {

    // Basic exam info
    private String id;
    private String title;
    private String description;
    private String instructions;
    private String courseId;

    // Grading context
    private Integer totalPoints;
    private Double passPercentage;
    private List<ExamQuestion> questions;

    // Question analysis
    private Integer questionCount;
    private Long autoGradableQuestions;
    private Long manualGradingRequired;
    private Boolean hasAutoGradableQuestions;
    private Boolean requiresManualGrading;

    // Question type breakdown
    private Map<String, Long> questionTypeBreakdown;

    // Grading statistics
    private String autoGradingStats;
    private String questionBreakdown;

    // Timing info
    private Integer duration;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    // Status
    private String status;
    private Boolean visibleToStudents;
}