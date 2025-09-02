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
            List<Exam> exams = examRepository.findByCourseIdOrderByCreatedAtDesc(courseId);
            return exams;
        } catch (Exception e) {
            System.err.println("Error fetching exams: " + e.getMessage());
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

            // Create corresponding grade column
            createGradeColumnForExam(savedExam, instructorId);

            return savedExam;

        } catch (Exception e) {
            System.err.println("Error creating exam: " + e.getMessage());
            throw new RuntimeException("Failed to create exam: " + e.getMessage(), e);
        }
    }

    // FIXED: Helper method to create grade column for exam with default percentage
    private void createGradeColumnForExam(Exam exam, String instructorId) {
        try {

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

        } catch (Exception e) {
            System.err.println("⚠️ Failed to create grade column for exam: " + e.getMessage());
            e.printStackTrace();
        }
    }

    //  Calculate suggested percentage based on task type
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
            return updatedExam;

        } catch (Exception e) {
            System.err.println("Error updating exam: " + e.getMessage());
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
                }

                // Update max points if changed
                Integer newMaxPoints = exam.getTotalPoints();
                if (newMaxPoints != null && !newMaxPoints.equals(gradeColumn.getMaxPoints())) {
                    gradeColumn.setMaxPoints(newMaxPoints);
                    columnUpdated = true;
                }

                if (columnUpdated) {
                    gradeColumnRepository.save(gradeColumn);
                }
            }
        } catch (Exception e) {
            System.err.println("⚠️ Failed to update grade column for exam: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void deleteExam(String examId, String instructorId) {

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

        } catch (Exception e) {
            System.err.println("Error deleting exam: " + e.getMessage());
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
            }
        } catch (Exception e) {
            System.err.println("⚠️ Failed to delete grade column for exam: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public Exam publishExam(String examId, String instructorId) {

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
                }
            }
        } catch (Exception e) {
            System.err.println("⚠️ Failed to update grade column max points: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public Exam unpublishExam(String examId, String instructorId) {

        Exam exam = getExamById(examId);

        if (!exam.getInstructorId().equals(instructorId)) {
            throw new RuntimeException("Not authorized to unpublish this exam");
        }

        exam.setStatus("DRAFT");
        exam.setVisibleToStudents(false);

        Exam unpublishedExam = examRepository.save(exam);
        return unpublishedExam;
    }

    @Override
    public Exam updateExamStatus(String examId, String status, String instructorId) {

        Exam exam = getExamById(examId);

        if (!exam.getInstructorId().equals(instructorId)) {
            throw new RuntimeException("Not authorized to update this exam");
        }

        exam.setStatus(status);

        Exam updatedExam = examRepository.save(exam);
        return updatedExam;
    }

    @Override
    public ExamQuestion addQuestion(String examId, ExamQuestionRequest request, String instructorId) {

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
        return question;
    }

    @Override
    public ExamQuestion updateQuestion(String examId, String questionId, ExamQuestionRequest request, String instructorId) {

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
        return question;
    }

    @Override
    public void deleteQuestion(String examId, String questionId, String instructorId) {

        Exam exam = getExamById(examId);

        if (!exam.getInstructorId().equals(instructorId)) {
            throw new RuntimeException("Not authorized to modify this exam");
        }

        exam.removeQuestion(questionId);
        Exam savedExam = examRepository.save(exam);

        // Update grade column max points after deleting question
        updateGradeColumnMaxPoints(savedExam);
    }

    @Override
    public void reorderQuestions(String examId, List<String> questionIds, String instructorId) {

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
    }

    @Override
    public Exam getStudentExam(String examId, String studentId) {

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
        return savedResponse;
    }

    @Override
    public ExamResponse saveProgress(ExamResponseRequest request, String studentId) {

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
        return savedResponse;
    }

    @Override
    public ExamResponse submitExam(ExamResponseRequest request, String studentId) {

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
        return submittedResponse;
    }

    /**
     * FIXED: Enhanced getExamResponses method with debugging
     * Replace this method in your ExamServiceImpl.java
     */

    @Override
    public List<ExamResponse> getExamResponses(String examId) {
        try {

            // Debug: Check if exam exists first
            Exam exam = getExamById(examId);

            // Try multiple repository methods to see which works
            List<ExamResponse> responses = null;

            // Method 1: Your current approach
            try {
                responses = examResponseRepository.findByExamIdOrderBySubmittedAtDesc(examId);
            } catch (Exception e) {
                System.err.println("Method 1 failed: " + e.getMessage());
                responses = new ArrayList<>();
            }

            // Method 2: Try simple findByExamId if the ordered version fails
            if (responses.isEmpty()) {
                try {
                    responses = examResponseRepository.findByExamId(examId);
                } catch (Exception e) {
                    System.err.println("Method 2 failed: " + e.getMessage());
                    responses = new ArrayList<>();
                }
            }

            // Method 3: Try findAll and filter (last resort for debugging)
            if (responses.isEmpty()) {
                try {
                    List<ExamResponse> allResponses = examResponseRepository.findAll();

                    // Filter manually to see what's happening
                    responses = new ArrayList<>();
                    for (ExamResponse response : allResponses) {

                        if (examId.equals(response.getExamId())) {
                            responses.add(response);
                        }
                    }

                    // If we found responses this way, there might be an issue with the repository method
                    if (!responses.isEmpty()) {
                        System.err.println("⚠️ WARNING: Manual filtering found responses but repository methods didn't!");
                        System.err.println("⚠️ This suggests an issue with your repository method or MongoDB indexes");
                    }

                } catch (Exception e) {
                    System.err.println("Method 3 failed: " + e.getMessage());
                    responses = new ArrayList<>();
                }
            }

            // Log detailed information about found responses
            if (!responses.isEmpty()) {
                for (int i = 0; i < Math.min(responses.size(), 3); i++) {
                    ExamResponse response = responses.get(i);
                }
                if (responses.size() > 3) {
                }
            } else {
            }
            return responses;

        } catch (Exception e) {
            System.err.println("Error fetching exam responses: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * ADDITIONAL DEBUGGING METHOD - Add this to help troubleshoot
     */
    public void debugExamResponses() {
        try {
            List<ExamResponse> allResponses = examResponseRepository.findAll();

            for (ExamResponse response : allResponses) {
            }
        } catch (Exception e) {
            System.err.println("Debug method failed: " + e.getMessage());
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

    //  Get responses for a specific student and exam (response history)
    @Override
    public List<ExamResponse> getStudentExamResponses(String examId, String studentId) {
        return examResponseRepository.findByExamIdAndStudentIdOrderByAttemptNumberDesc(examId, studentId);
    }

    @Override
    public ExamResponse gradeResponse(ExamGradeRequest request, String instructorId) {

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

        ExamResponse response = getResponse(responseId);
        Exam exam = getExamById(response.getExamId());

        // Ensure exam total points are up to date
        exam.recalculateTotalPoints();

        // Update response max score to current exam total
        response.setMaxScore(exam.getTotalPoints());

        Map<String, Integer> autoScores = new HashMap<>();
        boolean hasManualGradingRequired = false;

        // Enhanced grading logic with better error handling
        for (ExamQuestion question : exam.getQuestions()) {

            // FIXED: Use our enhanced method to check if question can be auto-graded
            boolean canAutoGrade = canQuestionBeAutoGraded(question);

            if (canAutoGrade) {
                try {
                    String studentAnswer = response.getAnswers().get(question.getId());

                    int score = gradeQuestionWithEnhancedDebugging(question, studentAnswer);
                    autoScores.put(question.getId(), score);
                } catch (Exception e) {
                    System.err.println("⚠️ Error grading question " + question.getId() + ": " + e.getMessage());
                    e.printStackTrace();
                    // Set score to 0 for failed auto-grading
                    autoScores.put(question.getId(), 0);
                }
            } else {
                // Essay questions or other non-auto-gradable questions
                hasManualGradingRequired = true;
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
        } else {
            response.setStatus("GRADED");
            response.setGraded(true);
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

        if (question.getType() == null) {
            return false;
        }

        String type = question.getType().toLowerCase().trim();

        switch (type) {
            case "multiple-choice":
            case "multiple_choice":
            case "multiplechoice":
                boolean hasOptions = question.getOptions() != null && !question.getOptions().isEmpty();
                boolean hasCorrectIndex = question.getCorrectAnswerIndex() != null;
                return hasOptions && hasCorrectIndex;

            case "true-false":
            case "true_false":
            case "truefalse":
            case "boolean":
                boolean hasCorrectAnswer = question.getCorrectAnswer() != null && !question.getCorrectAnswer().trim().isEmpty();
                return hasCorrectAnswer;

            case "short-answer":
            case "short_answer":
            case "shortanswer":
            case "text":
            case "fill-in-the-blank":
            case "fill_in_the_blank":
                List<String> acceptableAnswers = question.getAcceptableAnswers();
                boolean hasAcceptableAnswers = acceptableAnswers != null && !acceptableAnswers.isEmpty();
                if (hasAcceptableAnswers) {
                    for (int i = 0; i < acceptableAnswers.size(); i++) {
                        String answer = acceptableAnswers.get(i);
                        boolean isValidAnswer = answer != null && !answer.trim().isEmpty();
                    }
                    // Check if at least one acceptable answer is valid
                    for (String answer : acceptableAnswers) {
                        if (answer != null && !answer.trim().isEmpty()) {
                            return true;
                        }
                    }
                    return false;
                }
                return false;

            case "essay":
            case "long-answer":
            case "long_answer":
            case "paragraph":
                return false;

            default:
                return false;
        }
    }

    // FIXED: Enhanced grading method with better debugging
    private int gradeQuestionWithEnhancedDebugging(ExamQuestion question, String studentAnswer) {

        if (studentAnswer == null || studentAnswer.trim().isEmpty()) {
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
                    return 0;
            }

        } catch (Exception e) {
            System.err.println("Error grading question " + question.getId() + ": " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }

    // FIXED: Enhanced text question grading with detailed debugging
    private int gradeTextQuestionWithEnhancedDebugging(ExamQuestion question, String studentAnswer) {

        List<String> acceptableAnswers = question.getAcceptableAnswers();

        if (acceptableAnswers != null && !acceptableAnswers.isEmpty()) {
            for (int i = 0; i < acceptableAnswers.size(); i++) {
            }
        }

        Boolean caseSensitive = question.getCaseSensitive();
        boolean isCaseSensitive = caseSensitive != null && caseSensitive;

        Integer questionPoints = question.getPoints();

        if (acceptableAnswers == null || acceptableAnswers.isEmpty()) {
            return 0;
        }

        // Trim student answer and handle null case
        if (studentAnswer == null) {
            return 0;
        }

        String trimmedStudentAnswer = studentAnswer.trim();
        if (trimmedStudentAnswer.isEmpty()) {
            return 0;
        }

        final String answerToCheck = isCaseSensitive ? trimmedStudentAnswer : trimmedStudentAnswer.toLowerCase();

        // Check if answer matches any acceptable answer
        boolean correct = false;
        for (int i = 0; i < acceptableAnswers.size(); i++) {
            String acceptable = acceptableAnswers.get(i);

            if (acceptable == null) {
                continue;
            }

            String trimmedAcceptable = acceptable.trim();
            if (trimmedAcceptable.isEmpty()) {
                continue;
            }

            String acceptableAnswer = isCaseSensitive ? trimmedAcceptable : trimmedAcceptable.toLowerCase();

            if (acceptableAnswer.equals(answerToCheck)) {
                correct = true;
                break;
            } else {

                // Character-by-character comparison for debugging
                if (acceptableAnswer.length() == answerToCheck.length()) {
                    for (int j = 0; j < acceptableAnswer.length(); j++) {
                        char expected = acceptableAnswer.charAt(j);
                        char actual = answerToCheck.charAt(j);
                        if (expected != actual) {
                        }
                    }
                }
            }
        }

        int pointsToAward = 0;
        if (correct) {
            pointsToAward = questionPoints != null ? questionPoints : 0;
        } else {
        }
        return pointsToAward;
    }

    //  Update individual question score
    @Override
    public ExamResponse updateQuestionScore(String responseId, String questionId, Integer score, String feedback, String instructorId) {

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
        return updatedResponse;
    }

    //  Flag response for review
    @Override
    public ExamResponse flagResponseForReview(String responseId, String reason, String priority, String instructorId) {

        ExamResponse response = getResponse(responseId);

        response.setFlaggedForReview(true);
        if (response.getInstructorFeedback() == null) {
            response.setInstructorFeedback("");
        }
        response.setInstructorFeedback(response.getInstructorFeedback() + "\n[FLAGGED: " + reason + "]");

        ExamResponse flaggedResponse = examResponseRepository.save(response);
        return flaggedResponse;
    }

    //  Unflag response
    @Override
    public ExamResponse unflagResponse(String responseId, String instructorId) {

        ExamResponse response = getResponse(responseId);

        response.setFlaggedForReview(false);
        // Remove flag markers from feedback
        if (response.getInstructorFeedback() != null) {
            response.setInstructorFeedback(
                    response.getInstructorFeedback().replaceAll("\\n\\[FLAGGED:.*?\\]", "")
            );
        }

        ExamResponse unflaggedResponse = examResponseRepository.save(response);
        return unflaggedResponse;
    }

    //  Batch grade multiple responses
    @Override
    public List<ExamResponse> batchGradeResponses(List<String> responseIds, String instructorFeedback, Boolean flagForReview, String instructorId) {

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
                System.err.println("Error batch grading response " + responseId + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
        return batchGradedResponses;
    }

    //  Get grading statistics for an exam
    @Override
    public Map<String, Object> getExamGradingStats(String examId) {

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
        return stats;
    }

    // Sync exam grade to grade column
    private void syncExamGradeToGradeColumn(ExamResponse examResponse, Exam exam) {

        try {
            // Find linked grade column for this exam
            List<GradeColumn> linkedColumns = gradeColumnRepository.findAllByCourseIdAndLinkedAssignmentId(
                    exam.getCourseId(), exam.getId());

            if (linkedColumns.isEmpty()) {
                return;
            }

            GradeColumn gradeColumn = linkedColumns.get(0);

            // Update the grade column's max points to match current exam total
            if (!exam.getTotalPoints().equals(gradeColumn.getMaxPoints())) {
                gradeColumn.setMaxPoints(exam.getTotalPoints());
                gradeColumnRepository.save(gradeColumn);
            }

            // Calculate the grade as a percentage (0-100)
            Double gradePercentage = examResponse.getPercentage();

            // Use GradeService to update the student's grade for this column
            gradeService.updateStudentGrade(examResponse.getStudentId(), gradeColumn.getId(), gradePercentage);

        } catch (Exception e) {
            System.err.println("Error syncing exam grade to grade column: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to sync exam grade to grade column: " + e.getMessage());
        }
    }

    @Override
    public List<ExamResponse> autoGradeAllResponses(String examId) {

        // Verify exam exists and get updated total points
        Exam exam = getExamById(examId);
        exam.recalculateTotalPoints();
        examRepository.save(exam);

        List<ExamResponse> ungraded = examResponseRepository.findUngraded(examId);
        List<ExamResponse> graded = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;

        for (ExamResponse response : ungraded) {
            try {
                // Skip if response is already being manually graded
                if ("GRADING_IN_PROGRESS".equals(response.getStatus())) {
                    continue;
                }

                ExamResponse gradedResponse = autoGradeResponse(response.getId());
                graded.add(gradedResponse);
                successCount++;

            } catch (Exception e) {
                System.err.println("Failed to auto-grade response " + response.getId() + ": " + e.getMessage());
                e.printStackTrace();
                failCount++;

                // Mark as failed auto-grading for manual review
                try {
                    response.setStatus("AUTO_GRADE_FAILED");
                    response.setInstructorFeedback("Auto-grading failed: " + e.getMessage());
                    examResponseRepository.save(response);
                } catch (Exception saveError) {
                    System.err.println("Failed to save error status: " + saveError.getMessage());
                }
            }
        }

        return graded;
    }

    @Override
    public ExamStatsResponse getExamStats(String examId) {

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
            return 0;

        } catch (Exception e) {
            System.err.println("Error grading question " + question.getId() + ": " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }

    private int gradeMultipleChoice(ExamQuestion question, String studentAnswer) {
        Integer correctIndex = question.getCorrectAnswerIndex();
        if (correctIndex == null) {
            System.err.println("No correct answer index set for multiple choice question " + question.getId());
            return 0;
        }

        List<String> options = question.getOptions();
        if (options == null || options.isEmpty()) {
            System.err.println("No options available for multiple choice question " + question.getId());
            return 0;
        }

        int studentAnswerIndex = -1;

        // Try to parse as index first (for backward compatibility)
        try {
            studentAnswerIndex = Integer.parseInt(studentAnswer.trim());
        } catch (NumberFormatException e) {
            // If not a number, treat as text answer and find its index

            for (int i = 0; i < options.size(); i++) {
                if (options.get(i) != null && options.get(i).trim().equals(studentAnswer.trim())) {
                    studentAnswerIndex = i;
                    break;
                }
            }

            if (studentAnswerIndex == -1) {
                System.err.println("Student answer '" + studentAnswer + "' not found in options for question " + question.getId());
                return 0;
            }
        }

        // Check if the answer is correct
        if (studentAnswerIndex == correctIndex) {
            return question.getPoints() != null ? question.getPoints() : 0;
        } else {
            return 0;
        }
    }

    private int gradeTrueFalse(ExamQuestion question, String studentAnswer) {
        String correctAnswer = question.getCorrectAnswer();
        if (correctAnswer == null) {
            System.err.println("No correct answer set for true/false question " + question.getId());
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
            return question.getPoints() != null ? question.getPoints() : 0;
        } else {
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

        List<String> acceptableAnswers = question.getAcceptableAnswers();

        if (acceptableAnswers == null || acceptableAnswers.isEmpty()) {
            return 0;
        }

        Boolean caseSensitive = question.getCaseSensitive();
        boolean isCaseSensitive = caseSensitive != null && caseSensitive;

        // Handle null/empty student answer
        if (studentAnswer == null || studentAnswer.trim().isEmpty()) {
            return 0;
        }

        // Make this variable final by not reassigning it
        final String answerToCheck = isCaseSensitive ? studentAnswer.trim() : studentAnswer.trim().toLowerCase();

        // Check if answer matches any acceptable answer without using lambda
        boolean correct = false;
        for (String acceptable : acceptableAnswers) {
            if (acceptable == null || acceptable.trim().isEmpty()) {
                continue;
            }

            String acceptableAnswer = isCaseSensitive ? acceptable.trim() : acceptable.trim().toLowerCase();

            if (acceptableAnswer.equals(answerToCheck)) {
                correct = true;
                break;
            }
        }

        if (correct) {
            int points = question.getPoints() != null ? question.getPoints() : 0;
            return points;
        } else {
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