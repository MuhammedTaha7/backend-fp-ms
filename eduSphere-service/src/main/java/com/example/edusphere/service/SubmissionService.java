package com.example.edusphere.service;

import com.example.edusphere.dto.response.CourseAnalyticsDTO;
import com.example.edusphere.dto.response.ChartDataPoint;
import com.example.edusphere.entity.Submission;

import java.util.List;

public interface SubmissionService {
    List<Submission> findSubmissionsByCourseId(String courseId);
    Submission createSubmission(Submission submission);
    List<Submission> findSubmissionsByStudentId(String studentId);
    CourseAnalyticsDTO getCourseAnalytics(String courseId, int year);
    List<ChartDataPoint> getAssignmentTimeline(String courseId, int year);
}