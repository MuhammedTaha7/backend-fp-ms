package com.example.edusphere.dto.request;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * DTO for receiving data from the frontend when creating or updating an assignment.
 * This represents the data from the 'Add/Edit Assignment' form.
 */
@Data
public class AssignmentRequestDto {

    private String id; // Can be empty for new assignments, will contain ID for updates
    private String title;
    private String description;
    private String course; // Course ID
    private String type;
    private LocalDate dueDate;
    private LocalTime dueTime; // Can be null/empty if not provided
    private String progress; // Changed from int to String to match frontend data
    private String status;
    private String priority; // Can be empty string
    private String instructor; // Can be empty string if not assigned yet
    private String instructorId; // Alternative field name for instructor ID
    private String difficulty; // Can be empty string
    private String semester; // Can be empty string
    private String year; // Added to match frontend data (was missing)
    private List<String> badges; // Optional list of badges

    // Helper method to get progress as integer with default value


    // Helper method to check if dueTime is provided
    public boolean hasDueTime() {
        return dueTime != null;
    }

    // Helper method to check if assignment has an instructor assigned
    public boolean hasInstructor() {
        return (instructor != null && !instructor.trim().isEmpty()) ||
                (instructorId != null && !instructorId.trim().isEmpty());
    }

    // Helper method to get the instructor identifier (prioritizes instructorId over instructor)
    public String getInstructorIdentifier() {
        if (instructorId != null && !instructorId.trim().isEmpty()) {
            return instructorId;
        }
        return instructor;
    }
}