package com.example.edusphere.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;

@Data
@Document(collection = "exam_responses")
public class ExamResponse {

    @Id
    private String id;

    // FIXED: Remove @Field annotation or use correct field name
    private String examId;  // This will map to "examId" in MongoDB

    // FIXED: Remove @Field annotation or use correct field name
    private String studentId;  // This will map to "studentId" in MongoDB

    // FIXED: Remove @Field annotation or use correct field name
    private String courseId;   // This will map to "courseId" in MongoDB

    // Response data
    private Map<String, String> answers = new HashMap<>(); // questionId -> answer
    private Map<String, Integer> questionScores = new HashMap<>(); // questionId -> points earned

    // Timing - FIXED: Remove @Field annotations
    private LocalDateTime startedAt;
    private LocalDateTime submittedAt;
    private Integer timeSpent; // in seconds

    // Status and scoring
    private String status = "IN_PROGRESS"; // IN_PROGRESS, SUBMITTED, GRADED, ABANDONED
    private Integer totalScore = 0;
    private Integer maxScore = 0;
    private Double percentage = 0.0;
    private Boolean passed = false;
    private Boolean graded = false;
    private Boolean autoGraded = false;

    // Attempt tracking - FIXED: Remove @Field annotation
    private Integer attemptNumber = 1;

    // Instructor feedback - FIXED: Remove @Field annotations
    private String instructorFeedback;
    private String gradedBy;
    private LocalDateTime gradedAt;

    // Flags - FIXED: Remove @Field annotations
    private Boolean flaggedForReview = false;
    private Boolean lateSubmission = false;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    // Helper methods
    public void addAnswer(String questionId, String answer) {
        if (answers == null) {
            answers = new HashMap<>();
        }
        answers.put(questionId, answer);
    }

    public void scoreQuestion(String questionId, Integer points) {
        if (questionScores == null) {
            questionScores = new HashMap<>();
        }
        questionScores.put(questionId, points);
        recalculateTotal();
    }

    public void recalculateTotal() {
        if (questionScores == null) {
            totalScore = 0;
        } else {
            totalScore = questionScores.values().stream()
                    .mapToInt(Integer::intValue)
                    .sum();
        }

        if (maxScore > 0) {
            percentage = (totalScore.doubleValue() / maxScore.doubleValue()) * 100.0;
            percentage = Math.round(percentage * 100.0) / 100.0; // Round to 2 decimal places
        }
    }

    public boolean isSubmitted() {
        return "SUBMITTED".equals(status) || "GRADED".equals(status);
    }

    public boolean isInProgress() {
        return "IN_PROGRESS".equals(status);
    }

    public boolean needsGrading() {
        return isSubmitted() && !graded;
    }
}