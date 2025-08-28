package com.example.edusphere.entity;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Task entity for lecturer dashboard - separate from Assignment
 */
@Data
@Document(collection = "tasks")
public class Task {

    @Id
    private String id;

    private String title;
    private String description;

    @Field("course_id")
    private String courseId;

    private String type; // e.g., "homework", "project", "quiz", "essay"

    @Field("due_date")
    private LocalDate dueDate;

    @Field("due_time")
    private LocalTime dueTime;

    @Field("max_points")
    private Integer maxPoints = 100; // Default to 100 points

    private String status = "active"; // "active", "draft", "archived", "completed"
    private String priority = "medium"; // "low", "medium", "high"
    private String difficulty = "medium"; // "easy", "medium", "hard"

    // File attachment information
    @Field("file_url")
    private String fileUrl;

    @Field("file_name")
    private String fileName;

    @Field("file_size")
    private Long fileSize;

    // Instructions for students
    private String instructions;

    // Whether submissions are allowed
    @Field("allow_submissions")
    private Boolean allowSubmissions = true;

    // Late submission policy
    @Field("allow_late_submissions")
    private Boolean allowLateSubmissions = false;

    @Field("late_penalty_per_day")
    private Double latePenaltyPerDay = 0.0; // Percentage penalty per day

    // Auto-grading settings
    @Field("auto_grade")
    private Boolean autoGrade = false;

    // Visibility settings
    @Field("visible_to_students")
    private Boolean visibleToStudents = true;

    // Publication date (when task becomes visible)
    @Field("publish_date")
    private LocalDateTime publishDate;

    /**
     * The ID of the instructor (UserEntity) who created the task.
     */
    @Field("instructor_id")
    private String instructorId;

    // Statistics (calculated fields)
    @Field("submission_count")
    private Integer submissionCount = 0;

    @Field("graded_count")
    private Integer gradedCount = 0;

    @Field("average_grade")
    private Double averageGrade = 0.0;

    // Task-specific fields
    @Field("estimated_duration")
    private Integer estimatedDuration; // in minutes

    @Field("task_category")
    private String category; // "individual", "group", "presentation"

    @Field("requires_submission")
    private Boolean requiresSubmission = true;

    @Field("max_attempts")
    private Integer maxAttempts = 1;

    // Progress tracking
    private Integer progress = 0; // Percentage (0-100) - completion progress

    // Tags for organization
    private List<String> tags = new ArrayList<>();

    // Dependencies (other tasks that must be completed first)
    @Field("prerequisite_tasks")
    private List<String> prerequisiteTasks = new ArrayList<>();

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    // Helper methods
    public boolean isOverdue() {
        if (dueDate == null) return false;

        LocalDateTime dueDateTime = dueDate.atTime(dueTime != null ? dueTime : LocalTime.of(23, 59));
        return LocalDateTime.now().isAfter(dueDateTime);
    }

    public boolean isPublished() {
        if (publishDate == null) return true; // If no publish date, assume published
        return LocalDateTime.now().isAfter(publishDate);
    }

    public boolean acceptsSubmissions() {
        return allowSubmissions &&
                requiresSubmission &&
                (allowLateSubmissions || !isOverdue()) &&
                "active".equals(status) &&
                isPublished();
    }

    public LocalDateTime getDueDateTime() {
        if (dueDate == null) return null;
        LocalTime time = dueTime != null ? dueTime : LocalTime.of(23, 59);
        return dueDate.atTime(time);
    }

    public boolean hasFileAttachment() {
        return fileUrl != null && !fileUrl.trim().isEmpty();
    }

    public double getCompletionRate() {
        if (submissionCount == null || submissionCount == 0) return 0.0;
        // This would be calculated based on enrolled students vs submissions
        return submissionCount.doubleValue(); // Simplified - you'd calculate vs total enrolled
    }
}