// File: src/main/java/com/example/backend/eduSphere/service/impl/ExamServiceImpl.java
package com.example.edusphere.service.impl;

import com.example.edusphere.entity.Exam;
import com.example.edusphere.entity.ExamQuestion;
import com.example.edusphere.entity.ExamResponse;
import com.example.edusphere.entity.GradeColumn;
import com.example.edusphere.repository.ExamRepository;
import com.example.edusphere.repository.ExamResponseRepository;
import com.example.edusphere.repository.GradeColumnRepository;
import com.example.edusphere.service.ExamService;
import com.example.edusphere.service.GradeService;
import com.example.edusphere.dto.request.*;
import com.example.edusphere.dto.response.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import java.util.ArrayList;

@Service
@Transactional
public class ExamServiceImpl implements ExamService {

    private final ExamRepository examRepository;
    private final ExamResponseRepository examResponseRepository;
    private final GradeColumnRepository gradeColumnRepository;
    private final GradeService gradeService;

    public ExamServiceImpl(ExamRepository examRepository,
                           ExamResponseRepository examResponseRepository,
                           GradeColumnRepository gradeColumnRepository,
                           GradeService gradeService) {
        this.examRepository = examRepository;
        this.examResponseRepository = examResponseRepository;
        this.gradeColumnRepository = gradeColumnRepository;
        this.gradeService = gradeService;
    }

    @Override
    public List<Exam> getExamsByCourse(String courseId) {
        try {
            System.out.println("🔍 Fetching exams for course: " + courseId);
            List<Exam> exams = examRepository.findByCourseIdOrderByCreatedAtDesc(courseId);
            System.out.println("✅ Found " + exams.size() + " exams");
            return exams;
        } catch (Exception e) {
            System.err.println("❌ Error fetching exams: " + e.getMessage());
            throw new RuntimeException("Failed to fetch exams: " + e.getMessage());
        }
    }

    @Override
    public Exam getExamById(String examId) {
        return examRepository.findById(examId)
                .orElseThrow(() -> new RuntimeException("Exam not found with ID: " + examId));
    }

    @Override
    public Exam createExam(ExamCreateRequest request, String instructorId) {
        System.out.println("➕ Creating new exam: " + request.getTitle());

        // Validation
        if (request.getEndTime().isBefore(request.getStartTime())) {
            throw new RuntimeException("End time must be after start time");
        }

        LocalDateTime now = LocalDateTime.now();
        if (request.getStartTime().isBefore(now.minusHours(1))) {
            throw new RuntimeException("Start time cannot be more than 1 hour in the past");
        }

        try {
            // Create the exam first
            Exam exam = new Exam();
            exam.setTitle(request.getTitle());
            exam.setDescription(request.getDescription());
            exam.setInstructions(request.getInstructions());
            exam.setCourseId(request.getCourseId());
            exam.setInstructorId(instructorId);
            exam.setDuration(request.getDuration());
            exam.setStartTime(request.getStartTime());
            exam.setEndTime(request.getEndTime());
            exam.setPublishTime(request.getPublishTime());
            exam.setMaxAttempts(request.getMaxAttempts());
            exam.setShowResults(request.getShowResults());
            exam.setShuffleQuestions(request.getShuffleQuestions());
            exam.setShuffleOptions(request.getShuffleOptions());
            exam.setAllowNavigation(request.getAllowNavigation());
            exam.setShowTimer(request.getShowTimer());
            exam.setAutoSubmit(request.getAutoSubmit());
            exam.setRequireSafeBrowser(request.getRequireSafeBrowser());
            exam.setVisibleToStudents(request.getVisibleToStudents());
            exam.setPassPercentage(request.getPassPercentage());

            Exam savedExam = examRepository.save(exam);
            System.out.println("✅ Created exam with ID: " + savedExam.getId());

            // Create corresponding grade column
            createGradeColumnForExam(savedExam, instructorId);

            return savedExam;

        } catch (Exception e) {
            System.err.println("❌ Error creating exam: " + e.getMessage());
            throw new RuntimeException("Failed to create exam: " + e.getMessage(), e);
        }
    }

