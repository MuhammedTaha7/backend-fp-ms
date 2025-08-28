package com.example.edusphere.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO to hold the optional filter parameters for the calendar admin view.
 */
@Data
@AllArgsConstructor // Creates a constructor with all fields
@NoArgsConstructor  // Creates a default no-args constructor
public class CalendarFilterDto {
    private String courseId;
    private String instructorId;
    private String groupId;
}