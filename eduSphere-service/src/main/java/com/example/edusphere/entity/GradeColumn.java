package com.example.edusphere.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Data
@Document(collection = "grade_columns")
public class GradeColumn {

    @Id
    private String id;

    @Field("course_id")
    private String courseId;

    private String name; // e.g., "Midterm Exam", "Final Project", "Assignment 1"
    private String type; // e.g., "assignment", "exam", "quiz", "project"
    private Integer percentage; // Weight percentage (0-100)
    private Integer maxPoints; // Maximum points possible
    private String description;
    private Boolean isActive = true;
    private Integer displayOrder; // For ordering columns

    // NEW: Link to assignment if auto-created
    @Field("linked_assignment_id")
    private String linkedAssignmentId;

    // NEW: Auto-created flag
    @Field("auto_created")
    private Boolean autoCreated = false;

    // NEW: Source information
    @Field("created_by")
    private String createdBy; // "manual" or "auto" or instructor ID

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    // Helper methods
    public boolean isLinkedToAssignment() {
        return linkedAssignmentId != null && !linkedAssignmentId.trim().isEmpty();
    }

    public boolean isAutoCreated() {
        return autoCreated != null && autoCreated;
    }

    public boolean isManuallyCreated() {
        return !isAutoCreated();
    }
}