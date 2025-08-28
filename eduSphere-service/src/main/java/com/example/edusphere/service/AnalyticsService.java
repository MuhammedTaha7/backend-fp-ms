package com.example.edusphere.service;

import com.example.edusphere.dto.response.LecturerStatsDto;
import com.example.edusphere.dto.response.StudentStatsDto;

public interface AnalyticsService {
    StudentStatsDto getStudentStats(String studentId);
    LecturerStatsDto getLecturerStats(String lecturerId);

    double calculateStudentGPA(String id);
}