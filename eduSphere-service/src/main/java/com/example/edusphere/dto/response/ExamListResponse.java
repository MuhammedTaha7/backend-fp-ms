package com.example.edusphere.dto.response;

import lombok.Data;
import lombok.Builder;

import java.time.LocalDateTime;

@Data
@Builder
public class ExamListResponse {
    private String id;
    private String title;
    private String courseId;
    private String courseName;
    private Integer duration;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private Boolean isActive;
    private Boolean isUpcoming;
    private Boolean isCompleted;
    private Integer totalPoints;
    private Integer questionCount;
    private Long totalResponses;
    private Long submittedResponses;
    private Double averageScore;
    private LocalDateTime createdAt;
}