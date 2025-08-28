package com.example.edusphere.dto.response;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
public class TaskDetailResponse extends TaskResponse {

    // Additional details for single task view
    private List<TaskSubmissionSummary> recentSubmissions;
    private TaskStatistics statistics;
    private List<TaskResponse> prerequisiteTaskDetails; // Renamed to avoid conflict

    @Data
    public static class TaskSubmissionSummary {
        private String id;
        private String studentId;
        private String studentName;
        private LocalDateTime submittedAt;
        private Integer grade;
        private String status; // "submitted", "graded", "late", "pending"
        private boolean isLate;
        private Integer attemptNumber;
        private String feedback;
    }

    @Data
    public static class TaskStatistics {
        private int totalStudents;
        private int submittedCount;
        private int gradedCount;
        private int pendingCount;
        private int lateSubmissions;
        private double completionRate; // percentage
        private double averageGrade;
        private int highestGrade;
        private int lowestGrade;
        private double averageTimeToComplete; // in hours
        private int studentsStarted;
        private int studentsCompleted;

        // Time-based statistics
        private long averageSubmissionTime; // minutes before due date
        private int submissionsToday;
        private int submissionsThisWeek;

        // Grade distribution
        private GradeDistribution gradeDistribution;
    }

    @Data
    public static class GradeDistribution {
        private int gradeA; // 90-100
        private int gradeB; // 80-89
        private int gradeC; // 70-79
        private int gradeD; // 60-69
        private int gradeF; // 0-59

        public GradeDistribution(int a, int b, int c, int d, int f) {
            this.gradeA = a;
            this.gradeB = b;
            this.gradeC = c;
            this.gradeD = d;
            this.gradeF = f;
        }
    }
}