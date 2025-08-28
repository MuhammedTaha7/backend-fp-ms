package com.example.edusphere.dto.response;

import lombok.Data;

@Data
public class StudentStatsDto {
    private Double gpa;
    private Integer totalCredits;
    private Integer totalCourses;
    private Integer completedCourses;
    private String enrollmentStatus;
}