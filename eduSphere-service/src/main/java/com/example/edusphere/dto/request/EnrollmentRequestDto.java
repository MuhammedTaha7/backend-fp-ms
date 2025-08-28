package com.example.edusphere.dto.request;

import lombok.Data;

@Data
public class EnrollmentRequestDto {
    private String studentId;
    private String courseId;
    private String status;
}