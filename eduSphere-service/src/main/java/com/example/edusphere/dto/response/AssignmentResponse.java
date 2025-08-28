package com.example.edusphere.dto.response;

import com.example.edusphere.entity.Assignment;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
public class AssignmentResponse {
    private String id;
    private String title;
    private String type;
    private LocalDate dueDate;
    private String status;

    // Helper method to convert an Assignment entity to this DTO
    public static AssignmentResponse fromEntity(Assignment assignment) {
        AssignmentResponse dto = new AssignmentResponse();
        dto.setId(assignment.getId());
        dto.setTitle(assignment.getTitle());
        dto.setType(assignment.getType());
        dto.setDueDate(assignment.getDueDate());
        dto.setStatus(assignment.getStatus());
        return dto;
    }
}