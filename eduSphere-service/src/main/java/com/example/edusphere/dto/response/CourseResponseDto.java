package com.example.edusphere.dto.response;

import lombok.Data;

@Data
public class CourseResponseDto {
    private String id;
    private String courseCode;
    private String courseName;
    private Integer credits;
    private String semester;
    private String department;
    private Integer classSize; // We'll derive this from enrollments
    private String status; // We'll need a way to determine this
}