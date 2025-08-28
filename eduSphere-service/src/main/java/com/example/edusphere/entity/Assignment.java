package com.example.edusphere.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Represents an Assignment document in the MongoDB 'assignments' collection.
 */
@Data
@Document(collection = "assignments")
public class Assignment {

    @Id
    private String id;

    private String title;
    private String description;
    private String course;
    private String type; // e.g., "assignment", "test", "exam"

    @Field("due_date")
    private LocalDate dueDate;

    @Field("due_time")
    private LocalTime dueTime;

    private int progress; // Percentage (0-100)
    private String status; // e.g., "pending", "in-progress", "completed"
    private String priority; // e.g., "low", "medium", "high"

    /**
     * The ID of the instructor (UserEntity) who created the assignment.
     */
    @Field("instructor_id")
    private String instructorId;

    private String difficulty;
    private String semester;

    /**
     * A list of badges for quick visual cues.
     * Corresponds to the 'badges' field in the frontend static data.
     */
    private List<String> badges;

    // We can add fields for creation/update timestamps if needed
    // @CreatedDate
    // private LocalDateTime createdAt;
    // @LastModifiedDate
    // private LocalDateTime updatedAt;
}