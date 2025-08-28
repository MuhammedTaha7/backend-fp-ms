package com.example.edusphere.dto.request;

import lombok.Data;
import java.util.Map;

@Data
public class GradeRequestDto {
    private String studentId;
    private String courseId;
    private Map<String, Double> grades; // Map of columnId -> grade
    private Double finalGrade;
}