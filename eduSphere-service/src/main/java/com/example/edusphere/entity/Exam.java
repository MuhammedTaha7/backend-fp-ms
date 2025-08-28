package com.example.edusphere.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

@Data
@Document(collection = "exams")
public class Exam {

    @Id
    private String id;

    @Field("course_id")
    private String courseId;

    @Field("instructor_id")
    private String instructorId;

    private String title;
    private String description;
    private String instructions;

    // Timing
    private Integer duration; // in minutes
    @Field("start_time")
    private LocalDateTime startTime;
    @Field("end_time")
    private LocalDateTime endTime;
    @Field("publish_time")
    private LocalDateTime publishTime;

    // Settings
    @Field("max_attempts")
    private Integer maxAttempts = 1;
    @Field("show_results")
    private Boolean showResults = true;
    @Field("shuffle_questions")
    private Boolean shuffleQuestions = false;
    @Field("shuffle_options")
    private Boolean shuffleOptions = false;
    @Field("allow_navigation")
    private Boolean allowNavigation = true;
    @Field("show_timer")
    private Boolean showTimer = true;
    @Field("auto_submit")
    private Boolean autoSubmit = true;
    @Field("require_safe_browser")
    private Boolean requireSafeBrowser = false;
    @Field("visible_to_students")
    private Boolean visibleToStudents = false;

    // Grading
    @Field("total_points")
    private Integer totalPoints = 0;
    @Field("pass_percentage")
    private Double passPercentage = 60.0;

    // Status
    private String status = "DRAFT"; // DRAFT, PUBLISHED, ACTIVE, COMPLETED, CANCELLED

    // Questions (embedded)
    private List<ExamQuestion> questions = new ArrayList<>();

