package com.example.edusphere.service;

import com.example.edusphere.entity.GradeColumn;
import com.example.edusphere.entity.StudentGrade;

import java.util.List;

public interface GradeService {
    // Grade Columns
    List<GradeColumn> getGradeColumnsByCourse(String courseId);
    GradeColumn createGradeColumn(GradeColumn gradeColumn);
    GradeColumn updateGradeColumn(String columnId, GradeColumn updates);
    void deleteGradeColumn(String columnId);

    // Student Grades
    List<StudentGrade> getGradesByCourse(String courseId);
    StudentGrade updateStudentGrade(String studentId, String columnId, Double grade);
    void deleteStudentGrades(String studentId, String courseId);
    Double calculateFinalGrade(String studentId, String courseId);
    String calculateLetterGrade(Double percentage);

    // Validation
    boolean validateGradeColumnPercentage(String courseId, Integer percentage, String excludeColumnId);
}