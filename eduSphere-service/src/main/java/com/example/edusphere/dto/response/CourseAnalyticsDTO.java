package com.example.edusphere.dto.response;

import java.util.List;
import java.util.Map;

// This class will hold all the analytics data for a single course.
public class CourseAnalyticsDTO {

    private double averageGrade;
    private int totalSubmissions;
    private GradeDistribution gradeDistribution;

    // Inner class for the grade distribution chart data
    public static class GradeDistribution {
        private List<String> labels; // e.g., ["90-100", "80-89", ...]
        private List<Integer> data;  // e.g., [5, 12, ...]

        public GradeDistribution(List<String> labels, List<Integer> data) {
            this.labels = labels;
            this.data = data;
        }

        // Getters and Setters
        public List<String> getLabels() { return labels; }
        public void setLabels(List<String> labels) { this.labels = labels; }
        public List<Integer> getData() { return data; }
        public void setData(List<Integer> data) { this.data = data; }
    }

    // Getters and Setters for the main DTO
    public double getAverageGrade() { return averageGrade; }
    public void setAverageGrade(double averageGrade) { this.averageGrade = averageGrade; }
    public int getTotalSubmissions() { return totalSubmissions; }
    public void setTotalSubmissions(int totalSubmissions) { this.totalSubmissions = totalSubmissions; }
    public GradeDistribution getGradeDistribution() { return gradeDistribution; }
    public void setGradeDistribution(GradeDistribution gradeDistribution) { this.gradeDistribution = gradeDistribution; }
}