    @CreatedDate
    @Field("created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Field("updated_at")
    private LocalDateTime updatedAt;

    // ===============================
    // STATUS HELPER METHODS
    // ===============================

    public boolean isActive() {
        LocalDateTime now = LocalDateTime.now();
        return "PUBLISHED".equals(status) &&
                now.isAfter(startTime) &&
                now.isBefore(endTime);
    }

    public boolean isUpcoming() {
        return LocalDateTime.now().isBefore(startTime);
    }

    public boolean isCompleted() {
        return LocalDateTime.now().isAfter(endTime);
    }

    public boolean canStudentTake() {
        return "PUBLISHED".equals(status) &&
                visibleToStudents &&
                isActive();
    }

    // ===============================
    // QUESTION MANAGEMENT METHODS
    // ===============================

    public void addQuestion(ExamQuestion question) {
        if (questions == null) {
            questions = new ArrayList<>();
        }
        questions.add(question);
        recalculateTotalPoints();
        System.out.println("✅ Added question to exam. New total: " + totalPoints + " points");
    }

    public void removeQuestion(String questionId) {
        if (questions != null) {
            boolean removed = questions.removeIf(q -> questionId.equals(q.getId()));
            if (removed) {
                recalculateTotalPoints();
                System.out.println("✅ Removed question from exam. New total: " + totalPoints + " points");
            }
        }
    }

    // ===============================
    // POINTS CALCULATION METHODS - COMPLETELY FIXED
    // ===============================

    /**
     * ✅ FIXED: Made this method public so it can be called from the service layer
     * Recalculates total points based on current questions
     */
    public void recalculateTotalPoints() {
        if (questions == null || questions.isEmpty()) {
            totalPoints = 0;
            System.out.println("📊 Recalculated total points: 0 (no questions)");
        } else {
            int calculatedTotal = questions.stream()
                    .mapToInt(q -> q.getPoints() != null ? q.getPoints() : 0)
                    .sum();

            totalPoints = calculatedTotal;
            System.out.println("📊 Recalculated total points: " + totalPoints + " (" + questions.size() + " questions)");

            // Debug: Show breakdown
            questions.forEach(q -> {
                System.out.println("  - Question " + q.getId() + ": " + q.getPoints() + " points (" + q.getType() + ")");
            });
        }
    }

    /**
     * ✅ COMPLETELY FIXED: Always ensures calculation is up to date
     * This getter always returns the correct current total
     */
    public Integer getTotalPoints() {
        // Always recalculate to ensure accuracy
        if (questions != null && !questions.isEmpty()) {
            int calculatedTotal = questions.stream()
                    .mapToInt(q -> q.getPoints() != null ? q.getPoints() : 0)
                    .sum();

            // Update if different or null
            if (totalPoints == null || !totalPoints.equals(calculatedTotal)) {
                int oldTotal = totalPoints != null ? totalPoints : 0;
                totalPoints = calculatedTotal;
                System.out.println("📊 Updated total points: " + oldTotal + " → " + totalPoints);
            }
        } else {
            if (totalPoints == null || totalPoints != 0) {
                totalPoints = 0;
                System.out.println("📊 Updated total points: → 0 (no questions)");
            }
        }

        return totalPoints;
    }

    /**
     * ✅ NEW: Force recalculation and return updated total
     */
    public Integer getUpdatedTotalPoints() {
        recalculateTotalPoints();
        return totalPoints;
    }

    // ===============================
    // VALIDATION METHODS
    // ===============================

    /**
     * ✅ ENHANCED: Comprehensive validation before publishing
     */
    public boolean canBePublished() {
        if (questions == null || questions.isEmpty()) {
            System.err.println("❌ Cannot publish exam: No questions added");
            return false;
        }

        // Check if all questions have valid points
        boolean allQuestionsValid = questions.stream()
                .allMatch(q -> q.getPoints() != null && q.getPoints() > 0);

        if (!allQuestionsValid) {
            System.err.println("❌ Cannot publish exam: Some questions have invalid points");
            questions.stream()
                    .filter(q -> q.getPoints() == null || q.getPoints() <= 0)
                    .forEach(q -> System.err.println("  - Question " + q.getId() + ": " + q.getPoints() + " points"));
            return false;
        }

        // Check if there are auto-gradable questions or if manual grading is set up
        if (!hasAutoGradableQuestions() && !hasValidManualGradingSetup()) {
            System.err.println("❌ Cannot publish exam: No auto-gradable questions and manual grading not configured");
            return false;
        }

        // Ensure total points are calculated
        recalculateTotalPoints();

        System.out.println("✅ Exam can be published: " + questions.size() + " questions, " + totalPoints + " total points");
        return true;
    }

    /**
     * ✅ NEW: Check if manual grading setup is valid
     */
    private boolean hasValidManualGradingSetup() {
        // If there are essay questions, manual grading is implied
        return questions.stream().anyMatch(ExamQuestion::isEssayQuestion);
    }

    // ===============================
    // ANALYSIS METHODS
    // ===============================

    /**
     * ✅ ENHANCED: Get question count with validation
     */
    public int getQuestionCount() {
        return questions != null ? questions.size() : 0;
    }

    /**
     * ✅ ENHANCED: Check if exam has auto-gradable questions
     */
    public boolean hasAutoGradableQuestions() {
        if (questions == null || questions.isEmpty()) {
            return false;
        }

        boolean hasAuto = questions.stream().anyMatch(ExamQuestion::canAutoGrade);
        System.out.println("🤖 Exam has auto-gradable questions: " + hasAuto);
        return hasAuto;
    }

    /**
     * ✅ ENHANCED: Check if exam requires manual grading
     */
    public boolean requiresManualGrading() {
        if (questions == null || questions.isEmpty()) {
            return false;
        }

        boolean requiresManual = questions.stream().anyMatch(q -> !q.canAutoGrade());
        System.out.println("👨‍🏫 Exam requires manual grading: " + requiresManual);
        return requiresManual;
    }

    /**
     * ✅ ENHANCED: Get detailed breakdown of question types and points
     */
    public String getQuestionTypeBreakdown() {
        if (questions == null || questions.isEmpty()) {
            return "No questions";
        }

        long multipleChoice = questions.stream().filter(ExamQuestion::isMultipleChoice).count();
        long trueFalse = questions.stream().filter(ExamQuestion::isTrueFalse).count();
        long textQuestions = questions.stream().filter(ExamQuestion::isTextQuestion).count();
        long essayQuestions = questions.stream().filter(ExamQuestion::isEssayQuestion).count();

        int mcPoints = questions.stream()
                .filter(ExamQuestion::isMultipleChoice)
                .mapToInt(q -> q.getPoints() != null ? q.getPoints() : 0)
                .sum();
        int tfPoints = questions.stream()
                .filter(ExamQuestion::isTrueFalse)
                .mapToInt(q -> q.getPoints() != null ? q.getPoints() : 0)
                .sum();
        int textPoints = questions.stream()
                .filter(ExamQuestion::isTextQuestion)
                .mapToInt(q -> q.getPoints() != null ? q.getPoints() : 0)
                .sum();
        int essayPoints = questions.stream()
                .filter(ExamQuestion::isEssayQuestion)
                .mapToInt(q -> q.getPoints() != null ? q.getPoints() : 0)
                .sum();

        return String.format("MC: %d (%dp), T/F: %d (%dp), Text: %d (%dp), Essay: %d (%dp)",
                multipleChoice, mcPoints, trueFalse, tfPoints,
                textQuestions, textPoints, essayQuestions, essayPoints);
    }

    /**
     * ✅ NEW: Get auto-grading statistics
     */
    public String getAutoGradingStats() {
        if (questions == null || questions.isEmpty()) {
            return "No questions";
        }

        long autoGradable = questions.stream().filter(ExamQuestion::canAutoGrade).count();
        long manualGrading = questions.stream().filter(q -> !q.canAutoGrade()).count();

        int autoPoints = questions.stream()
                .filter(ExamQuestion::canAutoGrade)
                .mapToInt(q -> q.getPoints() != null ? q.getPoints() : 0)
                .sum();
        int manualPoints = questions.stream()
                .filter(q -> !q.canAutoGrade())
                .mapToInt(q -> q.getPoints() != null ? q.getPoints() : 0)
                .sum();

        double autoPercentage = getTotalPoints() > 0 ? (autoPoints * 100.0) / getTotalPoints() : 0;

        return String.format("Auto-gradable: %d questions (%d points, %.1f%%), Manual: %d questions (%d points)",
                autoGradable, autoPoints, autoPercentage, manualGrading, manualPoints);
    }

    // ===============================
    // UTILITY METHODS
    // ===============================

    /**
     * ✅ NEW: Validate exam data integrity
     */
    public boolean isDataIntegrityValid() {
        try {
            // Check basic fields
            if (title == null || title.trim().isEmpty()) {
                System.err.println("❌ Exam validation failed: Title is required");
                return false;
            }

            if (courseId == null || instructorId == null) {
                System.err.println("❌ Exam validation failed: Course ID and Instructor ID are required");
                return false;
            }

            if (startTime == null || endTime == null) {
                System.err.println("❌ Exam validation failed: Start and end times are required");
                return false;
            }

            if (endTime.isBefore(startTime)) {
                System.err.println("❌ Exam validation failed: End time must be after start time");
                return false;
            }

            // Validate questions
            if (questions != null) {
                for (int i = 0; i < questions.size(); i++) {
                    ExamQuestion question = questions.get(i);
                    if (question.getPoints() == null || question.getPoints() <= 0) {
                        System.err.println("❌ Exam validation failed: Question " + (i + 1) + " has invalid points");
                        return false;
                    }
                }
            }

            // Ensure total points match
            int calculatedTotal = questions != null ?
                    questions.stream().mapToInt(q -> q.getPoints() != null ? q.getPoints() : 0).sum() : 0;

            if (totalPoints == null || !totalPoints.equals(calculatedTotal)) {
                System.out.println("⚠️ Total points mismatch detected - recalculating");
                recalculateTotalPoints();
            }

            System.out.println("✅ Exam data integrity validated successfully");
            return true;

        } catch (Exception e) {
            System.err.println("❌ Exam validation failed with exception: " + e.getMessage());
            return false;
        }
    }

    /**
     * ✅ NEW: Get comprehensive exam summary
     */
    public String getExamSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("Exam: ").append(title).append("\n");
        summary.append("Status: ").append(status).append("\n");
        summary.append("Questions: ").append(getQuestionCount()).append("\n");
        summary.append("Total Points: ").append(getTotalPoints()).append("\n");
        summary.append("Type Breakdown: ").append(getQuestionTypeBreakdown()).append("\n");
        summary.append("Grading: ").append(getAutoGradingStats()).append("\n");
        summary.append("Duration: ").append(duration).append(" minutes\n");
        summary.append("Pass Percentage: ").append(passPercentage).append("%\n");
        return summary.toString();
    }
}