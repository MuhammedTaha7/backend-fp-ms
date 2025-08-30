package com.example.edusphere.service.impl;

import com.example.edusphere.entity.Task;
import com.example.edusphere.entity.TaskSubmission;
import com.example.edusphere.entity.GradeColumn;
import com.example.edusphere.repository.TaskRepository;
import com.example.edusphere.repository.TaskSubmissionRepository;
import com.example.common.repository.UserRepository;
import com.example.edusphere.repository.GradeColumnRepository;
import com.example.edusphere.service.TaskSubmissionService;
import com.example.edusphere.service.GradeService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class TaskSubmissionServiceImpl implements TaskSubmissionService {

    private final TaskSubmissionRepository taskSubmissionRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final GradeColumnRepository gradeColumnRepository;
    private final GradeService gradeService;

    public TaskSubmissionServiceImpl(TaskSubmissionRepository taskSubmissionRepository,
                                     TaskRepository taskRepository,
                                     UserRepository userRepository,
                                     GradeColumnRepository gradeColumnRepository,
                                     GradeService gradeService) {
        this.taskSubmissionRepository = taskSubmissionRepository;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.gradeColumnRepository = gradeColumnRepository;
        this.gradeService = gradeService;
    }

    @Override
    public TaskSubmission createSubmission(TaskSubmission submission) {

        try {
            // Validate task exists and get course information
            Task task = taskRepository.findById(submission.getTaskId())
                    .orElseThrow(() -> new RuntimeException("Task not found: " + submission.getTaskId()));

            // ✅ AUTO-POPULATE COURSE ID FROM TASK - THIS IS THE KEY FIX
            submission.setCourseId(task.getCourseId());

            // Validate student exists
            userRepository.findById(submission.getStudentId())
                    .orElseThrow(() -> new RuntimeException("Student not found: " + submission.getStudentId()));

            // Check if student can submit
            if (!canStudentSubmit(submission.getTaskId(), submission.getStudentId())) {
                throw new RuntimeException("Student cannot submit to this task");
            }

            // Set defaults
            if (submission.getStatus() == null) {
                submission.setStatus("submitted");
            }
            if (submission.getAttemptNumber() == null) {
                // Calculate attempt number
                long existingAttempts = taskSubmissionRepository.countByTaskIdAndStudentId(
                        submission.getTaskId(), submission.getStudentId());
                submission.setAttemptNumber((int) existingAttempts + 1);
            }
            if (submission.getSubmittedAt() == null) {
                submission.setSubmittedAt(LocalDateTime.now());
            }

            // Check if submission is late
            if (task.getDueDateTime() != null &&
                    submission.getSubmittedAt().isAfter(task.getDueDateTime())) {
                submission.setIsLate(true);

                // Calculate late penalty if applicable
                if (task.getLatePenaltyPerDay() != null && task.getLatePenaltyPerDay() > 0) {
                    // Calculate days late
                    long daysLate = Duration.between(
                            task.getDueDateTime(), submission.getSubmittedAt()).toDays();
                    double penalty = Math.min(daysLate * task.getLatePenaltyPerDay(), 100.0);
                    submission.setLatePenaltyApplied(penalty);
                }
            }

            TaskSubmission savedSubmission = taskSubmissionRepository.save(submission);

            // Update task statistics
            recalculateTaskStatistics(submission.getTaskId());
            return savedSubmission;

        } catch (Exception e) {
            System.err.println("❌ Error creating task submission: " + e.getMessage());
            throw new RuntimeException("Failed to create submission: " + e.getMessage());
        }
    }

    @Override
    public TaskSubmission updateSubmission(String submissionId, TaskSubmission submission) {

        try {
            TaskSubmission existing = taskSubmissionRepository.findById(submissionId)
                    .orElseThrow(() -> new RuntimeException("Submission not found: " + submissionId));

            // Update fields
            if (submission.getContent() != null) {
                existing.setContent(submission.getContent());
            }
            if (submission.getNotes() != null) {
                existing.setNotes(submission.getNotes());
            }
            if (submission.getGrade() != null) {
                existing.setGrade(submission.getGrade());
                existing.setGradedAt(LocalDateTime.now());
            }
            if (submission.getFeedback() != null) {
                existing.setFeedback(submission.getFeedback());
            }
            if (submission.getStatus() != null) {
                existing.setStatus(submission.getStatus());
            }

            // ✅ ENSURE COURSE ID IS ALWAYS SET
            if (existing.getCourseId() == null) {
                Optional<Task> taskOpt = taskRepository.findById(existing.getTaskId());
                if (taskOpt.isPresent()) {
                    existing.setCourseId(taskOpt.get().getCourseId());
                }
            }

            TaskSubmission savedSubmission = taskSubmissionRepository.save(existing);

            // Update task statistics if grade was changed
            if (submission.getGrade() != null) {
                recalculateTaskStatistics(existing.getTaskId());
            }
            return savedSubmission;

        } catch (Exception e) {
            System.err.println("❌ Error updating task submission: " + e.getMessage());
            throw new RuntimeException("Failed to update submission: " + e.getMessage());
        }
    }

    @Override
    public void deleteSubmission(String submissionId) {

        try {
            TaskSubmission submission = taskSubmissionRepository.findById(submissionId)
                    .orElseThrow(() -> new RuntimeException("Submission not found: " + submissionId));

            String taskId = submission.getTaskId();

            // Remove grade from grade column if it was graded
            if (submission.getGrade() != null) {
                removeGradeFromGradeColumn(submission);
            }

            taskSubmissionRepository.deleteById(submissionId);

            // Update task statistics
            recalculateTaskStatistics(taskId);

        } catch (Exception e) {
            System.err.println("❌ Error deleting task submission: " + e.getMessage());
            throw new RuntimeException("Failed to delete submission: " + e.getMessage());
        }
    }

    @Override
    public Optional<TaskSubmission> findSubmissionById(String submissionId) {
        try {
            Optional<TaskSubmission> submission = taskSubmissionRepository.findById(submissionId);

            // ✅ FIX MISSING COURSE ID FOR EXISTING SUBMISSIONS
            if (submission.isPresent() && submission.get().getCourseId() == null) {
                TaskSubmission sub = submission.get();
                Optional<Task> taskOpt = taskRepository.findById(sub.getTaskId());
                if (taskOpt.isPresent()) {
                    sub.setCourseId(taskOpt.get().getCourseId());
                    taskSubmissionRepository.save(sub);
                }
            }

            return submission;
        } catch (Exception e) {
            System.err.println("❌ Error finding submission by ID: " + e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public List<TaskSubmission> findSubmissionsByTaskId(String taskId) {
        try {
            List<TaskSubmission> submissions = taskSubmissionRepository.findByTaskIdOrderBySubmittedAtDesc(taskId);
            return fixMissingCourseIds(submissions);
        } catch (Exception e) {
            System.err.println("❌ Error finding submissions by task ID: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public List<TaskSubmission> findSubmissionsByCourseId(String courseId) {
        try {
            List<TaskSubmission> submissions = taskSubmissionRepository.findByCourseIdOrderBySubmittedAtDesc(courseId);

            // ✅ ALSO CHECK FOR SUBMISSIONS WITH MISSING COURSE ID
            List<TaskSubmission> submissionsWithMissingCourseId = taskSubmissionRepository.findByCourseIdIsNull();
            List<TaskSubmission> fixedSubmissions = new ArrayList<>();

            for (TaskSubmission sub : submissionsWithMissingCourseId) {
                Optional<Task> taskOpt = taskRepository.findById(sub.getTaskId());
                if (taskOpt.isPresent() && courseId.equals(taskOpt.get().getCourseId())) {
                    sub.setCourseId(courseId);
                    TaskSubmission saved = taskSubmissionRepository.save(sub);
                    fixedSubmissions.add(saved);
                }
            }

            submissions.addAll(fixedSubmissions);
            submissions = submissions.stream()
                    .sorted((a, b) -> b.getSubmittedAt().compareTo(a.getSubmittedAt()))
                    .collect(Collectors.toList());
            return submissions;
        } catch (Exception e) {
            System.err.println("❌ Error finding submissions by course ID: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public List<TaskSubmission> findSubmissionsByStudentId(String studentId) {
        try {
            List<TaskSubmission> submissions = taskSubmissionRepository.findByStudentIdOrderBySubmittedAtDesc(studentId);
            return fixMissingCourseIds(submissions);
        } catch (Exception e) {
            System.err.println("❌ Error finding submissions by student ID: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * ✅ NEW METHOD: Fix missing course IDs for existing submissions
     */
    private List<TaskSubmission> fixMissingCourseIds(List<TaskSubmission> submissions) {
        List<TaskSubmission> fixed = new ArrayList<>();
        boolean anyFixed = false;

        for (TaskSubmission submission : submissions) {
            if (submission.getCourseId() == null) {
                Optional<Task> taskOpt = taskRepository.findById(submission.getTaskId());
                if (taskOpt.isPresent()) {
                    submission.setCourseId(taskOpt.get().getCourseId());
                    TaskSubmission saved = taskSubmissionRepository.save(submission);
                    fixed.add(saved);
                    anyFixed = true;
                } else {
                    fixed.add(submission);
                }
            } else {
                fixed.add(submission);
            }
        }

        if (anyFixed) {
        }

        return fixed;
    }

    @Override
    public Page<TaskSubmission> findSubmissionsByTaskId(String taskId, Pageable pageable) {
        try {
            return taskSubmissionRepository.findByTaskIdOrderBySubmittedAtDesc(taskId, pageable);
        } catch (Exception e) {
            System.err.println("❌ Error finding submissions page by task ID: " + e.getMessage());
            return Page.empty();
        }
    }

    @Override
    public Optional<TaskSubmission> findSubmissionByTaskAndStudent(String taskId, String studentId) {
        try {
            Optional<TaskSubmission> submission = taskSubmissionRepository.findByTaskIdAndStudentId(taskId, studentId);

            // Fix missing course ID if found
            if (submission.isPresent() && submission.get().getCourseId() == null) {
                TaskSubmission sub = submission.get();
                Optional<Task> taskOpt = taskRepository.findById(sub.getTaskId());
                if (taskOpt.isPresent()) {
                    sub.setCourseId(taskOpt.get().getCourseId());
                    taskSubmissionRepository.save(sub);
                }
            }

            return submission;
        } catch (Exception e) {
            System.err.println("❌ Error finding submission by task and student: " + e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public List<TaskSubmission> findSubmissionsByStudentAndCourse(String studentId, String courseId) {
        try {
            List<TaskSubmission> submissions = taskSubmissionRepository.findByStudentIdOrderBySubmittedAtDesc(studentId)
                    .stream()
                    .filter(submission -> {
                        // Fix missing course ID if needed
                        if (submission.getCourseId() == null) {
                            Optional<Task> taskOpt = taskRepository.findById(submission.getTaskId());
                            if (taskOpt.isPresent()) {
                                submission.setCourseId(taskOpt.get().getCourseId());
                                taskSubmissionRepository.save(submission);
                            }
                        }
                        return courseId.equals(submission.getCourseId());
                    })
                    .collect(Collectors.toList());

            return submissions;
        } catch (Exception e) {
            System.err.println("❌ Error finding submissions by student and course: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public TaskSubmission updateSubmissionGrade(String submissionId, Integer grade, String feedback) {

        try {
            TaskSubmission submission = taskSubmissionRepository.findById(submissionId)
                    .orElseThrow(() -> new RuntimeException("Submission not found: " + submissionId));

            submission.setGrade(grade);
            submission.setFeedback(feedback);
            submission.setStatus("graded");
            submission.setGradedAt(LocalDateTime.now());

            TaskSubmission savedSubmission = taskSubmissionRepository.save(submission);

            // Update task statistics
            recalculateTaskStatistics(submission.getTaskId());
            return savedSubmission;

        } catch (Exception e) {
            System.err.println("❌ Error updating submission grade: " + e.getMessage());
            throw new RuntimeException("Failed to update grade: " + e.getMessage());
        }
    }

    @Override
    public TaskSubmission updateSubmissionGradeWithSync(String submissionId, Integer grade, String feedback) {

        try {
            // First update the submission grade
            TaskSubmission submission = updateSubmissionGrade(submissionId, grade, feedback);

            // Now sync with grade column
            syncSubmissionGradeToGradeColumn(submission);
            return submission;

        } catch (Exception e) {
            System.err.println("❌ Error updating submission grade with sync: " + e.getMessage());
            throw new RuntimeException("Failed to update and sync grade: " + e.getMessage());
        }
    }

    /**
     * NEW: Check if student can update their own submission
     */
    public boolean canStudentUpdateSubmission(String submissionId, String studentId) {
        try {
            Optional<TaskSubmission> submissionOpt = taskSubmissionRepository.findById(submissionId);
            if (submissionOpt.isEmpty()) {
                return false;
            }

            TaskSubmission submission = submissionOpt.get();

            // Student must own the submission
            if (!studentId.equals(submission.getStudentId())) {
                return false;
            }

            // Cannot update if already graded
            if (submission.getGrade() != null) {
                return false;
            }

            // Check if task still allows updates
            Optional<Task> taskOpt = taskRepository.findById(submission.getTaskId());
            if (taskOpt.isEmpty()) {
                return false;
            }

            Task task = taskOpt.get();

            // Cannot update if task no longer allows submissions
            if (Boolean.FALSE.equals(task.getAllowSubmissions())) {
                return false;
            }

            // Check due date if specified
            if (task.getDueDateTime() != null) {
                LocalDateTime now = LocalDateTime.now();
                // Allow updates up to 1 hour after due date for student corrections
                if (now.isAfter(task.getDueDateTime().plusHours(1))) {
                    return false;
                }
            }

            return true;

        } catch (Exception e) {
            System.err.println("❌ Error checking if student can update submission: " + e.getMessage());
            return false;
        }
    }

    /**
     * NEW: Check if student can delete their own submission
     */
    public boolean canStudentDeleteSubmission(String submissionId, String studentId) {
        try {
            Optional<TaskSubmission> submissionOpt = taskSubmissionRepository.findById(submissionId);
            if (submissionOpt.isEmpty()) {
                return false;
            }

            TaskSubmission submission = submissionOpt.get();

            // Student must own the submission
            if (!studentId.equals(submission.getStudentId())) {
                return false;
            }

            // Cannot delete if already graded
            if (submission.getGrade() != null) {
                return false;
            }

            // Check if task still allows deletions
            Optional<Task> taskOpt = taskRepository.findById(submission.getTaskId());
            if (taskOpt.isEmpty()) {
                return false;
            }

            Task task = taskOpt.get();

            // Cannot delete if task no longer allows submissions
            if (Boolean.FALSE.equals(task.getAllowSubmissions())) {
                return false;
            }

            // Check due date - allow deletion only if submitted within last 30 minutes
            LocalDateTime now = LocalDateTime.now();
            if (submission.getSubmittedAt().isBefore(now.minusMinutes(30))) {
                return false;
            }

            return true;

        } catch (Exception e) {
            System.err.println("❌ Error checking if student can delete submission: " + e.getMessage());
            return false;
        }
    }

    /**
     * Sync submission grade to the corresponding grade column
     */
    private void syncSubmissionGradeToGradeColumn(TaskSubmission submission) {
        try {

            // Find the task to get course information
            Optional<Task> taskOpt = taskRepository.findById(submission.getTaskId());
            if (taskOpt.isEmpty()) {
                System.err.println("❌ Task not found for submission: " + submission.getTaskId());
                return;
            }

            Task task = taskOpt.get();
            String courseId = task.getCourseId();

            // Find the grade column linked to this task
            Optional<GradeColumn> gradeColumnOpt = gradeColumnRepository
                    .findByCourseIdAndLinkedAssignmentId(courseId, submission.getTaskId());

            if (gradeColumnOpt.isEmpty()) {

                // Try to auto-create grade column
                GradeColumn autoCreatedColumn = autoCreateGradeColumnForTask(task);
                if (autoCreatedColumn == null) {
                    System.err.println("❌ Failed to auto-create grade column");
                    return;
                }
                gradeColumnOpt = Optional.of(autoCreatedColumn);
            }

            GradeColumn gradeColumn = gradeColumnOpt.get();

            // Convert submission grade to percentage if needed
            Double gradePercentage = convertGradeToPercentage(submission.getGrade(), task.getMaxPoints());

            // Update the student's grade in the grade column
            gradeService.updateStudentGrade(submission.getStudentId(), gradeColumn.getId(), gradePercentage);

        } catch (Exception e) {
            System.err.println("❌ Error syncing grade to grade column: " + e.getMessage());
            e.printStackTrace();
            // Don't throw exception - submission grade should still be saved
        }
    }

    /**
     * Remove grade from grade column when submission is deleted or ungraded
     */
    private void removeGradeFromGradeColumn(TaskSubmission submission) {
        try {

            // Find the task
            Optional<Task> taskOpt = taskRepository.findById(submission.getTaskId());
            if (taskOpt.isEmpty()) {
                return;
            }

            Task task = taskOpt.get();

            // Find the grade column
            Optional<GradeColumn> gradeColumnOpt = gradeColumnRepository
                    .findByCourseIdAndLinkedAssignmentId(task.getCourseId(), task.getId());

            if (gradeColumnOpt.isPresent()) {
                GradeColumn gradeColumn = gradeColumnOpt.get();
                // Remove the grade (set to null)
                gradeService.updateStudentGrade(submission.getStudentId(), gradeColumn.getId(), null);
            }

        } catch (Exception e) {
            System.err.println("❌ Error removing grade from column: " + e.getMessage());
        }
    }

    /**
     * Auto-create grade column for task if it doesn't exist
     */
    private GradeColumn autoCreateGradeColumnForTask(Task task) {
        try {

            // Check if grade column already exists
            Optional<GradeColumn> existingColumn = gradeColumnRepository
                    .findByCourseIdAndLinkedAssignmentId(task.getCourseId(), task.getId());

            if (existingColumn.isPresent()) {
                return existingColumn.get();
            }

            // Calculate suggested percentage based on task type
            int suggestedPercentage = calculateSuggestedPercentage(task.getType());

            // Get current total percentage for the course
            List<GradeColumn> existingColumns = gradeColumnRepository.findByCourseId(task.getCourseId());
            int currentTotal = existingColumns.stream()
                    .mapToInt(col -> col.getPercentage() != null ? col.getPercentage() : 0)
                    .sum();

            // Ensure total doesn't exceed 100%
            if (currentTotal + suggestedPercentage > 100) {
                suggestedPercentage = Math.max(1, 100 - currentTotal);
            }

            // Create the grade column
            GradeColumn gradeColumn = new GradeColumn();
            gradeColumn.setCourseId(task.getCourseId());
            gradeColumn.setName(task.getTitle());
            gradeColumn.setType(task.getType());
            gradeColumn.setPercentage(suggestedPercentage);
            gradeColumn.setMaxPoints(task.getMaxPoints());
            gradeColumn.setDescription("Auto-created for task: " + task.getTitle());
            gradeColumn.setLinkedAssignmentId(task.getId());
            gradeColumn.setAutoCreated(true);
            gradeColumn.setCreatedBy("auto");
            gradeColumn.setIsActive(true);
            gradeColumn.setDisplayOrder(existingColumns.size() + 1);

            GradeColumn savedColumn = gradeColumnRepository.save(gradeColumn);

            return savedColumn;

        } catch (Exception e) {
            System.err.println("❌ Error auto-creating grade column: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Calculate suggested percentage based on task type
     */
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

    /**
     * Convert submission grade to percentage based on max points
     */
    private Double convertGradeToPercentage(Integer grade, Integer maxPoints) {
        if (grade == null) {
            return null;
        }

        if (maxPoints == null || maxPoints == 0) {
            // Assume grade is already a percentage
            return grade.doubleValue();
        }

        // Convert to percentage
        return (grade.doubleValue() / maxPoints.doubleValue()) * 100.0;
    }

    @Override
    public List<TaskSubmission> findUngraduatedSubmissionsByTask(String taskId) {
        try {
            return taskSubmissionRepository.findUngraduatedSubmissionsByTask(taskId);
        } catch (Exception e) {
            System.err.println("❌ Error finding ungraduated submissions: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public List<TaskSubmission> findSubmissionsNeedingGrading(String courseId) {
        try {
            return taskSubmissionRepository.findSubmissionsNeedingAttention(courseId);
        } catch (Exception e) {
            System.err.println("❌ Error finding submissions needing grading: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public long countSubmissionsByTask(String taskId) {
        try {
            return taskSubmissionRepository.countByTaskId(taskId);
        } catch (Exception e) {
            System.err.println("❌ Error counting submissions by task: " + e.getMessage());
            return 0;
        }
    }

    @Override
    public long countSubmissionsByStudent(String studentId) {
        try {
            return taskSubmissionRepository.findByStudentIdOrderBySubmittedAtDesc(studentId).size();
        } catch (Exception e) {
            System.err.println("❌ Error counting submissions by student: " + e.getMessage());
            return 0;
        }
    }

    @Override
    public long countGradedSubmissionsByTask(String taskId) {
        try {
            return taskSubmissionRepository.countGradedSubmissionsByTask(taskId);
        } catch (Exception e) {
            System.err.println("❌ Error counting graded submissions: " + e.getMessage());
            return 0;
        }
    }

    @Override
    public double calculateAverageGradeForTask(String taskId) {
        try {
            List<TaskSubmission> gradedSubmissions = taskSubmissionRepository.findGradedSubmissionsByTask(taskId);

            if (gradedSubmissions.isEmpty()) {
                return 0.0;
            }

            double sum = gradedSubmissions.stream()
                    .filter(s -> s.getGrade() != null)
                    .mapToDouble(TaskSubmission::getGrade)
                    .sum();

            return sum / gradedSubmissions.size();
        } catch (Exception e) {
            System.err.println("❌ Error calculating average grade: " + e.getMessage());
            return 0.0;
        }
    }

    @Override
    public TaskSubmission addFileToSubmission(String submissionId, String fileUrl, String fileName, Long fileSize) {
        try {
            TaskSubmission submission = taskSubmissionRepository.findById(submissionId)
                    .orElseThrow(() -> new RuntimeException("Submission not found: " + submissionId));

            submission.addFile(fileUrl, fileName, fileSize);

            return taskSubmissionRepository.save(submission);
        } catch (Exception e) {
            System.err.println("❌ Error adding file to submission: " + e.getMessage());
            throw new RuntimeException("Failed to add file: " + e.getMessage());
        }
    }

    @Override
    public TaskSubmission removeFileFromSubmission(String submissionId, int fileIndex) {
        try {
            TaskSubmission submission = taskSubmissionRepository.findById(submissionId)
                    .orElseThrow(() -> new RuntimeException("Submission not found: " + submissionId));

            submission.removeFile(fileIndex);

            return taskSubmissionRepository.save(submission);
        } catch (Exception e) {
            System.err.println("❌ Error removing file from submission: " + e.getMessage());
            throw new RuntimeException("Failed to remove file: " + e.getMessage());
        }
    }

    @Override
    public List<TaskSubmission> batchGradeSubmissions(List<String> submissionIds, Integer grade, String feedback) {

        try {
            List<TaskSubmission> updatedSubmissions = new ArrayList<>();

            for (String submissionId : submissionIds) {
                TaskSubmission updated = updateSubmissionGrade(submissionId, grade, feedback);
                updatedSubmissions.add(updated);
            }
            return updatedSubmissions;

        } catch (Exception e) {
            System.err.println("❌ Error in batch grading: " + e.getMessage());
            throw new RuntimeException("Failed to batch grade submissions: " + e.getMessage());
        }
    }

    @Override
    public List<TaskSubmission> batchGradeSubmissionsWithSync(List<String> submissionIds, Integer grade, String feedback) {

        try {
            List<TaskSubmission> updatedSubmissions = new ArrayList<>();

            for (String submissionId : submissionIds) {
                try {
                    TaskSubmission updated = updateSubmissionGradeWithSync(submissionId, grade, feedback);
                    updatedSubmissions.add(updated);
                } catch (Exception e) {
                    System.err.println("❌ Error grading submission " + submissionId + ": " + e.getMessage());
                    // Continue with other submissions
                }
            }
            return updatedSubmissions;

        } catch (Exception e) {
            System.err.println("❌ Error in batch grading with sync: " + e.getMessage());
            throw new RuntimeException("Failed to batch grade submissions with sync: " + e.getMessage());
        }
    }

    @Override
    public void recalculateTaskStatistics(String taskId) {
        try {

            Optional<Task> taskOpt = taskRepository.findById(taskId);
            if (taskOpt.isEmpty()) {
                System.err.println("❌ Task not found for statistics update: " + taskId);
                return;
            }

            Task task = taskOpt.get();
            List<TaskSubmission> submissions = taskSubmissionRepository.findByTaskId(taskId);

            // Update submission count
            task.setSubmissionCount(submissions.size());

            // Update graded count
            long gradedCount = submissions.stream()
                    .filter(s -> s.getGrade() != null)
                    .count();
            task.setGradedCount((int) gradedCount);

            // Update average grade
            double averageGrade = calculateAverageGradeForTask(taskId);
            task.setAverageGrade(averageGrade);

            taskRepository.save(task);

        } catch (Exception e) {
            System.err.println("❌ Error recalculating task statistics: " + e.getMessage());
        }
    }

    @Override
    public List<TaskSubmission> findLateSubmissionsByTask(String taskId) {
        try {
            return taskSubmissionRepository.findByTaskIdAndIsLateTrue(taskId);
        } catch (Exception e) {
            System.err.println("❌ Error finding late submissions: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public List<TaskSubmission> findSubmissionsByDateRange(String courseId, LocalDateTime start, LocalDateTime end) {
        try {
            return taskSubmissionRepository.findByCourseIdAndSubmittedAtBetween(courseId, start, end);
        } catch (Exception e) {
            System.err.println("❌ Error finding submissions by date range: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public List<TaskSubmission> findRecentSubmissions(String courseId, int days) {
        try {
            LocalDateTime sinceDate = LocalDateTime.now().minusDays(days);
            return taskSubmissionRepository.findByCourseIdAndSubmittedAtBetween(
                    courseId, sinceDate, LocalDateTime.now());
        } catch (Exception e) {
            System.err.println("❌ Error finding recent submissions: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public boolean canStudentSubmit(String taskId, String studentId) {
        try {
            // Check if task exists and accepts submissions
            Optional<Task> taskOpt = taskRepository.findById(taskId);
            if (taskOpt.isEmpty()) {
                return false;
            }

            Task task = taskOpt.get();

            // Check if task accepts submissions
            if (Boolean.FALSE.equals(task.getAllowSubmissions())) {
                return false;
            }

            // Check if task is visible to students
            if (Boolean.FALSE.equals(task.getVisibleToStudents())) {
                return false;
            }

            // Check if task is published
            if (!task.isPublished()) {
                return false;
            }

            // Check due date
            if (task.getDueDateTime() != null) {
                LocalDateTime now = LocalDateTime.now();
                if (now.isAfter(task.getDueDateTime()) && Boolean.FALSE.equals(task.getAllowLateSubmissions())) {
                    return false;
                }
            }

            // Check attempt limits
            if (task.getMaxAttempts() != null && task.getMaxAttempts() > 0) {
                long existingAttempts = taskSubmissionRepository.countByTaskIdAndStudentId(taskId, studentId);
                return existingAttempts < task.getMaxAttempts();
            }

            return true;
        } catch (Exception e) {
            System.err.println("❌ Error checking if student can submit: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean hasStudentSubmitted(String taskId, String studentId) {
        try {
            return taskSubmissionRepository.existsByTaskIdAndStudentId(taskId, studentId);
        } catch (Exception e) {
            System.err.println("❌ Error checking if student has submitted: " + e.getMessage());
            return false;
        }
    }

    @Override
    public int getSubmissionAttemptCount(String taskId, String studentId) {
        try {
            return (int) taskSubmissionRepository.countByTaskIdAndStudentId(taskId, studentId);
        } catch (Exception e) {
            System.err.println("❌ Error getting submission attempt count: " + e.getMessage());
            return 0;
        }
    }
}