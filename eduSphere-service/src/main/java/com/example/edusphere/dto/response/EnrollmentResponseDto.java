package com.example.edusphere.dto.response;

import lombok.Data;

@Data
public class EnrollmentResponseDto {
    private String id;
    private String studentId;
    private String courseId;
    private String courseCode;
    private String courseName;
    private int credits;
    private String semester;
    private int academicYear;
    private String lecturer;
    private String status; // "enrolled", "dropped", "completed", etc.
}