    // FIXED: Helper method to create grade column for exam with default percentage
    private void createGradeColumnForExam(Exam exam, String instructorId) {
        try {
            System.out.println("📊 Creating grade column for exam: " + exam.getTitle());

            // Get existing grade columns to calculate available percentage
            List<GradeColumn> existingColumns = gradeColumnRepository.findByCourseIdOrderByDisplayOrderDesc(exam.getCourseId());

            // Calculate current total percentage
            int currentTotal = existingColumns.stream()
                    .filter(col -> col.getPercentage() != null && col.getPercentage() > 0)
                    .mapToInt(col -> col.getPercentage())
                    .sum();

            // Calculate suggested percentage based on exam type
            int suggestedPercentage = calculateSuggestedPercentage("exam");

            // Ensure total doesn't exceed 100%
            if (currentTotal + suggestedPercentage > 100) {
                suggestedPercentage = Math.max(1, 100 - currentTotal);
            }

            System.out.println("📊 Suggested percentage: " + suggestedPercentage + "% (current total: " + currentTotal + "%)");

            // Get the next display order for this course
            Integer nextDisplayOrder = getNextDisplayOrderForCourse(exam.getCourseId());

            GradeColumn gradeColumn = new GradeColumn();
            gradeColumn.setName(exam.getTitle());
            gradeColumn.setType("exam");
            gradeColumn.setCourseId(exam.getCourseId());
            gradeColumn.setDescription("Auto-created grade column for exam: " + exam.getTitle());
            gradeColumn.setMaxPoints(exam.getTotalPoints() != null ? exam.getTotalPoints() : 100);
            gradeColumn.setPercentage(suggestedPercentage); // ← FIXED: Now sets default percentage
            gradeColumn.setIsActive(true);
            gradeColumn.setDisplayOrder(nextDisplayOrder);
            gradeColumn.setLinkedAssignmentId(exam.getId()); // Link to exam
            gradeColumn.setAutoCreated(true);
            gradeColumn.setCreatedBy(instructorId);

            GradeColumn savedColumn = gradeColumnRepository.save(gradeColumn);
            System.out.println("✅ Created grade column with ID: " + savedColumn.getId() +
                    " for exam: " + exam.getTitle() + " with " + suggestedPercentage + "% weight");

        } catch (Exception e) {
            System.err.println("⚠️ Failed to create grade column for exam: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // NEW: Calculate suggested percentage based on task type
    private int calculateSuggestedPercentage(String taskType) {
        if (taskType == null) return 10;

        return switch (taskType.toLowerCase()) {
            case "homework" -> 10;
            case "assignment" -> 15;
            case "project" -> 25;
            case "essay" -> 15;
            case "lab" -> 10;
            case "presentation" -> 15;
            case "quiz" -> 5;
            case "midterm" -> 15;
            case "exam", "final" -> 20;
            case "participation" -> 5;
            default -> 10;
        };
    }

    // Get next display order for grade columns in a course
    private Integer getNextDisplayOrderForCourse(String courseId) {
        try {
            List<GradeColumn> existingColumns = gradeColumnRepository.findByCourseIdOrderByDisplayOrderDesc(courseId);
            if (existingColumns.isEmpty()) {
                return 1;
            }
            Integer maxOrder = existingColumns.get(0).getDisplayOrder();
            return (maxOrder != null ? maxOrder : 0) + 1;
        } catch (Exception e) {
            System.err.println("⚠️ Error getting next display order: " + e.getMessage());
            return 1; // Default to 1 if error
        }
    }

    @Override
    public Exam updateExam(String examId, ExamUpdateRequest request, String instructorId) {
        System.out.println("🔄 Updating exam: " + examId);

        Exam exam = getExamById(examId);

        if (!exam.getInstructorId().equals(instructorId)) {
            throw new RuntimeException("Not authorized to update this exam");
        }

        try {
            // Store original title for grade column update
            String originalTitle = exam.getTitle();

            // Update fields if provided
            if (request.getTitle() != null) exam.setTitle(request.getTitle());
            if (request.getDescription() != null) exam.setDescription(request.getDescription());
            if (request.getInstructions() != null) exam.setInstructions(request.getInstructions());
            if (request.getDuration() != null) exam.setDuration(request.getDuration());
            if (request.getStartTime() != null) exam.setStartTime(request.getStartTime());
            if (request.getEndTime() != null) exam.setEndTime(request.getEndTime());
            if (request.getPublishTime() != null) exam.setPublishTime(request.getPublishTime());
            if (request.getMaxAttempts() != null) exam.setMaxAttempts(request.getMaxAttempts());
            if (request.getShowResults() != null) exam.setShowResults(request.getShowResults());
            if (request.getShuffleQuestions() != null) exam.setShuffleQuestions(request.getShuffleQuestions());
            if (request.getShuffleOptions() != null) exam.setShuffleOptions(request.getShuffleOptions());
            if (request.getAllowNavigation() != null) exam.setAllowNavigation(request.getAllowNavigation());
            if (request.getShowTimer() != null) exam.setShowTimer(request.getShowTimer());
            if (request.getAutoSubmit() != null) exam.setAutoSubmit(request.getAutoSubmit());
            if (request.getRequireSafeBrowser() != null) exam.setRequireSafeBrowser(request.getRequireSafeBrowser());
            if (request.getVisibleToStudents() != null) exam.setVisibleToStudents(request.getVisibleToStudents());
            if (request.getPassPercentage() != null) exam.setPassPercentage(request.getPassPercentage());
            if (request.getStatus() != null) exam.setStatus(request.getStatus());

            // Validate timing if updated
            if (exam.getEndTime().isBefore(exam.getStartTime())) {
                throw new RuntimeException("End time must be after start time");
            }

            Exam updatedExam = examRepository.save(exam);

            // Update corresponding grade column if title or points changed
            updateGradeColumnForExam(updatedExam, originalTitle, request.getTitle() != null);

            System.out.println("✅ Updated exam successfully");
            return updatedExam;

        } catch (Exception e) {
            System.err.println("❌ Error updating exam: " + e.getMessage());
            throw new RuntimeException("Failed to update exam: " + e.getMessage(), e);
        }
    }

    // Update grade column when exam is updated
    private void updateGradeColumnForExam(Exam exam, String originalTitle, boolean titleChanged) {
        try {
            // Find linked grade column
            List<GradeColumn> linkedColumns = gradeColumnRepository.findAllByCourseIdAndLinkedAssignmentId(
                    exam.getCourseId(), exam.getId());

            if (!linkedColumns.isEmpty()) {
                GradeColumn gradeColumn = linkedColumns.get(0);
                boolean columnUpdated = false;

                // Update title if changed
                if (titleChanged && !exam.getTitle().equals(originalTitle)) {
                    gradeColumn.setName(exam.getTitle());
                    columnUpdated = true;
                    System.out.println("📊 Updated grade column title from '" + originalTitle +
                            "' to '" + exam.getTitle() + "'");
                }

                // Update max points if changed
                Integer newMaxPoints = exam.getTotalPoints();
                if (newMaxPoints != null && !newMaxPoints.equals(gradeColumn.getMaxPoints())) {
                    gradeColumn.setMaxPoints(newMaxPoints);
                    columnUpdated = true;
                    System.out.println("📊 Updated grade column max points to: " + newMaxPoints);
                }

                if (columnUpdated) {
                    gradeColumnRepository.save(gradeColumn);
                    System.out.println("✅ Updated linked grade column for exam: " + exam.getTitle());
                }
            }
        } catch (Exception e) {
            System.err.println("⚠️ Failed to update grade column for exam: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void deleteExam(String examId, String instructorId) {
        System.out.println("🗑️ Deleting exam: " + examId);

        Exam exam = getExamById(examId);

        if (!exam.getInstructorId().equals(instructorId)) {
            throw new RuntimeException("Not authorized to delete this exam");
        }

        try {
            // Delete linked grade column first
            deleteGradeColumnForExam(exam);

            // Delete all responses
            examResponseRepository.deleteByExamId(examId);

            // Delete the exam
            examRepository.deleteById(examId);
            System.out.println("✅ Deleted exam and all responses");

        } catch (Exception e) {
            System.err.println("❌ Error deleting exam: " + e.getMessage());
            throw new RuntimeException("Failed to delete exam: " + e.getMessage(), e);
        }
    }

    // Delete grade column when exam is deleted
    private void deleteGradeColumnForExam(Exam exam) {
        try {
            List<GradeColumn> linkedColumns = gradeColumnRepository.findAllByCourseIdAndLinkedAssignmentId(
                    exam.getCourseId(), exam.getId());

            for (GradeColumn column : linkedColumns) {
                gradeColumnRepository.delete(column);
                System.out.println("🗑️ Deleted linked grade column: " + column.getName());
            }
        } catch (Exception e) {
            System.err.println("⚠️ Failed to delete grade column for exam: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public Exam publishExam(String examId, String instructorId) {
        System.out.println("📢 Publishing exam: " + examId);

        Exam exam = getExamById(examId);

        if (!exam.getInstructorId().equals(instructorId)) {
            throw new RuntimeException("Not authorized to publish this exam");
        }

        if (exam.getQuestions() == null || exam.getQuestions().isEmpty()) {
            throw new RuntimeException("Cannot publish exam without questions");
        }

        exam.setStatus("PUBLISHED");
        exam.setVisibleToStudents(true);

        if (exam.getPublishTime() == null) {
            exam.setPublishTime(LocalDateTime.now());
        }

        Exam publishedExam = examRepository.save(exam);

        // Update grade column max points when published
        updateGradeColumnMaxPoints(publishedExam);

        System.out.println("✅ Published exam successfully");
        return publishedExam;
    }

    // Update grade column max points
    private void updateGradeColumnMaxPoints(Exam exam) {
        try {
            List<GradeColumn> linkedColumns = gradeColumnRepository.findAllByCourseIdAndLinkedAssignmentId(
                    exam.getCourseId(), exam.getId());

            if (!linkedColumns.isEmpty()) {
                GradeColumn gradeColumn = linkedColumns.get(0);
                Integer examTotalPoints = exam.getTotalPoints();

                if (examTotalPoints != null && !examTotalPoints.equals(gradeColumn.getMaxPoints())) {
                    gradeColumn.setMaxPoints(examTotalPoints);
                    gradeColumnRepository.save(gradeColumn);
                    System.out.println("📊 Updated grade column max points to: " + examTotalPoints);
                }
            }
        } catch (Exception e) {
            System.err.println("⚠️ Failed to update grade column max points: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public Exam unpublishExam(String examId, String instructorId) {
        System.out.println("📝 Unpublishing exam: " + examId);

        Exam exam = getExamById(examId);

        if (!exam.getInstructorId().equals(instructorId)) {
            throw new RuntimeException("Not authorized to unpublish this exam");
        }

        exam.setStatus("DRAFT");
        exam.setVisibleToStudents(false);

        Exam unpublishedExam = examRepository.save(exam);
        System.out.println("✅ Unpublished exam successfully");
        return unpublishedExam;
    }

    @Override
    public Exam updateExamStatus(String examId, String status, String instructorId) {
        System.out.println("🔄 Updating exam status to: " + status);

        Exam exam = getExamById(examId);

        if (!exam.getInstructorId().equals(instructorId)) {
            throw new RuntimeException("Not authorized to update this exam");
        }

        exam.setStatus(status);

        Exam updatedExam = examRepository.save(exam);
        System.out.println("✅ Updated exam status successfully");
        return updatedExam;
    }

    @Override
    public ExamQuestion addQuestion(String examId, ExamQuestionRequest request, String instructorId) {
        System.out.println("➕ Adding question to exam: " + examId);

        Exam exam = getExamById(examId);

        if (!exam.getInstructorId().equals(instructorId)) {
            throw new RuntimeException("Not authorized to modify this exam");
        }

        ExamQuestion question = new ExamQuestion();
        question.setId(UUID.randomUUID().toString());
        question.setType(request.getType());
        question.setQuestion(request.getQuestion());
        question.setOptions(request.getOptions());
        question.setCorrectAnswer(request.getCorrectAnswer());
        question.setCorrectAnswerIndex(request.getCorrectAnswerIndex());
        question.setPoints(request.getPoints());
        question.setExplanation(request.getExplanation());
        question.setRequired(request.getRequired());
        question.setTimeLimit(request.getTimeLimit());
        question.setCaseSensitive(request.getCaseSensitive());
        question.setMaxLength(request.getMaxLength());
        question.setAcceptableAnswers(request.getAcceptableAnswers());

        // Set display order
        List<ExamQuestion> currentQuestions = exam.getQuestions();
        int nextOrder = (currentQuestions != null) ? currentQuestions.size() + 1 : 1;
        question.setDisplayOrder(nextOrder);

        // Validate question
        validateQuestion(question);

        exam.addQuestion(question);
        Exam savedExam = examRepository.save(exam);

        // Update grade column max points after adding question
        updateGradeColumnMaxPoints(savedExam);

        System.out.println("✅ Added question successfully");
        return question;
    }

    @Override
    public ExamQuestion updateQuestion(String examId, String questionId, ExamQuestionRequest request, String instructorId) {
        System.out.println("🔄 Updating question: " + questionId);

        Exam exam = getExamById(examId);

        if (!exam.getInstructorId().equals(instructorId)) {
            throw new RuntimeException("Not authorized to modify this exam");
        }

        ExamQuestion question = null;
        for (ExamQuestion q : exam.getQuestions()) {
            if (questionId.equals(q.getId())) {
                question = q;
                break;
            }
        }

        if (question == null) {
            throw new RuntimeException("Question not found");
        }

        // Update fields
        question.setType(request.getType());
        question.setQuestion(request.getQuestion());
        question.setOptions(request.getOptions());
        question.setCorrectAnswer(request.getCorrectAnswer());
        question.setCorrectAnswerIndex(request.getCorrectAnswerIndex());
        question.setPoints(request.getPoints());
        question.setExplanation(request.getExplanation());
        question.setRequired(request.getRequired());
        question.setTimeLimit(request.getTimeLimit());
        question.setCaseSensitive(request.getCaseSensitive());
        question.setMaxLength(request.getMaxLength());
        question.setAcceptableAnswers(request.getAcceptableAnswers());

        // Validate question
        validateQuestion(question);

        // Recalculate total points after updating question
        exam.recalculateTotalPoints();
        Exam savedExam = examRepository.save(exam);

        // Update grade column max points after updating question
        updateGradeColumnMaxPoints(savedExam);

        System.out.println("✅ Updated question successfully");
        return question;
    }

    @Override
    public void deleteQuestion(String examId, String questionId, String instructorId) {
        System.out.println("🗑️ Deleting question: " + questionId);

        Exam exam = getExamById(examId);

        if (!exam.getInstructorId().equals(instructorId)) {
            throw new RuntimeException("Not authorized to modify this exam");
        }

        exam.removeQuestion(questionId);
        Exam savedExam = examRepository.save(exam);

        // Update grade column max points after deleting question
        updateGradeColumnMaxPoints(savedExam);

        System.out.println("✅ Deleted question successfully");
    }

    @Override
    public void reorderQuestions(String examId, List<String> questionIds, String instructorId) {
        System.out.println("🔄 Reordering questions for exam: " + examId);

        Exam exam = getExamById(examId);

        if (!exam.getInstructorId().equals(instructorId)) {
            throw new RuntimeException("Not authorized to modify this exam");
        }

        // Update display order based on provided order
        for (int i = 0; i < questionIds.size(); i++) {
            String questionId = questionIds.get(i);
            int displayOrder = i + 1;

            // Find and update the question directly without lambda
            for (ExamQuestion question : exam.getQuestions()) {
                if (questionId.equals(question.getId())) {
                    question.setDisplayOrder(displayOrder);
                    break;
                }
            }
        }

        examRepository.save(exam);
        System.out.println("✅ Reordered questions successfully");
    }

    @Override
    public Exam getStudentExam(String examId, String studentId) {
        System.out.println("👀 Student viewing exam: " + examId);

        Exam exam = getExamById(examId);

        if (!exam.canStudentTake()) {
            throw new RuntimeException("Exam is not available for students");
        }

        // Remove sensitive information for students
        if (exam.getQuestions() != null) {
            for (ExamQuestion q : exam.getQuestions()) {
                q.setCorrectAnswer(null);
                q.setCorrectAnswerIndex(null);
                q.setAcceptableAnswers(null);
                q.setExplanation(null);
            }
        }

        return exam;
    }

    @Override
    public ExamResponse startExam(String examId, String studentId) {
        System.out.println("🎯 Student starting exam: " + examId);

        if (!canStudentTakeExam(examId, studentId)) {
            throw new RuntimeException("Student cannot take this exam");
        }

        if (hasActiveAttempt(examId, studentId)) {
            throw new RuntimeException("Student already has an active attempt");
        }

        Exam exam = getExamById(examId);

        // Ensure total points are calculated correctly
        exam.recalculateTotalPoints();

        ExamResponse response = new ExamResponse();
        response.setExamId(examId);
        response.setStudentId(studentId);
        response.setCourseId(exam.getCourseId());
        response.setStartedAt(LocalDateTime.now());
        response.setStatus("IN_PROGRESS");
        response.setMaxScore(exam.getTotalPoints());
        response.setAttemptNumber(getStudentAttemptCount(examId, studentId) + 1);

        ExamResponse savedResponse = examResponseRepository.save(response);
        System.out.println("✅ Started exam attempt: " + savedResponse.getId());
        return savedResponse;
    }

    @Override
    public ExamResponse saveProgress(ExamResponseRequest request, String studentId) {
        System.out.println("💾 Saving exam progress for student: " + studentId);

        ExamResponse response = examResponseRepository.findActiveResponse(request.getExamId(), studentId)
                .orElseThrow(() -> new RuntimeException("No active exam attempt found"));

        // Update answers
        if (request.getAnswers() != null) {
            response.getAnswers().putAll(request.getAnswers());
        }

        if (request.getTimeSpent() != null) {
            response.setTimeSpent(request.getTimeSpent());
        }

        ExamResponse savedResponse = examResponseRepository.save(response);
        System.out.println("✅ Saved exam progress");
        return savedResponse;
    }

    @Override
    public ExamResponse submitExam(ExamResponseRequest request, String studentId) {
        System.out.println("📤 Student submitting exam: " + request.getExamId());

        ExamResponse response = examResponseRepository.findActiveResponse(request.getExamId(), studentId)
                .orElseThrow(() -> new RuntimeException("No active exam attempt found"));

        // Update final answers
        if (request.getAnswers() != null) {
            response.getAnswers().putAll(request.getAnswers());
        }

        if (request.getTimeSpent() != null) {
            response.setTimeSpent(request.getTimeSpent());
        }

        response.setStatus("SUBMITTED");
        response.setSubmittedAt(LocalDateTime.now());

        // Check if late submission
        Exam exam = getExamById(request.getExamId());
        if (LocalDateTime.now().isAfter(exam.getEndTime())) {
            response.setLateSubmission(true);
        }

        ExamResponse submittedResponse = examResponseRepository.save(response);

        // Try auto-grading
        try {
            autoGradeResponse(submittedResponse.getId());
        } catch (Exception e) {
            System.err.println("⚠️ Auto-grading failed: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("✅ Submitted exam successfully");
        return submittedResponse;
    }

    /**
     * FIXED: Enhanced getExamResponses method with debugging
     * Replace this method in your ExamServiceImpl.java
     */

    @Override
    public List<ExamResponse> getExamResponses(String examId) {
        try {
            System.out.println("📊 === FETCHING RESPONSES FOR EXAM ===");
            System.out.println("📊 Exam ID: " + examId);

            // Debug: Check if exam exists first
            Exam exam = getExamById(examId);
            System.out.println("📊 Exam found: " + exam.getTitle() + " (Course: " + exam.getCourseId() + ")");

            // Try multiple repository methods to see which works
            List<ExamResponse> responses = null;

            // Method 1: Your current approach
            try {
                responses = examResponseRepository.findByExamIdOrderBySubmittedAtDesc(examId);
                System.out.println("📊 Method 1 (findByExamIdOrderBySubmittedAtDesc): Found " + responses.size() + " responses");
            } catch (Exception e) {
                System.err.println("❌ Method 1 failed: " + e.getMessage());
                responses = new ArrayList<>();
            }

            // Method 2: Try simple findByExamId if the ordered version fails
            if (responses.isEmpty()) {
                try {
                    responses = examResponseRepository.findByExamId(examId);
                    System.out.println("📊 Method 2 (findByExamId): Found " + responses.size() + " responses");
                } catch (Exception e) {
                    System.err.println("❌ Method 2 failed: " + e.getMessage());
                    responses = new ArrayList<>();
                }
            }

            // Method 3: Try findAll and filter (last resort for debugging)
            if (responses.isEmpty()) {
                try {
                    System.out.println("📊 Method 3: Trying findAll and filter...");
                    List<ExamResponse> allResponses = examResponseRepository.findAll();
                    System.out.println("📊 Total responses in database: " + allResponses.size());

                    // Filter manually to see what's happening
                    responses = new ArrayList<>();
                    for (ExamResponse response : allResponses) {
                        System.out.println("📊 Checking response: " + response.getId() +
                                " (examId: '" + response.getExamId() + "', target: '" + examId + "')");

                        if (examId.equals(response.getExamId())) {
                            responses.add(response);
                            System.out.println("📊 ✅ Match found!");
                        }
                    }
                    System.out.println("📊 Method 3 (manual filter): Found " + responses.size() + " responses");

                    // If we found responses this way, there might be an issue with the repository method
                    if (!responses.isEmpty()) {
                        System.err.println("⚠️ WARNING: Manual filtering found responses but repository methods didn't!");
                        System.err.println("⚠️ This suggests an issue with your repository method or MongoDB indexes");
                    }

                } catch (Exception e) {
                    System.err.println("❌ Method 3 failed: " + e.getMessage());
                    responses = new ArrayList<>();
                }
            }

            // Log detailed information about found responses
            if (!responses.isEmpty()) {
                System.out.println("📊 === RESPONSE DETAILS ===");
                for (int i = 0; i < Math.min(responses.size(), 3); i++) {
                    ExamResponse response = responses.get(i);
                    System.out.println("📊 Response " + (i + 1) + ":");
                    System.out.println("📊   ID: " + response.getId());
                    System.out.println("📊   ExamId: " + response.getExamId());
                    System.out.println("📊   StudentId: " + response.getStudentId());
                    System.out.println("📊   CourseId: " + response.getCourseId());
                    System.out.println("📊   Status: " + response.getStatus());
                    System.out.println("📊   Graded: " + response.getGraded());
                    System.out.println("📊   Submitted: " + response.getSubmittedAt());
                }
                if (responses.size() > 3) {
                    System.out.println("📊 ... and " + (responses.size() - 3) + " more responses");
                }
            } else {
                System.out.println("📊 ❌ NO RESPONSES FOUND");
                System.out.println("📊 Debugging info:");
                System.out.println("📊   Target examId: '" + examId + "'");
                System.out.println("📊   ExamId length: " + examId.length());
                System.out.println("📊   ExamId class: " + examId.getClass().getSimpleName());
            }

            System.out.println("✅ Found " + responses.size() + " responses for exam: " + examId);
            return responses;

        } catch (Exception e) {
            System.err.println("❌ Error fetching exam responses: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * ADDITIONAL DEBUGGING METHOD - Add this to help troubleshoot
     */
    public void debugExamResponses() {
        try {
            System.out.println("🔍 === DEBUG: ALL EXAM RESPONSES ===");
            List<ExamResponse> allResponses = examResponseRepository.findAll();
            System.out.println("🔍 Total responses in database: " + allResponses.size());

            for (ExamResponse response : allResponses) {
                System.out.println("🔍 Response: " + response.getId());
                System.out.println("🔍   ExamId: '" + response.getExamId() + "' (length: " +
                        (response.getExamId() != null ? response.getExamId().length() : "null") + ")");
                System.out.println("🔍   StudentId: " + response.getStudentId());
                System.out.println("🔍   CourseId: " + response.getCourseId());
                System.out.println("🔍   Status: " + response.getStatus());
                System.out.println("🔍   ---");
            }
        } catch (Exception e) {
            System.err.println("❌ Debug method failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public ExamResponse getResponse(String responseId) {
        return examResponseRepository.findById(responseId)
                .orElseThrow(() -> new RuntimeException("Response not found with ID: " + responseId));
    }

    @Override
    public List<ExamResponse> getStudentResponses(String studentId, String courseId) {
        return examResponseRepository.findByStudentIdAndCourseId(studentId, courseId);
    }

    // NEW: Get responses for a specific student and exam (response history)
    @Override
    public List<ExamResponse> getStudentExamResponses(String examId, String studentId) {
        System.out.println("📚 Fetching exam responses for student: " + studentId + " exam: " + examId);
        return examResponseRepository.findByExamIdAndStudentIdOrderByAttemptNumberDesc(examId, studentId);
    }

    @Override
    public ExamResponse gradeResponse(ExamGradeRequest request, String instructorId) {
        System.out.println("📝 === GRADING EXAM RESPONSE WITH GRADE COLUMN SYNC ===");
        System.out.println("Response ID: " + request.getResponseId());
        System.out.println("Grader: " + instructorId);

        ExamResponse response = getResponse(request.getResponseId());

        // Ensure the response has current exam max score
        Exam exam = getExamById(response.getExamId());
        exam.recalculateTotalPoints();
        response.setMaxScore(exam.getTotalPoints());

        // Update question scores
        response.setQuestionScores(request.getQuestionScores());
        response.setInstructorFeedback(request.getInstructorFeedback());
        response.setFlaggedForReview(request.getFlaggedForReview());
        response.setGraded(true);
        response.setAutoGraded(false);
        response.setGradedBy(instructorId);
        response.setGradedAt(LocalDateTime.now());
        response.setStatus("GRADED");

        // Recalculate totals
        response.recalculateTotal();

        // Determine if passed
        response.setPassed(response.getPercentage() >= exam.getPassPercentage());

        ExamResponse gradedResponse = examResponseRepository.save(response);
        System.out.println("✅ Graded exam response: " + gradedResponse.getTotalScore() + "/" + gradedResponse.getMaxScore());

        // Sync with grade column
        try {
            syncExamGradeToGradeColumn(gradedResponse, exam);
        } catch (Exception e) {
            System.err.println("⚠️ Failed to sync grade to grade column: " + e.getMessage());
            e.printStackTrace();
        }

        return gradedResponse;
    }

    // FIXED: Enhanced auto-grading method with better short answer handling
    @Override
    public ExamResponse autoGradeResponse(String responseId) {
        System.out.println("🤖 === AUTO-GRADING RESPONSE WITH GRADE COLUMN SYNC ===");
        System.out.println("Response ID: " + responseId);

        ExamResponse response = getResponse(responseId);
        Exam exam = getExamById(response.getExamId());

        // Ensure exam total points are up to date
        exam.recalculateTotalPoints();

        // Update response max score to current exam total
        response.setMaxScore(exam.getTotalPoints());

        System.out.println("📊 Exam total points: " + exam.getTotalPoints());
        System.out.println("📊 Response max score updated to: " + response.getMaxScore());

        Map<String, Integer> autoScores = new HashMap<>();
        boolean hasManualGradingRequired = false;

        // Enhanced grading logic with better error handling
        for (ExamQuestion question : exam.getQuestions()) {
            System.out.println("🔍 Processing question: " + question.getId() + " (Type: " + question.getType() + ")");

            // FIXED: Use our enhanced method to check if question can be auto-graded
            boolean canAutoGrade = canQuestionBeAutoGraded(question);
            System.out.println("🔍 Can auto-grade: " + canAutoGrade);

            if (canAutoGrade) {
                try {
                    String studentAnswer = response.getAnswers().get(question.getId());
                    System.out.println("🔍 Student answer: '" + studentAnswer + "' for question: " + question.getId());

                    int score = gradeQuestionWithEnhancedDebugging(question, studentAnswer);
                    autoScores.put(question.getId(), score);

                    System.out.println("🔍 Auto-graded question " + question.getId() +
                            ": " + score + "/" + question.getPoints() + " points");
                } catch (Exception e) {
                    System.err.println("⚠️ Error grading question " + question.getId() + ": " + e.getMessage());
                    e.printStackTrace();
                    // Set score to 0 for failed auto-grading
                    autoScores.put(question.getId(), 0);
                }
            } else {
                // Essay questions or other non-auto-gradable questions
                hasManualGradingRequired = true;
                System.out.println("🔍 Question " + question.getId() + " requires manual grading");
            }
        }

        // Update response with auto-graded scores, preserve manual grades
        Map<String, Integer> existingScores = response.getQuestionScores();
        if (existingScores == null) {
            existingScores = new HashMap<>();
        }

        // Only update scores for auto-gradable questions, preserve manual grades
        existingScores.putAll(autoScores);
        response.setQuestionScores(existingScores);

        // Set appropriate grading status
        if (hasManualGradingRequired) {
            response.setStatus("PARTIALLY_GRADED");
            response.setGraded(false); // Still needs manual review
            System.out.println("⚠️ Response partially auto-graded - manual grading required");
        } else {
            response.setStatus("GRADED");
            response.setGraded(true);
            System.out.println("✅ Response fully auto-graded");
        }

        response.setAutoGraded(true);
        response.setGradedAt(LocalDateTime.now());

        // Recalculate totals
        response.recalculateTotal();

        // Determine if passed (only if fully graded)
        if (!hasManualGradingRequired) {
            response.setPassed(response.getPercentage() >= exam.getPassPercentage());
        }

        ExamResponse gradedResponse = examResponseRepository.save(response);

        System.out.println("✅ Auto-graded response: " + gradedResponse.getTotalScore() +
                "/" + gradedResponse.getMaxScore() + " points (" +
                String.format("%.1f", gradedResponse.getPercentage()) + "%)");

        // Sync with grade column (only if fully graded)
        if (!hasManualGradingRequired) {
            try {
                syncExamGradeToGradeColumn(gradedResponse, exam);
            } catch (Exception e) {
                System.err.println("⚠️ Failed to sync grade to grade column: " + e.getMessage());
                e.printStackTrace();
            }
        }

        return gradedResponse;
    }

    // FIXED: Better method to check if a question can be auto-graded
    private boolean canQuestionBeAutoGraded(ExamQuestion question) {
        System.out.println("🔧 === CHECKING AUTO-GRADE CAPABILITY ===");
        System.out.println("🔧 Question ID: " + question.getId());
        System.out.println("🔧 Question Type: " + question.getType());

        if (question.getType() == null) {
            System.out.println("❌ Question type is null");
            return false;
        }

        String type = question.getType().toLowerCase().trim();
        System.out.println("🔧 Normalized type: '" + type + "'");

        switch (type) {
            case "multiple-choice":
            case "multiple_choice":
            case "multiplechoice":
                boolean hasOptions = question.getOptions() != null && !question.getOptions().isEmpty();
                boolean hasCorrectIndex = question.getCorrectAnswerIndex() != null;
                System.out.println("🔧 Multiple Choice - Has options: " + hasOptions + ", Has correct index: " + hasCorrectIndex);
                return hasOptions && hasCorrectIndex;

            case "true-false":
            case "true_false":
            case "truefalse":
            case "boolean":
                boolean hasCorrectAnswer = question.getCorrectAnswer() != null && !question.getCorrectAnswer().trim().isEmpty();
                System.out.println("🔧 True/False - Has correct answer: " + hasCorrectAnswer);
                return hasCorrectAnswer;

            case "short-answer":
            case "short_answer":
            case "shortanswer":
            case "text":
            case "fill-in-the-blank":
            case "fill_in_the_blank":
                List<String> acceptableAnswers = question.getAcceptableAnswers();
                boolean hasAcceptableAnswers = acceptableAnswers != null && !acceptableAnswers.isEmpty();
                System.out.println("🔧 Short Answer - Has acceptable answers: " + hasAcceptableAnswers);
                if (hasAcceptableAnswers) {
                    System.out.println("🔧 Acceptable answers count: " + acceptableAnswers.size());
                    for (int i = 0; i < acceptableAnswers.size(); i++) {
                        String answer = acceptableAnswers.get(i);
                        boolean isValidAnswer = answer != null && !answer.trim().isEmpty();
                        System.out.println("🔧 Answer " + i + ": '" + answer + "' (valid: " + isValidAnswer + ")");
                    }
                    // Check if at least one acceptable answer is valid
                    for (String answer : acceptableAnswers) {
                        if (answer != null && !answer.trim().isEmpty()) {
                            System.out.println("✅ Found at least one valid acceptable answer");
                            return true;
                        }
                    }
                    System.out.println("❌ No valid acceptable answers found");
                    return false;
                }
                return false;

            case "essay":
            case "long-answer":
            case "long_answer":
            case "paragraph":
                System.out.println("🔧 Essay question - requires manual grading");
                return false;

            default:
                System.out.println("🔧 Unknown question type: '" + type + "' - requires manual grading");
                return false;
        }
    }

    // FIXED: Enhanced grading method with better debugging
    private int gradeQuestionWithEnhancedDebugging(ExamQuestion question, String studentAnswer) {
        System.out.println("🔍 === GRADING QUESTION WITH ENHANCED DEBUG ===");
        System.out.println("🔍 Question ID: " + question.getId());
        System.out.println("🔍 Question Type: " + question.getType());
        System.out.println("🔍 Student Answer: '" + studentAnswer + "'");

        if (studentAnswer == null || studentAnswer.trim().isEmpty()) {
            System.out.println("🔍 Empty answer for question " + question.getId());
            return 0;
        }

        try {
            String type = question.getType().toLowerCase().trim();

            switch (type) {
                case "multiple-choice":
                case "multiple_choice":
                case "multiplechoice":
                    return gradeMultipleChoice(question, studentAnswer);

                case "true-false":
                case "true_false":
                case "truefalse":
                case "boolean":
                    return gradeTrueFalse(question, studentAnswer);

                case "short-answer":
                case "short_answer":
                case "shortanswer":
                case "text":
                case "fill-in-the-blank":
                case "fill_in_the_blank":
                    return gradeTextQuestionWithEnhancedDebugging(question, studentAnswer);

                default:
                    System.out.println("🔍 Question type '" + type + "' requires manual grading");
                    return 0;
            }

        } catch (Exception e) {
            System.err.println("❌ Error grading question " + question.getId() + ": " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }

    // FIXED: Enhanced text question grading with detailed debugging
    private int gradeTextQuestionWithEnhancedDebugging(ExamQuestion question, String studentAnswer) {
        System.out.println("🔍 === DEBUGGING TEXT QUESTION GRADING ===");
        System.out.println("🔍 Question ID: " + question.getId());
        System.out.println("🔍 Question Type: " + question.getType());
        System.out.println("🔍 Student Answer: '" + studentAnswer + "'");
        System.out.println("🔍 Student Answer Length: " + (studentAnswer != null ? studentAnswer.length() : "null"));

        List<String> acceptableAnswers = question.getAcceptableAnswers();
        System.out.println("🔍 Acceptable Answers: " + acceptableAnswers);
        System.out.println("🔍 Acceptable Answers Size: " + (acceptableAnswers != null ? acceptableAnswers.size() : "null"));

        if (acceptableAnswers != null && !acceptableAnswers.isEmpty()) {
            for (int i = 0; i < acceptableAnswers.size(); i++) {
                System.out.println("🔍 Acceptable Answer " + i + ": '" + acceptableAnswers.get(i) + "'");
            }
        }

        Boolean caseSensitive = question.getCaseSensitive();
        boolean isCaseSensitive = caseSensitive != null && caseSensitive;
        System.out.println("🔍 Case Sensitive: " + isCaseSensitive);

        Integer questionPoints = question.getPoints();
        System.out.println("🔍 Question Points: " + questionPoints);

        if (acceptableAnswers == null || acceptableAnswers.isEmpty()) {
            System.out.println("⚠️ No acceptable answers set for text question " + question.getId() + " - skipping auto-grade");
            return 0;
        }

        // Trim student answer and handle null case
        if (studentAnswer == null) {
            System.out.println("❌ Student answer is null");
            return 0;
        }

        String trimmedStudentAnswer = studentAnswer.trim();
        if (trimmedStudentAnswer.isEmpty()) {
            System.out.println("❌ Student answer is empty after trimming");
            return 0;
        }

        final String answerToCheck = isCaseSensitive ? trimmedStudentAnswer : trimmedStudentAnswer.toLowerCase();
        System.out.println("🔍 Answer to check: '" + answerToCheck + "'");

        // Check if answer matches any acceptable answer
        boolean correct = false;
        for (int i = 0; i < acceptableAnswers.size(); i++) {
            String acceptable = acceptableAnswers.get(i);
            System.out.println("🔍 Checking against acceptable answer " + i + ": '" + acceptable + "'");

            if (acceptable == null) {
                System.out.println("🔍 Acceptable answer " + i + " is null - skipping");
                continue;
            }

            String trimmedAcceptable = acceptable.trim();
            if (trimmedAcceptable.isEmpty()) {
                System.out.println("🔍 Acceptable answer " + i + " is empty after trimming - skipping");
                continue;
            }

            String acceptableAnswer = isCaseSensitive ? trimmedAcceptable : trimmedAcceptable.toLowerCase();
            System.out.println("🔍 Processed acceptable answer " + i + ": '" + acceptableAnswer + "'");

            if (acceptableAnswer.equals(answerToCheck)) {
                System.out.println("✅ MATCH FOUND! Acceptable answer " + i + " matches student answer");
                correct = true;
                break;
            } else {
                System.out.println("❌ No match for acceptable answer " + i);
                System.out.println("❌ Expected: '" + acceptableAnswer + "' (length: " + acceptableAnswer.length() + ")");
                System.out.println("❌ Actual: '" + answerToCheck + "' (length: " + answerToCheck.length() + ")");

                // Character-by-character comparison for debugging
                if (acceptableAnswer.length() == answerToCheck.length()) {
                    System.out.println("🔍 Same length - checking character differences:");
                    for (int j = 0; j < acceptableAnswer.length(); j++) {
                        char expected = acceptableAnswer.charAt(j);
                        char actual = answerToCheck.charAt(j);
                        if (expected != actual) {
                            System.out.println("🔍 Difference at position " + j + ": expected '" + expected + "' (code: " + (int)expected + "), got '" + actual + "' (code: " + (int)actual + ")");
                        }
                    }
                }
            }
        }

        int pointsToAward = 0;
        if (correct) {
            pointsToAward = questionPoints != null ? questionPoints : 0;
            System.out.println("✅ Correct text answer for question " + question.getId() + " - awarding " + pointsToAward + " points");
        } else {
            System.out.println("❌ Incorrect text answer for question " + question.getId() + " - awarding 0 points");
        }

        System.out.println("🔍 Final result: awarding " + pointsToAward + " points");
        return pointsToAward;
    }

    // NEW: Update individual question score
    @Override
    public ExamResponse updateQuestionScore(String responseId, String questionId, Integer score, String feedback, String instructorId) {
        System.out.println("🔢 === UPDATING INDIVIDUAL QUESTION SCORE ===");
        System.out.println("Response ID: " + responseId + ", Question ID: " + questionId + ", Score: " + score);

        ExamResponse response = getResponse(responseId);
        Exam exam = getExamById(response.getExamId());

        // Find the question to validate max points
        ExamQuestion question = null;
        for (ExamQuestion q : exam.getQuestions()) {
            if (questionId.equals(q.getId())) {
                question = q;
                break;
            }
        }

        if (question == null) {
            throw new RuntimeException("Question not found in exam");
        }

        // Validate score
        if (score < 0 || score > question.getPoints()) {
            throw new RuntimeException("Score must be between 0 and " + question.getPoints());
        }

        // Update the question score
        Map<String, Integer> questionScores = response.getQuestionScores();
        if (questionScores == null) {
            questionScores = new HashMap<>();
        }
        questionScores.put(questionId, score);
        response.setQuestionScores(questionScores);

        // Update grading info
        response.setGradedBy(instructorId);
        response.setGradedAt(LocalDateTime.now());

        // Recalculate totals
        response.recalculateTotal();

        // Check if all questions are now graded
        // Check if all questions are now graded without using lambda
        boolean allQuestionsGraded = true;
        for (ExamQuestion q : exam.getQuestions()) {
            if (!questionScores.containsKey(q.getId())) {
                allQuestionsGraded = false;
                break;
            }
        }

        if (allQuestionsGraded) {
            response.setGraded(true);
            response.setStatus("GRADED");
            response.setPassed(response.getPercentage() >= exam.getPassPercentage());
        }

        ExamResponse updatedResponse = examResponseRepository.save(response);

        // Sync with grade column if fully graded
        if (allQuestionsGraded) {
            try {
                syncExamGradeToGradeColumn(updatedResponse, exam);
            } catch (Exception e) {
                System.err.println("⚠️ Failed to sync grade to grade column: " + e.getMessage());
                e.printStackTrace();
            }
        }

        System.out.println("✅ Updated question score successfully");
        return updatedResponse;
    }

    // NEW: Flag response for review
    @Override
    public ExamResponse flagResponseForReview(String responseId, String reason, String priority, String instructorId) {
        System.out.println("🚩 === FLAGGING RESPONSE FOR REVIEW ===");
        System.out.println("Response ID: " + responseId + ", Reason: " + reason);

        ExamResponse response = getResponse(responseId);

        response.setFlaggedForReview(true);
        if (response.getInstructorFeedback() == null) {
            response.setInstructorFeedback("");
        }
        response.setInstructorFeedback(response.getInstructorFeedback() + "\n[FLAGGED: " + reason + "]");

        ExamResponse flaggedResponse = examResponseRepository.save(response);
        System.out.println("✅ Response flagged for review successfully");
        return flaggedResponse;
    }

    // NEW: Unflag response
    @Override
    public ExamResponse unflagResponse(String responseId, String instructorId) {
        System.out.println("🚩 === UNFLAGGING RESPONSE ===");
        System.out.println("Response ID: " + responseId);

        ExamResponse response = getResponse(responseId);

        response.setFlaggedForReview(false);
        // Remove flag markers from feedback
        if (response.getInstructorFeedback() != null) {
            response.setInstructorFeedback(
                    response.getInstructorFeedback().replaceAll("\\n\\[FLAGGED:.*?\\]", "")
            );
        }

        ExamResponse unflaggedResponse = examResponseRepository.save(response);
        System.out.println("✅ Response unflagged successfully");
        return unflaggedResponse;
    }

    // NEW: Batch grade multiple responses
    @Override
    public List<ExamResponse> batchGradeResponses(List<String> responseIds, String instructorFeedback, Boolean flagForReview, String instructorId) {
        System.out.println("📦 === BATCH GRADING RESPONSES ===");
        System.out.println("Grading " + responseIds.size() + " responses");

        List<ExamResponse> batchGradedResponses = new ArrayList<>();

        for (String responseId : responseIds) {
            try {
                ExamResponse response = getResponse(responseId);

                // Update feedback if provided
                if (instructorFeedback != null && !instructorFeedback.trim().isEmpty()) {
                    response.setInstructorFeedback(instructorFeedback);
                }

                // Flag for review if requested
                if (flagForReview != null && flagForReview) {
                    response.setFlaggedForReview(true);
                }

                response.setGradedBy(instructorId);
                response.setGradedAt(LocalDateTime.now());

                ExamResponse savedResponse = examResponseRepository.save(response);
                batchGradedResponses.add(savedResponse);

            } catch (Exception e) {
                System.err.println("❌ Error batch grading response " + responseId + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        System.out.println("✅ Batch grading completed for " + batchGradedResponses.size() + " responses");
        return batchGradedResponses;
    }

    // NEW: Get grading statistics for an exam
    @Override
    public Map<String, Object> getExamGradingStats(String examId) {
        System.out.println("📊 === CALCULATING GRADING STATISTICS ===");
        System.out.println("Exam ID: " + examId);

        List<ExamResponse> responses = examResponseRepository.findByExamId(examId);

        Map<String, Object> stats = new HashMap<>();

        if (responses.isEmpty()) {
            stats.put("totalResponses", 0);
            stats.put("gradedResponses", 0);
            stats.put("autoGradedResponses", 0);
            stats.put("manuallyGradedResponses", 0);
            stats.put("needsGrading", 0);
            stats.put("flaggedResponses", 0);
            stats.put("averageGradingTimeSeconds", 0);
            stats.put("gradingProgress", 0.0);
            return stats;
        }

        // Calculate statistics without lambda expressions
        long totalResponses = responses.size();

        long gradedResponses = 0;
        long autoGradedResponses = 0;
        long manuallyGradedResponses = 0;
        long needsGrading = 0;
        long flaggedResponses = 0;
        long passedResponses = 0;
        long inProgressResponses = 0;
        long submittedResponses = 0;

        for (ExamResponse response : responses) {
            if (Boolean.TRUE.equals(response.getGraded())) {
                gradedResponses++;
                if (Boolean.TRUE.equals(response.getAutoGraded())) {
                    autoGradedResponses++;
                } else {
                    manuallyGradedResponses++;
                }
            }

            if (!Boolean.TRUE.equals(response.getGraded()) && "SUBMITTED".equals(response.getStatus())) {
                needsGrading++;
            }

            if (Boolean.TRUE.equals(response.getFlaggedForReview())) {
                flaggedResponses++;
            }

            if (Boolean.TRUE.equals(response.getPassed())) {
                passedResponses++;
            }

            if ("IN_PROGRESS".equals(response.getStatus())) {
                inProgressResponses++;
            }

            if ("SUBMITTED".equals(response.getStatus())) {
                submittedResponses++;
            }
        }

        double gradingProgress = totalResponses > 0 ? (gradedResponses * 100.0) / totalResponses : 0.0;

        stats.put("totalResponses", totalResponses);
        stats.put("gradedResponses", gradedResponses);
        stats.put("autoGradedResponses", autoGradedResponses);
        stats.put("manuallyGradedResponses", manuallyGradedResponses);
        stats.put("needsGrading", needsGrading);
        stats.put("flaggedResponses", flaggedResponses);
        stats.put("passedResponses", passedResponses);
        stats.put("gradingProgress", Math.round(gradingProgress * 100.0) / 100.0);
        stats.put("inProgressResponses", responses.stream().filter(r -> "IN_PROGRESS".equals(r.getStatus())).count());
        stats.put("submittedResponses", responses.stream().filter(r -> "SUBMITTED".equals(r.getStatus())).count());

        System.out.println("✅ Grading statistics calculated successfully");
        return stats;
    }

    // Sync exam grade to grade column
    private void syncExamGradeToGradeColumn(ExamResponse examResponse, Exam exam) {
        System.out.println("🔄 === SYNCING EXAM GRADE TO GRADE COLUMN ===");
        System.out.println("Student: " + examResponse.getStudentId());
        System.out.println("Exam: " + exam.getTitle());
        System.out.println("Score: " + examResponse.getTotalScore() + "/" + examResponse.getMaxScore() +
                " (" + String.format("%.2f", examResponse.getPercentage()) + "%)");

        try {
            // Find linked grade column for this exam
            List<GradeColumn> linkedColumns = gradeColumnRepository.findAllByCourseIdAndLinkedAssignmentId(
                    exam.getCourseId(), exam.getId());

            if (linkedColumns.isEmpty()) {
                System.out.println("⚠️ No linked grade column found for exam: " + exam.getTitle());
                return;
            }

            GradeColumn gradeColumn = linkedColumns.get(0);
            System.out.println("📊 Found linked grade column: " + gradeColumn.getName() + " (ID: " + gradeColumn.getId() + ")");

            // Update the grade column's max points to match current exam total
            if (!exam.getTotalPoints().equals(gradeColumn.getMaxPoints())) {
                gradeColumn.setMaxPoints(exam.getTotalPoints());
                gradeColumnRepository.save(gradeColumn);
                System.out.println("📊 Updated grade column max points to " + exam.getTotalPoints());
            }

            // Calculate the grade as a percentage (0-100)
            Double gradePercentage = examResponse.getPercentage();

            // Use GradeService to update the student's grade for this column
            gradeService.updateStudentGrade(examResponse.getStudentId(), gradeColumn.getId(), gradePercentage);

            System.out.println("✅ Successfully synced exam grade to grade column");
            System.out.println("📊 Student " + examResponse.getStudentId() + " received " +
                    String.format("%.2f", gradePercentage) + "% for " + gradeColumn.getName());

        } catch (Exception e) {
            System.err.println("❌ Error syncing exam grade to grade column: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to sync exam grade to grade column: " + e.getMessage());
        }
    }

    @Override
    public List<ExamResponse> autoGradeAllResponses(String examId) {
        System.out.println("🤖 Auto-grading all responses for exam: " + examId);

        // Verify exam exists and get updated total points
        Exam exam = getExamById(examId);
        exam.recalculateTotalPoints();
        examRepository.save(exam);

        List<ExamResponse> ungraded = examResponseRepository.findUngraded(examId);
        List<ExamResponse> graded = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;

        System.out.println("📊 Found " + ungraded.size() + " ungraded responses");

        for (ExamResponse response : ungraded) {
            try {
                // Skip if response is already being manually graded
                if ("GRADING_IN_PROGRESS".equals(response.getStatus())) {
                    System.out.println("⏭️ Skipping response " + response.getId() + " - manual grading in progress");
                    continue;
                }

                ExamResponse gradedResponse = autoGradeResponse(response.getId());
                graded.add(gradedResponse);
                successCount++;

            } catch (Exception e) {
                System.err.println("❌ Failed to auto-grade response " + response.getId() + ": " + e.getMessage());
                e.printStackTrace();
                failCount++;

                // Mark as failed auto-grading for manual review
                try {
                    response.setStatus("AUTO_GRADE_FAILED");
                    response.setInstructorFeedback("Auto-grading failed: " + e.getMessage());
                    examResponseRepository.save(response);
                } catch (Exception saveError) {
                    System.err.println("❌ Failed to save error status: " + saveError.getMessage());
                }
            }
        }

        System.out.println("✅ Auto-grading completed: " + successCount + " successful, " +
                failCount + " failed out of " + ungraded.size() + " total responses");

        return graded;
    }

    @Override
    public ExamStatsResponse getExamStats(String examId) {
        System.out.println("📊 Calculating stats for exam: " + examId);

        Exam exam = getExamById(examId);
        List<ExamResponse> responses = examResponseRepository.findByExamId(examId);

        // Calculate graded responses without lambda
        List<ExamResponse> graded = new ArrayList<>();
        for (ExamResponse response : responses) {
            if (Boolean.TRUE.equals(response.getGraded())) {
                graded.add(response);
            }
        }

        // Calculate average score without lambda
        double averageScore = 0.0;
        if (!graded.isEmpty()) {
            double total = 0.0;
            for (ExamResponse response : graded) {
                total += response.getPercentage();
            }
            averageScore = total / graded.size();
        }

        // Calculate passed count without lambda
        long passed = 0;
        for (ExamResponse response : graded) {
            if (Boolean.TRUE.equals(response.getPassed())) {
                passed++;
            }
        }

        // Calculate submitted count without lambda
        long submittedCount = 0;
        for (ExamResponse response : responses) {
            if (response.isSubmitted()) {
                submittedCount++;
            }
        }

        return ExamStatsResponse.builder()
                .examId(examId)
                .examTitle(exam.getTitle())
                .totalResponses((long) responses.size())
                .submittedResponses(submittedCount)
                .gradedResponses((long) graded.size())
                .passedResponses(passed)
                .averageScore(averageScore)
                .passRate(graded.isEmpty() ? 0.0 : (passed * 100.0) / graded.size())
                .completionRate(responses.isEmpty() ? 0.0 : (submittedCount * 100.0) / responses.size())
                .build();
    }

    @Override
    public List<ExamStatsResponse> getCourseExamStats(String courseId) {
        List<Exam> exams = examRepository.findByCourseId(courseId);
        List<ExamStatsResponse> statsResponses = new ArrayList<>();

        for (Exam exam : exams) {
            ExamStatsResponse stats = getExamStats(exam.getId());
            statsResponses.add(stats);
        }

        return statsResponses;
    }

    @Override
    public boolean canStudentTakeExam(String examId, String studentId) {
        Exam exam = getExamById(examId);

        if (!exam.canStudentTake()) {
            return false;
        }

        int attemptCount = getStudentAttemptCount(examId, studentId);
        return attemptCount < exam.getMaxAttempts();
    }

    @Override
    public int getStudentAttemptCount(String examId, String studentId) {
        return (int) examResponseRepository.countByExamIdAndStudentId(examId, studentId);
    }

    @Override
    public boolean hasActiveAttempt(String examId, String studentId) {
        return examResponseRepository.findActiveResponse(examId, studentId).isPresent();
    }

    // ===============================
    // HELPER METHODS
    // ===============================

    private void validateQuestion(ExamQuestion question) {
        if (question.isMultipleChoice()) {
            if (question.getOptions() == null || question.getOptions().size() < 2) {
                throw new RuntimeException("Multiple choice questions need at least 2 options");
            }
            if (question.getCorrectAnswerIndex() == null ||
                    question.getCorrectAnswerIndex() >= question.getOptions().size()) {
                throw new RuntimeException("Invalid correct answer index");
            }
        }
    }

    // ORIGINAL: Keep the original gradeQuestion method for backward compatibility
    private int gradeQuestion(ExamQuestion question, String studentAnswer) {
        if (studentAnswer == null || studentAnswer.trim().isEmpty()) {
            System.out.println("🔍 Empty answer for question " + question.getId());
            return 0;
        }

        try {
            if (question.isMultipleChoice()) {
                return gradeMultipleChoice(question, studentAnswer);
            }

            if (question.isTrueFalse()) {
                return gradeTrueFalse(question, studentAnswer);
            }

            if (question.isTextQuestion()) {
                return gradeTextQuestion(question, studentAnswer);
            }

            // Essay questions cannot be auto-graded
            System.out.println("🔍 Essay question " + question.getId() + " requires manual grading");
            return 0;

        } catch (Exception e) {
            System.err.println("❌ Error grading question " + question.getId() + ": " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }

    private int gradeMultipleChoice(ExamQuestion question, String studentAnswer) {
        Integer correctIndex = question.getCorrectAnswerIndex();
        if (correctIndex == null) {
            System.err.println("❌ No correct answer index set for multiple choice question " + question.getId());
            return 0;
        }

        List<String> options = question.getOptions();
        if (options == null || options.isEmpty()) {
            System.err.println("❌ No options available for multiple choice question " + question.getId());
            return 0;
        }

        int studentAnswerIndex = -1;

        // Try to parse as index first (for backward compatibility)
        try {
            studentAnswerIndex = Integer.parseInt(studentAnswer.trim());
            System.out.println("🔍 Parsed student answer as index: " + studentAnswerIndex);
        } catch (NumberFormatException e) {
            // If not a number, treat as text answer and find its index
            System.out.println("🔍 Student answer is text, searching for matching option: '" + studentAnswer + "'");

            for (int i = 0; i < options.size(); i++) {
                if (options.get(i) != null && options.get(i).trim().equals(studentAnswer.trim())) {
                    studentAnswerIndex = i;
                    System.out.println("🔍 Found matching option at index: " + i);
                    break;
                }
            }

            if (studentAnswerIndex == -1) {
                System.err.println("❌ Student answer '" + studentAnswer + "' not found in options for question " + question.getId());
                return 0;
            }
        }

        // Check if the answer is correct
        if (studentAnswerIndex == correctIndex) {
            System.out.println("✅ Correct multiple choice answer for question " + question.getId() +
                    " (student: " + studentAnswerIndex + ", correct: " + correctIndex + ")");
            return question.getPoints() != null ? question.getPoints() : 0;
        } else {
            System.out.println("❌ Incorrect multiple choice answer for question " + question.getId() +
                    " (student: " + studentAnswerIndex + ", correct: " + correctIndex + ")");
            return 0;
        }
    }

    private int gradeTrueFalse(ExamQuestion question, String studentAnswer) {
        String correctAnswer = question.getCorrectAnswer();
        if (correctAnswer == null) {
            System.err.println("❌ No correct answer set for true/false question " + question.getId());
            return 0;
        }

        // Normalize answers for comparison
        String normalizedStudentAnswer = studentAnswer.trim().toLowerCase();
        String normalizedCorrectAnswer = correctAnswer.trim().toLowerCase();

        // Handle various true/false formats
        boolean studentBool = parseBoolean(normalizedStudentAnswer);
        boolean correctBool = parseBoolean(normalizedCorrectAnswer);

        boolean correct = (studentBool == correctBool);

        if (correct) {
            System.out.println("✅ Correct true/false answer for question " + question.getId() +
                    " (student: '" + studentAnswer + "', correct: '" + correctAnswer + "')");
            return question.getPoints() != null ? question.getPoints() : 0;
        } else {
            System.out.println("❌ Incorrect true/false answer for question " + question.getId() +
                    " (student: '" + studentAnswer + "', correct: '" + correctAnswer + "')");
            return 0;
        }
    }

    private boolean parseBoolean(String value) {
        if (value == null) return false;
        value = value.trim().toLowerCase();

        // Handle various true representations
        if (value.equals("true") || value.equals("1") || value.equals("yes") ||
                value.equals("t") || value.equals("y")) {
            return true;
        }

        // Handle various false representations
        if (value.equals("false") || value.equals("0") || value.equals("no") ||
                value.equals("f") || value.equals("n")) {
            return false;
        }

        // Default to false for unrecognized values
        return false;
    }

    // FIXED: Enhanced original gradeTextQuestion method
    private int gradeTextQuestion(ExamQuestion question, String studentAnswer) {
        System.out.println("🔍 === GRADING TEXT QUESTION (ORIGINAL METHOD) ===");
        System.out.println("🔍 Question ID: " + question.getId());
        System.out.println("🔍 Student Answer: '" + studentAnswer + "'");

        List<String> acceptableAnswers = question.getAcceptableAnswers();
        System.out.println("🔍 Acceptable Answers: " + acceptableAnswers);

        if (acceptableAnswers == null || acceptableAnswers.isEmpty()) {
            System.out.println("⚠️ No acceptable answers set for text question " + question.getId() + " - skipping auto-grade");
            return 0;
        }

        Boolean caseSensitive = question.getCaseSensitive();
        boolean isCaseSensitive = caseSensitive != null && caseSensitive;
        System.out.println("🔍 Case Sensitive: " + isCaseSensitive);

        // Handle null/empty student answer
        if (studentAnswer == null || studentAnswer.trim().isEmpty()) {
            System.out.println("❌ Student answer is null or empty");
            return 0;
        }

        // Make this variable final by not reassigning it
        final String answerToCheck = isCaseSensitive ? studentAnswer.trim() : studentAnswer.trim().toLowerCase();
        System.out.println("🔍 Answer to check: '" + answerToCheck + "'");

        // Check if answer matches any acceptable answer without using lambda
        boolean correct = false;
        for (String acceptable : acceptableAnswers) {
            if (acceptable == null || acceptable.trim().isEmpty()) {
                System.out.println("🔍 Skipping null/empty acceptable answer");
                continue;
            }

            String acceptableAnswer = isCaseSensitive ? acceptable.trim() : acceptable.trim().toLowerCase();
            System.out.println("🔍 Checking against: '" + acceptableAnswer + "'");

            if (acceptableAnswer.equals(answerToCheck)) {
                System.out.println("✅ MATCH FOUND!");
                correct = true;
                break;
            }
        }

        if (correct) {
            int points = question.getPoints() != null ? question.getPoints() : 0;
            System.out.println("✅ Correct text answer for question " + question.getId() +
                    " (student: '" + studentAnswer + "') - awarding " + points + " points");
            return points;
        } else {
            System.out.println("❌ Incorrect text answer for question " + question.getId() +
                    " (student: '" + studentAnswer + "', acceptable: " + acceptableAnswers + ")");
            return 0;
        }
    }

    public boolean canStudentViewResponse(String responseId, String username) {
        try {
            ExamResponse response = getResponse(responseId);
            // Students can only view their own responses
            return response.getStudentId().equals(username);
        } catch (Exception e) {
            return false;
        }
    }
}