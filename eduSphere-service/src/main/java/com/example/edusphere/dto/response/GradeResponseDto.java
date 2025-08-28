package com.example.edusphere.dto.response;

import lombok.Data;

@Data
public class GradeResponseDto {
    private String id;
    private String studentId;
    private String courseId; // Use courseId to link to a Course entity
    private String courseCode; // We'll fetch this from the Course entity
    private String courseName; // We'll fetch this from the Course entity
    private Double grade; // Corresponds to finalGrade
    private String letterGrade; // Corresponds to finalLetterGrade
    private Integer credits; // We'll fetch this from the Course entity
    private String semester; // This will likely come from a Course or Enrollment entity
}