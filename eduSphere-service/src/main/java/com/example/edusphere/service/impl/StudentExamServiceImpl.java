package com.example.edusphere.service.impl;

import com.example.edusphere.entity.Exam;
import com.example.edusphere.entity.ExamQuestion;
import com.example.edusphere.entity.ExamResponse;
import com.example.edusphere.repository.ExamRepository;
import com.example.edusphere.repository.ExamResponseRepository;
import com.example.edusphere.service.StudentExamService;
import com.example.edusphere.service.ExamService;
import com.example.edusphere.dto.request.ExamResponseRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class StudentExamServiceImpl implements StudentExamService {

    private final ExamRepository examRepository;
    private final ExamResponseRepository examResponseRepository;
    private final ExamService examService; // For reusing auto-grading logic

    public StudentExamServiceImpl(ExamRepository examRepository,
                                  ExamResponseRepository examResponseRepository,
                                  ExamService examService) {
        this.examRepository = examRepository;
        this.examResponseRepository = examResponseRepository;
        this.examService = examService;
    }

    // ===================================
    // EXAM LISTING AND VIEWING
    // ===================================

    @Override
    public List<Map<String, Object>> getAvailableExamsForStudent(String studentId, String courseId) {
        System.out.println("📚 === FETCHING AVAILABLE EXAMS FOR STUDENT ===");
        System.out.println("Student ID: " + studentId);
        System.out.println("Course ID: " + courseId);

        try {
            List<Exam> allExams = examRepository.findByCourseIdOrderByStartTimeAsc(courseId);
            System.out.println("Found " + allExams.size() + " total exams in course");

            List<Map<String, Object>> availableExams = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now();

            for (Exam exam : allExams) {
                // Only include published and visible exams
                if (!"PUBLISHED".equals(exam.getStatus()) || !exam.getVisibleToStudents()) {
                    System.out.println("⭐ Skipping exam " + exam.getId() + " - not published or visible");
                    continue;
                }

                Map<String, Object> examInfo = createStudentExamInfo(exam, studentId, now);
                availableExams.add(examInfo);
            }

            System.out.println("✅ Returning " + availableExams.size() + " available exams");
            return availableExams;

        } catch (Exception e) {
            System.err.println("❌ Error fetching available exams: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to fetch available exams: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getStudentExamDetails(String examId, String studentId) {
        System.out.println("📄 === FETCHING STUDENT EXAM DETAILS ===");
        System.out.println("Exam ID: " + examId);
        System.out.println("Student ID: " + studentId);

        try {
            Exam exam = examRepository.findById(examId)
                    .orElseThrow(() -> new RuntimeException("Exam not found with ID: " + examId));

            // Check if student can access this exam
            if (!"PUBLISHED".equals(exam.getStatus()) || !exam.getVisibleToStudents()) {
                throw new RuntimeException("Exam is not available for students");
            }

            LocalDateTime now = LocalDateTime.now();
            Map<String, Object> examDetails = createStudentExamInfo(exam, studentId, now);

            // Add eligibility information
            Map<String, Object> eligibility = checkExamEligibility(examId, studentId);
            examDetails.put("eligibility", eligibility);

            // Add attempt history summary
            List<ExamResponse> attempts = examResponseRepository.findByExamIdAndStudentIdOrderByAttemptNumberDesc(examId, studentId);
            examDetails.put("attemptCount", attempts.size());

            if (!attempts.isEmpty()) {
                ExamResponse lastAttempt = attempts.get(0);
                examDetails.put("lastAttempt", createAttemptSummary(lastAttempt));
            }

            // Add questions for exam preview (without answers)
            List<Map<String, Object>> questionPreviews = new ArrayList<>();
            if (exam.getQuestions() != null) {
                for (int i = 0; i < exam.getQuestions().size(); i++) {
                    ExamQuestion question = exam.getQuestions().get(i);
                    Map<String, Object> preview = new HashMap<>();
                    preview.put("number", i + 1);
                    preview.put("type", question.getType());
                    preview.put("points", question.getPoints());
                    preview.put("required", question.getRequired());
                    // Don't include actual question text or answers for preview
                    questionPreviews.add(preview);
                }
            }
            examDetails.put("questionPreviews", questionPreviews);

            System.out.println("✅ Retrieved exam details successfully");
            return examDetails;

        } catch (RuntimeException e) {
            System.err.println("❌ Runtime error: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            System.err.println("❌ Unexpected error: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to fetch exam details: " + e.getMessage());
        }
    }

    // ===================================
    // EXAM ATTEMPT MANAGEMENT
    // ===================================

    @Override
    public Map<String, Object> startExamAttempt(String examId, String studentId) {
        System.out.println("🎯 === STARTING EXAM ATTEMPT ===");
        System.out.println("Exam ID: " + examId);
        System.out.println("Student ID: " + studentId);

        try {
            // Check eligibility first
            Map<String, Object> eligibility = checkExamEligibility(examId, studentId);
            Boolean canTake = (Boolean) eligibility.get("canTake");

            if (!canTake) {
                String reason = (String) eligibility.get("reason");
                throw new RuntimeException("Cannot start exam: " + reason);
            }

            // Check for existing active attempt
            Optional<ExamResponse> activeAttempt = examResponseRepository.findActiveResponse(examId, studentId);
            if (activeAttempt.isPresent()) {
                throw new RuntimeException("You already have an active attempt for this exam. Please resume or complete it first.");
            }

            Exam exam = examRepository.findById(examId)
                    .orElseThrow(() -> new RuntimeException("Exam not found with ID: " + examId));

            // Calculate attempt number
            int attemptNumber = (int) examResponseRepository.countByExamIdAndStudentId(examId, studentId) + 1;

            // Create new exam response
            ExamResponse response = new ExamResponse();
            response.setExamId(examId);
            response.setStudentId(studentId);
            response.setCourseId(exam.getCourseId());
            response.setStartedAt(LocalDateTime.now());
            response.setStatus("IN_PROGRESS");
            response.setMaxScore(exam.getTotalPoints());
            response.setAttemptNumber(attemptNumber);
            response.setAnswers(new HashMap<>());
            response.setQuestionScores(new HashMap<>());

            ExamResponse savedResponse = examResponseRepository.save(response);

            // Prepare exam data for student (remove sensitive information)
            Map<String, Object> examData = sanitizeExamForStudent(exam);

            // Create response data
            Map<String, Object> attemptData = new HashMap<>();
            attemptData.put("responseId", savedResponse.getId());
            attemptData.put("examId", examId);
            attemptData.put("attemptNumber", attemptNumber);
            attemptData.put("maxAttempts", exam.getMaxAttempts());
            attemptData.put("startedAt", savedResponse.getStartedAt());
            attemptData.put("status", "IN_PROGRESS");
            attemptData.put("exam", examData);

            System.out.println("✅ Started exam attempt " + attemptNumber + " for student " + studentId);
            return attemptData;

        } catch (RuntimeException e) {
            System.err.println("❌ Runtime error: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            System.err.println("❌ Unexpected error: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to start exam attempt: " + e.getMessage());
        }
    }

    @Override
    public ExamResponse saveExamProgress(ExamResponseRequest request, String studentId) {
        System.out.println("💾 === SAVING EXAM PROGRESS ===");
        System.out.println("Exam ID: " + request.getExamId());
        System.out.println("Student ID: " + studentId);

        try {
            ExamResponse response = examResponseRepository.findActiveResponse(request.getExamId(), studentId)
                    .orElseThrow(() -> new RuntimeException("No active exam attempt found"));

            // Verify the response belongs to this student
            if (!studentId.equals(response.getStudentId())) {
                throw new RuntimeException("Unauthorized: This exam attempt does not belong to you");
            }

            // FIXED: Process answers correctly for shuffled options
            if (request.getAnswers() != null && !request.getAnswers().isEmpty()) {
                Map<String, String> currentAnswers = response.getAnswers();
                if (currentAnswers == null) {
                    currentAnswers = new HashMap<>();
                }

                // CRITICAL: Store answers as received from frontend (already converted to original indices)
                // Frontend hook now properly converts shuffled indices back to original indices
                System.out.println("📝 Saving answers (already converted from shuffled): " + request.getAnswers());
                currentAnswers.putAll(request.getAnswers());
                response.setAnswers(currentAnswers);

                System.out.println("🔍 Updated " + request.getAnswers().size() + " answers");
            }

            // Update time spent
            if (request.getTimeSpent() != null) {
                response.setTimeSpent(request.getTimeSpent());
                System.out.println("⏱️ Updated time spent: " + request.getTimeSpent() + " seconds");
            }

            response.setUpdatedAt(LocalDateTime.now());
            ExamResponse savedResponse = examResponseRepository.save(response);

            System.out.println("✅ Saved exam progress successfully");
            return savedResponse;

        } catch (RuntimeException e) {
            System.err.println("❌ Runtime error: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            System.err.println("❌ Unexpected error: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to save exam progress: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> submitExam(ExamResponseRequest request, String studentId) {
        System.out.println("📤 === SUBMITTING EXAM ===");
        System.out.println("Exam ID: " + request.getExamId());
        System.out.println("Student ID: " + studentId);

        try {
            ExamResponse response = examResponseRepository.findActiveResponse(request.getExamId(), studentId)
                    .orElseThrow(() -> new RuntimeException("No active exam attempt found"));

            // Verify the response belongs to this student
            if (!studentId.equals(response.getStudentId())) {
                throw new RuntimeException("Unauthorized: This exam attempt does not belong to you");
            }

            Exam exam = examRepository.findById(request.getExamId())
                    .orElseThrow(() -> new RuntimeException("Exam not found"));

            // FIXED: Update final answers (already converted from shuffled indices)
            if (request.getAnswers() != null && !request.getAnswers().isEmpty()) {
                Map<String, String> currentAnswers = response.getAnswers();
                if (currentAnswers == null) {
                    currentAnswers = new HashMap<>();
                }

                // Frontend already converted shuffled indices to original indices
                System.out.println("📤 Final answers (converted from shuffled): " + request.getAnswers());
                currentAnswers.putAll(request.getAnswers());
                response.setAnswers(currentAnswers);
            }

            // Update final time spent
            if (request.getTimeSpent() != null) {
                response.setTimeSpent(request.getTimeSpent());
            }

            // Set submission details
            response.setStatus("SUBMITTED");
            response.setSubmittedAt(LocalDateTime.now());

            // Check if late submission
            if (LocalDateTime.now().isAfter(exam.getEndTime())) {
                response.setLateSubmission(true);
                System.out.println("⚠️ Late submission detected");
            }

            ExamResponse submittedResponse = examResponseRepository.save(response);

            // Attempt auto-grading
            boolean autoGraded = false;
            Map<String, Object> results = null;

            try {
                System.out.println("🤖 Attempting auto-grading...");
                ExamResponse gradedResponse = examService.autoGradeResponse(submittedResponse.getId());
                autoGraded = gradedResponse.getAutoGraded() != null && gradedResponse.getAutoGraded();

                if (autoGraded && exam.getShowResults()) {
                    System.out.println("📊 Auto-grading successful, preparing results...");
                    results = getStudentExamResults(gradedResponse.getId(), studentId);
                }

            } catch (Exception e) {
                System.err.println("⚠️ Auto-grading failed: " + e.getMessage());
                e.printStackTrace();
                // Continue without auto-grading
            }

            // Prepare response data
            Map<String, Object> submitResult = new HashMap<>();
            submitResult.put("responseId", submittedResponse.getId());
            submitResult.put("examId", request.getExamId());
            submitResult.put("submittedAt", submittedResponse.getSubmittedAt());
            submitResult.put("status", "SUBMITTED");
            submitResult.put("autoGraded", autoGraded);
            submitResult.put("showResults", exam.getShowResults());
            submitResult.put("lateSubmission", submittedResponse.getLateSubmission());

            if (results != null && exam.getShowResults()) {
                submitResult.put("results", results);
                submitResult.put("graded", true);
                System.out.println("📊 Including immediate results in response");
            } else {
                submitResult.put("graded", false);
                submitResult.put("message", "Exam submitted successfully. Results will be available once graded.");
            }

            System.out.println("✅ Submitted exam successfully");
            return submitResult;

        } catch (RuntimeException e) {
            System.err.println("❌ Runtime error: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            System.err.println("❌ Unexpected error: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to submit exam: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> resumeExamAttempt(String examId, String studentId) {
        System.out.println("🔄 === RESUMING EXAM ATTEMPT ===");
        System.out.println("Exam ID: " + examId);
        System.out.println("Student ID: " + studentId);

        try {
            ExamResponse activeResponse = examResponseRepository.findActiveResponse(examId, studentId)
                    .orElseThrow(() -> new RuntimeException("No active exam attempt found to resume"));

            // Verify the response belongs to this student
            if (!studentId.equals(activeResponse.getStudentId())) {
                throw new RuntimeException("Unauthorized: This exam attempt does not belong to you");
            }

            Exam exam = examRepository.findById(examId)
                    .orElseThrow(() -> new RuntimeException("Exam not found"));

            // Check if exam is still available
            LocalDateTime now = LocalDateTime.now();
            if (now.isAfter(exam.getEndTime())) {
                throw new RuntimeException("Exam time has expired. Cannot resume attempt.");
            }

            // Calculate elapsed time
            long elapsedMinutes = java.time.Duration.between(activeResponse.getStartedAt(), now).toMinutes();
            long remainingMinutes = Math.max(0, exam.getDuration() - elapsedMinutes);

            if (remainingMinutes <= 0) {
                throw new RuntimeException("Time limit exceeded. Cannot resume attempt.");
            }

            // Prepare exam data for student
            Map<String, Object> examData = sanitizeExamForStudent(exam);

            // FIXED: Convert stored answers back to shuffled format for frontend display
            Map<String, String> currentAnswers = activeResponse.getAnswers();
            Map<String, String> displayAnswers = new HashMap<>();

            if (currentAnswers != null && exam.getShuffleOptions()) {
                System.out.println("🔄 Converting stored answers back to shuffled format for resume");
                // Note: This is complex because we need the original shuffle mapping
                // For now, we'll pass the stored answers as-is and let frontend handle
                displayAnswers = currentAnswers;
            } else {
                displayAnswers = currentAnswers != null ? currentAnswers : new HashMap<>();
            }

            // Create resume data
            Map<String, Object> resumeData = new HashMap<>();
            resumeData.put("responseId", activeResponse.getId());
            resumeData.put("examId", examId);
            resumeData.put("attemptNumber", activeResponse.getAttemptNumber());
            resumeData.put("startedAt", activeResponse.getStartedAt());
            resumeData.put("currentAnswers", displayAnswers);
            resumeData.put("timeSpent", activeResponse.getTimeSpent());
            resumeData.put("remainingMinutes", remainingMinutes);
            resumeData.put("exam", examData);
            resumeData.put("status", "RESUMED");

            System.out.println("✅ Resumed exam attempt successfully");
            System.out.println("⏱️ Remaining time: " + remainingMinutes + " minutes");

            return resumeData;

        } catch (RuntimeException e) {
            System.err.println("❌ Runtime error: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            System.err.println("❌ Unexpected error: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to resume exam attempt: " + e.getMessage());
        }
    }

    // ===================================
    // EXAM ELIGIBILITY AND STATUS
    // ===================================

    @Override
    public Map<String, Object> checkExamEligibility(String examId, String studentId) {
        System.out.println("🔍 === CHECKING EXAM ELIGIBILITY ===");
        System.out.println("Exam ID: " + examId);
        System.out.println("Student ID: " + studentId);

        try {
            Exam exam = examRepository.findById(examId)
                    .orElseThrow(() -> new RuntimeException("Exam not found with ID: " + examId));

            Map<String, Object> eligibility = new HashMap<>();
            LocalDateTime now = LocalDateTime.now();
            boolean canTake = true;
            String reason = "";

            // Check if exam is published and visible
            if (!"PUBLISHED".equals(exam.getStatus())) {
                canTake = false;
                reason = "Exam is not published";
            } else if (!exam.getVisibleToStudents()) {
                canTake = false;
                reason = "Exam is not visible to students";
            }
            // Check time window
            else if (now.isBefore(exam.getStartTime())) {
                canTake = false;
                reason = "Exam has not started yet";
                eligibility.put("startsAt", exam.getStartTime());
            } else if (now.isAfter(exam.getEndTime())) {
                canTake = false;
                reason = "Exam time has expired";
            }
            // Check attempt limit
            else {
                int attemptCount = (int) examResponseRepository.countByExamIdAndStudentId(examId, studentId);
                if (attemptCount >= exam.getMaxAttempts()) {
                    canTake = false;
                    reason = "Maximum attempts (" + exam.getMaxAttempts() + ") reached";
                }
                eligibility.put("attemptCount", attemptCount);
                eligibility.put("maxAttempts", exam.getMaxAttempts());
            }

            // Check for active attempt
            Optional<ExamResponse> activeAttempt = examResponseRepository.findActiveResponse(examId, studentId);
            boolean hasActiveAttempt = activeAttempt.isPresent();

            if (hasActiveAttempt && canTake) {
                reason = "You have an active attempt. Please resume or complete it.";
                eligibility.put("activeAttemptId", activeAttempt.get().getId());
                eligibility.put("canResume", true);
            }

            eligibility.put("canTake", canTake && !hasActiveAttempt);
            eligibility.put("hasActiveAttempt", hasActiveAttempt);
            eligibility.put("reason", reason);
            eligibility.put("examStatus", exam.getStatus());
            eligibility.put("isVisible", exam.getVisibleToStudents());
            eligibility.put("startTime", exam.getStartTime());
            eligibility.put("endTime", exam.getEndTime());
            eligibility.put("isWithinTimeWindow", !now.isBefore(exam.getStartTime()) && !now.isAfter(exam.getEndTime()));

            System.out.println("✅ Eligibility check completed - Can take: " + canTake + ", Has active: " + hasActiveAttempt);
            return eligibility;

        } catch (RuntimeException e) {
            System.err.println("❌ Runtime error: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            System.err.println("❌ Unexpected error: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to check exam eligibility: " + e.getMessage());
        }
    }

    @Override
    public List<Map<String, Object>> getStudentAttemptHistory(String examId, String studentId) {
        System.out.println("📚 === FETCHING STUDENT ATTEMPT HISTORY ===");
        System.out.println("Exam ID: " + examId);
        System.out.println("Student ID: " + studentId);

        try {
            List<ExamResponse> attempts = examResponseRepository.findByExamIdAndStudentIdOrderByAttemptNumberDesc(examId, studentId);

            List<Map<String, Object>> attemptHistory = new ArrayList<>();
            for (ExamResponse attempt : attempts) {
                attemptHistory.add(createAttemptSummary(attempt));
            }

            System.out.println("✅ Retrieved " + attemptHistory.size() + " attempt records");
            return attemptHistory;

        } catch (Exception e) {
            System.err.println("❌ Error fetching attempt history: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to fetch attempt history: " + e.getMessage());
        }
    }

    // ===================================
    // EXAM RESULTS AND FEEDBACK - FIXED FOR SHUFFLED OPTIONS
    // ===================================

    @Override
    public Map<String, Object> getStudentExamResults(String responseId, String studentId) {
        System.out.println("📊 === FETCHING STUDENT EXAM RESULTS ===");
        System.out.println("Response ID: " + responseId);
        System.out.println("Student ID: " + studentId);

        try {
            ExamResponse response = examResponseRepository.findById(responseId)
                    .orElseThrow(() -> new RuntimeException("Exam response not found with ID: " + responseId));

            // Verify the response belongs to this student
            if (!studentId.equals(response.getStudentId())) {
                throw new RuntimeException("Unauthorized: This exam response does not belong to you");
            }

            // Check if results should be shown
            Exam exam = examRepository.findById(response.getExamId())
                    .orElseThrow(() -> new RuntimeException("Exam not found"));

            if (!exam.getShowResults()) {
                throw new RuntimeException("Results are not available for this exam");
            }

            // Only show results if graded
            if (!response.getGraded()) {
                throw new RuntimeException("Exam is not yet graded");
            }

            Map<String, Object> results = new HashMap<>();
            results.put("responseId", responseId);
            results.put("examId", response.getExamId());
            results.put("examTitle", exam.getTitle());
            results.put("attemptNumber", response.getAttemptNumber());
            results.put("maxAttempts", exam.getMaxAttempts());
            results.put("submittedAt", response.getSubmittedAt());
            results.put("timeSpent", response.getTimeSpent());
            results.put("status", response.getStatus());

            // Score information
            results.put("totalScore", response.getTotalScore());
            results.put("maxScore", response.getMaxScore());
            results.put("percentage", response.getPercentage());
            results.put("passed", response.getPassed());
            results.put("passPercentage", exam.getPassPercentage());

            // Grading information
            results.put("graded", response.getGraded());
            results.put("autoGraded", response.getAutoGraded());
            results.put("gradedAt", response.getGradedAt());

            // Instructor feedback (if any)
            if (response.getInstructorFeedback() != null && !response.getInstructorFeedback().trim().isEmpty()) {
                results.put("instructorFeedback", response.getInstructorFeedback());
            }

            // Time formatting
            if (response.getTimeSpent() != null) {
                results.put("timeSpentFormatted", formatTimeSpent(response.getTimeSpent()));
            }

            System.out.println("✅ Retrieved exam results successfully");
            return results;

        } catch (RuntimeException e) {
            System.err.println("❌ Runtime error: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            System.err.println("❌ Unexpected error: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to fetch exam results: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getDetailedExamResults(String responseId, String studentId) {
        System.out.println("📋 === FETCHING DETAILED EXAM RESULTS ===");
        System.out.println("Response ID: " + responseId);
        System.out.println("Student ID: " + studentId);

        try {
            ExamResponse response = examResponseRepository.findById(responseId)
                    .orElseThrow(() -> new RuntimeException("Exam response not found with ID: " + responseId));

            // Verify the response belongs to this student
            if (!studentId.equals(response.getStudentId())) {
                throw new RuntimeException("Unauthorized: This exam response does not belong to you");
            }

            Exam exam = examRepository.findById(response.getExamId())
                    .orElseThrow(() -> new RuntimeException("Exam not found"));

            if (!exam.getShowResults()) {
                throw new RuntimeException("Detailed results are not available for this exam");
            }

            if (!response.getGraded()) {
                throw new RuntimeException("Exam is not yet graded");
            }

            // Get basic results
            Map<String, Object> detailedResults = getStudentExamResults(responseId, studentId);

            // FIXED: Add question-by-question breakdown with proper answer handling
            List<Map<String, Object>> questionResults = new ArrayList<>();
            Map<String, String> answers = response.getAnswers();
            Map<String, Integer> questionScores = response.getQuestionScores();

            if (exam.getQuestions() != null) {
                for (int i = 0; i < exam.getQuestions().size(); i++) {
                    ExamQuestion question = exam.getQuestions().get(i);
                    String questionId = question.getId();

                    Map<String, Object> questionResult = new HashMap<>();
                    questionResult.put("questionNumber", i + 1);
                    questionResult.put("questionType", question.getType());
                    questionResult.put("points", question.getPoints());
                    questionResult.put("earnedPoints", questionScores.getOrDefault(questionId, 0));

                    // FIXED: Student's answer (stored as original index, convert to display text)
                    String studentAnswer = answers.get(questionId);
                    questionResult.put("studentAnswer", studentAnswer);

                    // FIXED: Format answer display based on question type for results
                    if ("multiple-choice".equals(question.getType()) && question.getOptions() != null) {
                        try {
                            // For results display, convert stored original index back to option text
                            int answerIndex = Integer.parseInt(studentAnswer);
                            if (answerIndex >= 0 && answerIndex < question.getOptions().size()) {
                                questionResult.put("studentAnswerText", question.getOptions().get(answerIndex));
                            } else {
                                questionResult.put("studentAnswerText", "Invalid selection");
                            }
                        } catch (NumberFormatException e) {
                            // If not a number, treat as text answer
                            questionResult.put("studentAnswerText", studentAnswer);
                        }
                    } else {
                        questionResult.put("studentAnswerText", studentAnswer);
                    }

                    // FIXED: Show correct answer - always use original question data
                    if (exam.getShowResults()) {
                        if ("multiple-choice".equals(question.getType())) {
                            if (question.getCorrectAnswerIndex() != null && question.getOptions() != null) {
                                int correctIndex = question.getCorrectAnswerIndex();
                                if (correctIndex >= 0 && correctIndex < question.getOptions().size()) {
                                    questionResult.put("correctAnswer", question.getOptions().get(correctIndex));
                                    questionResult.put("correctAnswerIndex", correctIndex);
                                }
                            }
                        } else if ("true-false".equals(question.getType())) {
                            questionResult.put("correctAnswer", question.getCorrectAnswer());
                        }
                        // For text questions, don't show acceptable answers for security
                    }

                    // FIXED: Determine if answer is correct using original indices
                    int earnedPoints = questionScores.getOrDefault(questionId, 0);
                    boolean isCorrect = false;

                    if ("multiple-choice".equals(question.getType())) {
                        try {
                            int studentIndex = Integer.parseInt(studentAnswer);
                            isCorrect = studentIndex == question.getCorrectAnswerIndex() && earnedPoints > 0;
                        } catch (NumberFormatException e) {
                            isCorrect = earnedPoints > 0;
                        }
                    } else if ("true-false".equals(question.getType())) {
                        isCorrect = question.getCorrectAnswer() != null &&
                                question.getCorrectAnswer().equalsIgnoreCase(studentAnswer) &&
                                earnedPoints > 0;
                    } else {
                        // For text questions, use points to determine correctness
                        isCorrect = earnedPoints > 0 && earnedPoints == question.getPoints();
                    }

                    questionResult.put("isCorrect", isCorrect);

                    // Show explanation if available
                    if (question.getExplanation() != null && !question.getExplanation().trim().isEmpty()) {
                        questionResult.put("explanation", question.getExplanation());
                    }

                    questionResults.add(questionResult);
                }
            }

            detailedResults.put("questionResults", questionResults);

            // Summary statistics
            Map<String, Object> summary = new HashMap<>();
            long correctAnswers = questionResults.stream()
                    .mapToLong(q -> Boolean.TRUE.equals(q.get("isCorrect")) ? 1 : 0)
                    .sum();

            summary.put("totalQuestions", questionResults.size());
            summary.put("correctAnswers", correctAnswers);
            summary.put("incorrectAnswers", questionResults.size() - correctAnswers);

            if (!questionResults.isEmpty()) {
                summary.put("accuracyPercentage", Math.round((correctAnswers * 100.0) / questionResults.size()));
            }

            detailedResults.put("summary", summary);

            System.out.println("✅ Retrieved detailed exam results successfully");
            return detailedResults;

        } catch (RuntimeException e) {
            System.err.println("❌ Runtime error: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            System.err.println("❌ Unexpected error: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to fetch detailed exam results: " + e.getMessage());
        }
    }

    // ===================================
    // STUDENT STATISTICS
    // ===================================

    @Override
    public Map<String, Object> getStudentExamStats(String studentId, String courseId) {
        System.out.println("📈 === FETCHING STUDENT EXAM STATISTICS ===");
        System.out.println("Student ID: " + studentId);
        System.out.println("Course ID: " + courseId);

        try {
            List<ExamResponse> responses = examResponseRepository.findByStudentIdAndCourseId(studentId, courseId);

            Map<String, Object> stats = new HashMap<>();

            // Basic counts
            stats.put("totalAttempts", responses.size());

            long completedAttempts = responses.stream()
                    .filter(r -> "SUBMITTED".equals(r.getStatus()) || "GRADED".equals(r.getStatus()))
                    .count();
            stats.put("completedAttempts", completedAttempts);

            long gradedAttempts = responses.stream()
                    .filter(r -> Boolean.TRUE.equals(r.getGraded()))
                    .count();
            stats.put("gradedAttempts", gradedAttempts);

            long passedAttempts = responses.stream()
                    .filter(r -> Boolean.TRUE.equals(r.getPassed()))
                    .count();
            stats.put("passedAttempts", passedAttempts);

            // Average scores
            if (gradedAttempts > 0) {
                double averageScore = responses.stream()
                        .filter(r -> Boolean.TRUE.equals(r.getGraded()))
                        .mapToDouble(r -> r.getPercentage() != null ? r.getPercentage() : 0.0)
                        .average()
                        .orElse(0.0);
                stats.put("averageScore", Math.round(averageScore * 100.0) / 100.0);

                double passRate = (passedAttempts * 100.0) / gradedAttempts;
                stats.put("passRate", Math.round(passRate * 100.0) / 100.0);
            } else {
                stats.put("averageScore", 0.0);
                stats.put("passRate", 0.0);
            }

            // Time statistics
            List<Integer> timeSpentList = responses.stream()
                    .filter(r -> r.getTimeSpent() != null)
                    .map(ExamResponse::getTimeSpent)
                    .collect(Collectors.toList());

            if (!timeSpentList.isEmpty()) {
                double averageTime = timeSpentList.stream()
                        .mapToInt(Integer::intValue)
                        .average()
                        .orElse(0.0);
                stats.put("averageTimeSpent", Math.round(averageTime));
                stats.put("averageTimeSpentFormatted", formatTimeSpent((int) averageTime));
            }

            System.out.println("✅ Retrieved exam statistics successfully");
            return stats;

        } catch (Exception e) {
            System.err.println("❌ Error fetching exam stats: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to fetch exam statistics: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> checkActiveAttempt(String examId, String studentId) {
        System.out.println("🔍 === CHECKING ACTIVE ATTEMPT ===");
        System.out.println("Exam ID: " + examId);
        System.out.println("Student ID: " + studentId);

        try {
            Optional<ExamResponse> activeAttempt = examResponseRepository.findActiveResponse(examId, studentId);

            Map<String, Object> result = new HashMap<>();
            result.put("hasActiveAttempt", activeAttempt.isPresent());

            if (activeAttempt.isPresent()) {
                ExamResponse response = activeAttempt.get();
                result.put("responseId", response.getId());
                result.put("attemptNumber", response.getAttemptNumber());
                result.put("startedAt", response.getStartedAt());
                result.put("timeSpent", response.getTimeSpent());
                result.put("canResume", true);

                // Calculate remaining time
                Exam exam = examRepository.findById(examId).orElse(null);
                if (exam != null) {
                    long elapsedMinutes = java.time.Duration.between(response.getStartedAt(), LocalDateTime.now()).toMinutes();
                    long remainingMinutes = Math.max(0, exam.getDuration() - elapsedMinutes);
                    result.put("remainingMinutes", remainingMinutes);
                    result.put("canResume", remainingMinutes > 0);
                }
            }

            System.out.println("✅ Checked active attempt successfully");
            return result;

        } catch (Exception e) {
            System.err.println("❌ Error checking active attempt: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to check active attempt: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getExamDashboardSummary(String studentId) {
        System.out.println("📊 === FETCHING EXAM DASHBOARD SUMMARY ===");
        System.out.println("Student ID: " + studentId);

        try {
            List<ExamResponse> allResponses = examResponseRepository.findByStudentId(studentId);
            LocalDateTime now = LocalDateTime.now();

            Map<String, Object> summary = new HashMap<>();

            // Count various statuses
            long inProgress = allResponses.stream()
                    .filter(r -> "IN_PROGRESS".equals(r.getStatus()))
                    .count();

            long completed = allResponses.stream()
                    .filter(r -> "SUBMITTED".equals(r.getStatus()) || "GRADED".equals(r.getStatus()))
                    .count();

            long graded = allResponses.stream()
                    .filter(r -> Boolean.TRUE.equals(r.getGraded()))
                    .count();

            long passed = allResponses.stream()
                    .filter(r -> Boolean.TRUE.equals(r.getPassed()))
                    .count();

            summary.put("inProgressExams", inProgress);
            summary.put("completedExams", completed);
            summary.put("gradedExams", graded);
            summary.put("passedExams", passed);

            // Calculate overall statistics
            if (graded > 0) {
                double overallAverage = allResponses.stream()
                        .filter(r -> Boolean.TRUE.equals(r.getGraded()))
                        .mapToDouble(r -> r.getPercentage() != null ? r.getPercentage() : 0.0)
                        .average()
                        .orElse(0.0);
                summary.put("overallAverage", Math.round(overallAverage * 100.0) / 100.0);

                double overallPassRate = (passed * 100.0) / graded;
                summary.put("overallPassRate", Math.round(overallPassRate * 100.0) / 100.0);
            } else {
                summary.put("overallAverage", 0.0);
                summary.put("overallPassRate", 0.0);
            }

            // Find upcoming exams (this would need exam repository access)
            // For now, just indicate if there are active attempts
            summary.put("hasActiveAttempts", inProgress > 0);

            System.out.println("✅ Retrieved dashboard summary successfully");
            return summary;

        } catch (Exception e) {
            System.err.println("❌ Error fetching dashboard summary: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to fetch dashboard summary: " + e.getMessage());
        }
    }

    // ===================================
    // HELPER METHODS
    // ===================================

    private Map<String, Object> createStudentExamInfo(Exam exam, String studentId, LocalDateTime now) {
        Map<String, Object> examInfo = new HashMap<>();

        // Basic exam information
        examInfo.put("id", exam.getId());
        examInfo.put("title", exam.getTitle());
        examInfo.put("description", exam.getDescription());
        examInfo.put("instructions", exam.getInstructions());
        examInfo.put("courseId", exam.getCourseId());
        examInfo.put("duration", exam.getDuration());
        examInfo.put("startTime", exam.getStartTime());
        examInfo.put("endTime", exam.getEndTime());
        examInfo.put("totalPoints", exam.getTotalPoints());
        examInfo.put("passPercentage", exam.getPassPercentage());
        examInfo.put("maxAttempts", exam.getMaxAttempts());
        examInfo.put("questionCount", exam.getQuestions() != null ? exam.getQuestions().size() : 0);

        // Student-specific settings
        examInfo.put("showResults", exam.getShowResults());
        examInfo.put("shuffleQuestions", exam.getShuffleQuestions());
        examInfo.put("shuffleOptions", exam.getShuffleOptions());
        examInfo.put("allowNavigation", exam.getAllowNavigation());
        examInfo.put("showTimer", exam.getShowTimer());
        examInfo.put("autoSubmit", exam.getAutoSubmit());
        examInfo.put("requireSafeBrowser", exam.getRequireSafeBrowser());

        // Calculate exam status
        String status;
        if (now.isBefore(exam.getStartTime())) {
            status = "upcoming";
        } else if (now.isAfter(exam.getEndTime())) {
            status = "expired";
        } else {
            status = "available";
        }
        examInfo.put("status", status);

        // Check student's attempt information
        List<ExamResponse> attempts = examResponseRepository.findByExamIdAndStudentIdOrderByAttemptNumberDesc(exam.getId(), studentId);
        examInfo.put("attemptCount", attempts.size());
        examInfo.put("canTakeExam", attempts.size() < exam.getMaxAttempts() && "available".equals(status));

        // Check for active attempt
        boolean hasActiveAttempt = attempts.stream()
                .anyMatch(a -> "IN_PROGRESS".equals(a.getStatus()));
        examInfo.put("hasActiveAttempt", hasActiveAttempt);

        // Add latest attempt info if exists
        if (!attempts.isEmpty()) {
            ExamResponse latestAttempt = attempts.get(0);
            Map<String, Object> latestInfo = new HashMap<>();
            latestInfo.put("id", latestAttempt.getId()); // FIXED: Include response ID
            latestInfo.put("attemptNumber", latestAttempt.getAttemptNumber());
            latestInfo.put("status", latestAttempt.getStatus());
            latestInfo.put("submittedAt", latestAttempt.getSubmittedAt());
            latestInfo.put("graded", latestAttempt.getGraded());

            if (latestAttempt.getGraded()) {
                latestInfo.put("totalScore", latestAttempt.getTotalScore());
                latestInfo.put("maxScore", latestAttempt.getMaxScore());
                latestInfo.put("percentage", latestAttempt.getPercentage());
                latestInfo.put("passed", latestAttempt.getPassed());
            }

            examInfo.put("latestAttempt", latestInfo);
        }

        return examInfo;
    }

    private Map<String, Object> sanitizeExamForStudent(Exam exam) {
        Map<String, Object> sanitizedExam = new HashMap<>();

        // Basic information
        sanitizedExam.put("id", exam.getId());
        sanitizedExam.put("title", exam.getTitle());
        sanitizedExam.put("description", exam.getDescription());
        sanitizedExam.put("instructions", exam.getInstructions());
        sanitizedExam.put("duration", exam.getDuration());
        sanitizedExam.put("totalPoints", exam.getTotalPoints());
        sanitizedExam.put("passPercentage", exam.getPassPercentage());

        // Student settings
        sanitizedExam.put("showResults", exam.getShowResults());
        sanitizedExam.put("shuffleQuestions", exam.getShuffleQuestions());
        sanitizedExam.put("shuffleOptions", exam.getShuffleOptions());
        sanitizedExam.put("allowNavigation", exam.getAllowNavigation());
        sanitizedExam.put("showTimer", exam.getShowTimer());
        sanitizedExam.put("autoSubmit", exam.getAutoSubmit());
        sanitizedExam.put("requireSafeBrowser", exam.getRequireSafeBrowser());

        // Sanitize questions - remove correct answers and explanations
        if (exam.getQuestions() != null) {
            List<Map<String, Object>> sanitizedQuestions = new ArrayList<>();

            for (ExamQuestion question : exam.getQuestions()) {
                Map<String, Object> sanitizedQuestion = new HashMap<>();
                sanitizedQuestion.put("id", question.getId());
                sanitizedQuestion.put("type", question.getType());
                sanitizedQuestion.put("question", question.getQuestion());
                sanitizedQuestion.put("points", question.getPoints());
                sanitizedQuestion.put("required", question.getRequired());
                sanitizedQuestion.put("timeLimit", question.getTimeLimit());
                sanitizedQuestion.put("displayOrder", question.getDisplayOrder());
                sanitizedQuestion.put("caseSensitive", question.getCaseSensitive());
                sanitizedQuestion.put("maxLength", question.getMaxLength());

                // Include options for multiple choice but not correct answers
                if ("multiple-choice".equals(question.getType()) && question.getOptions() != null) {
                    sanitizedQuestion.put("options", new ArrayList<>(question.getOptions()));
                    // Include correct answer index for frontend shuffling logic
                    sanitizedQuestion.put("correctAnswerIndex", question.getCorrectAnswerIndex());
                }

                // Don't include: correctAnswer, explanation, acceptableAnswers for security
                sanitizedQuestions.add(sanitizedQuestion);
            }

            sanitizedExam.put("questions", sanitizedQuestions);
        }

        return sanitizedExam;
    }

    private Map<String, Object> createAttemptSummary(ExamResponse attempt) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("id", attempt.getId());
        summary.put("attemptNumber", attempt.getAttemptNumber());
        summary.put("status", attempt.getStatus());
        summary.put("startedAt", attempt.getStartedAt());
        summary.put("submittedAt", attempt.getSubmittedAt());
        summary.put("timeSpent", attempt.getTimeSpent());

        if (attempt.getTimeSpent() != null) {
            summary.put("timeSpentFormatted", formatTimeSpent(attempt.getTimeSpent()));
        }

        // Score information if graded
        if (attempt.getGraded()) {
            summary.put("totalScore", attempt.getTotalScore());
            summary.put("maxScore", attempt.getMaxScore());
            summary.put("percentage", attempt.getPercentage());
            summary.put("passed", attempt.getPassed());
            summary.put("graded", true);
            summary.put("gradedAt", attempt.getGradedAt());
        } else {
            summary.put("graded", false);
        }

        summary.put("autoGraded", attempt.getAutoGraded());
        summary.put("lateSubmission", attempt.getLateSubmission());

        return summary;
    }

    private String formatTimeSpent(int seconds) {
        if (seconds <= 0) return "0m 0s";

        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        int secs = seconds % 60;

        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, secs);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, secs);
        } else {
            return String.format("%ds", secs);
        }
    }
}