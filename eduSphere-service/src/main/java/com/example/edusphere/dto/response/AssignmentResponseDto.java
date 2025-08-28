package com.example.edusphere.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO representing a single assignment item to be displayed on the dashboard.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentResponseDto {

    private String id;
    private String title;
    private String type; // e.g., "assignment", "project", "essay"
    private LocalDate dueDate;
    private String description;
    private int progress;
    private List<String> badges; // e.g., ["urgent", "important"]

}