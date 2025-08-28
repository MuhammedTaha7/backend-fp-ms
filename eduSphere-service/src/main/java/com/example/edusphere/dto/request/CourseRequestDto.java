package com.example.edusphere.dto.request;

import lombok.Data;

@Data
public class CourseRequestDto {
    private String lecturerId;
    private String courseId;
    private String courseCode;
    private String courseName;
    private String semester;
    private Integer classSize;
    private String department;
    private Integer credits;
}