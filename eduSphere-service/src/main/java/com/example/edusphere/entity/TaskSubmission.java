package com.example.edusphere.entity;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Task Submission entity - separate from regular Submission
 */
@Data
@Document(collection = "task_submissions")
public class TaskSubmission {

    @Id
    private String id;

    @Field("course_id")
    private String courseId;

    @Field("task_id")
    private String taskId;

    @Field("student_id")
    private String studentId;

    // Submission content
    private String content; // Text content if any
    private String notes; // Student's notes about the submission

    // File attachments (can have multiple files)
    @Field("file_urls")
    private List<String> fileUrls = new ArrayList<>();

    @Field("file_names")
    private List<String> fileNames = new ArrayList<>();

    @Field("file_sizes")
    private List<Long> fileSizes = new ArrayList<>();

    // Grading information
    private Integer grade; // The grade or score for the submission
    private String feedback; // Feedback from the lecturer
    private String status = "submitted"; // "submitted", "graded", "returned", "late"

    // Submission metadata
    @Field("attempt_number")
    private Integer attemptNumber = 1;

    @Field("is_late")
    private Boolean isLate = false;

    @Field("late_penalty_applied")
    private Double latePenaltyApplied = 0.0; // Actual penalty applied

    @Field("original_due_date")
    private LocalDateTime originalDueDate; // When the task was originally due

    // Auto-grading results
    @Field("auto_graded")
    private Boolean autoGraded = false;

    @Field("auto_grade_score")
    private Integer autoGradeScore;

    @Field("manual_override")
    private Boolean manualOverride = false; // If instructor manually overrode auto-grade

    // Collaboration (for group tasks)
    @Field("group_members")
    private List<String> groupMembers = new ArrayList<>(); // Other student IDs in group

    @Field("is_group_submission")
    private Boolean isGroupSubmission = false;

    // Plagiarism detection
    @Field("plagiarism_score")
    private Double plagiarismScore; // If plagiarism check was run

    @Field("plagiarism_checked")
    private Boolean plagiarismChecked = false;

    // Timestamps
    @CreatedDate
    private LocalDateTime submittedAt;

    @Field("graded_at")
    private LocalDateTime gradedAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Field("time_spent")
    private Integer timeSpent; // Minutes spent on task (if tracked)

    // Helper methods
    public boolean hasFiles() {
        return fileUrls != null && !fileUrls.isEmpty();
    }

    public int getFileCount() {
        return fileUrls != null ? fileUrls.size() : 0;
    }

    public boolean isGraded() {
        return grade != null;
    }

    public boolean needsGrading() {
        return "submitted".equals(status) && grade == null;
    }

    public void addFile(String fileUrl, String fileName, Long fileSize) {
        if (fileUrls == null) fileUrls = new ArrayList<>();
        if (fileNames == null) fileNames = new ArrayList<>();
        if (fileSizes == null) fileSizes = new ArrayList<>();

        fileUrls.add(fileUrl);
        fileNames.add(fileName);
        fileSizes.add(fileSize);
    }

    public void removeFile(int index) {
        if (fileUrls != null && index >= 0 && index < fileUrls.size()) {
            fileUrls.remove(index);
            if (fileNames != null && index < fileNames.size()) {
                fileNames.remove(index);
            }
            if (fileSizes != null && index < fileSizes.size()) {
                fileSizes.remove(index);
            }
        }
    }

    public double getFinalGrade() {
        if (grade == null) return 0.0;

        double finalGrade = grade.doubleValue();

        // Apply late penalty if applicable
        if (isLate && latePenaltyApplied != null && latePenaltyApplied > 0) {
            finalGrade = finalGrade * (1.0 - (latePenaltyApplied / 100.0));
        }

        return Math.max(0.0, finalGrade); // Ensure grade doesn't go below 0
    }
}