package com.example.edusphere.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL) // Prevents sending null fields in the JSON
public class CalendarEventDto {
    private String id;
    private String title;
    private String description;
    private String type; // "LECTURE", "LAB", "ASSIGNMENT", "EXAM"
    private String instructorName;
    private String instructorImage;


    // Fields for both Events and Assignments
    private String course;
    private LocalDate date;

    // Fields specific to scheduled Events
    private LocalTime startTime;
    private LocalTime endTime;
    private String location;

    // Fields specific to Assignments
    private LocalTime dueTime;
    private Integer progress;
}