package com.example.edusphere.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.index.CompoundIndex;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Data
@Document(collection = "student_grades")
@CompoundIndex(
        name = "student_course_unique_idx",
        def = "{'studentId': 1, 'courseId': 1}",
        unique = true
)
public class StudentGrade {

    @Id
    private String id;

    @Field("student_id")
    private String studentId;

    @Field("course_id")
    private String courseId;

    // Map of column_id -> grade value
    private Map<String, Double> grades = new HashMap<>();

    @Field("final_grade")
    private Double finalGrade;

    @Field("final_letter_grade")
    private String finalLetterGrade; // A, B, C, D, F

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    // Helper methods
    public void setGrade(String columnId, Double grade) {
        this.grades.put(columnId, grade);
    }

    public Double getGrade(String columnId) {
        return this.grades.get(columnId);
    }

    public void removeGrade(String columnId) {
        this.grades.remove(columnId);
    }
}