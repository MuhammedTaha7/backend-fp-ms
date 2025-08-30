package com.example.edusphere.service.impl;

import com.example.edusphere.dto.request.TaskCreateRequest;
import com.example.edusphere.dto.request.TaskUpdateRequest;
import com.example.edusphere.dto.response.TaskDetailResponse;
import com.example.edusphere.dto.response.TaskResponse;
import com.example.edusphere.entity.Course;
import com.example.edusphere.entity.Task;
import com.example.edusphere.entity.TaskSubmission;
import com.example.edusphere.entity.GradeColumn;
import com.example.edusphere.repository.CourseRepository;
import com.example.edusphere.repository.TaskRepository;
import com.example.edusphere.repository.TaskSubmissionRepository;
import com.example.common.repository.UserRepository;
import com.example.edusphere.repository.GradeColumnRepository;
import com.example.edusphere.service.TaskService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final CourseRepository courseRepository;
    private final TaskSubmissionRepository taskSubmissionRepository;
    private final UserRepository userRepository;
    private final GradeColumnRepository gradeColumnRepository;

    public TaskServiceImpl(TaskRepository taskRepository,
                           CourseRepository courseRepository,
                           TaskSubmissionRepository taskSubmissionRepository,
                           UserRepository userRepository,
                           GradeColumnRepository gradeColumnRepository) {
        this.taskRepository = taskRepository;
        this.courseRepository = courseRepository;
        this.taskSubmissionRepository = taskSubmissionRepository;
        this.userRepository = userRepository;
        this.gradeColumnRepository = gradeColumnRepository;
    }

    /**
     * Auto-create grade column for assignment
     */
    private GradeColumn autoCreateGradeColumnForTask(Task task, String instructorId) {
        try {

            // Check if grade column already exists for this task
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
            gradeColumn.setDescription("Auto-created for assignment: " + task.getTitle());
            gradeColumn.setLinkedAssignmentId(task.getId());
            gradeColumn.setAutoCreated(true);
            gradeColumn.setCreatedBy(instructorId);
            gradeColumn.setIsActive(true);
            gradeColumn.setDisplayOrder(existingColumns.size() + 1);

            GradeColumn savedColumn = gradeColumnRepository.save(gradeColumn);

            return savedColumn;

        } catch (Exception e) {
            System.err.println("❌ Error auto-creating grade column: " + e.getMessage());
            e.printStackTrace();
            // Don't throw error - task creation should still succeed
            return null;
        }
    }

    /**
     * Calculate suggested percentage based on task type
     */
    private int calculateSuggestedPercentage(String taskType) {
        Map<String, Integer> typePercentages = Map.of(
                "homework", 10,
                "project", 25,
                "essay", 15,
                "lab", 10,
                "presentation", 15,
                "quiz", 5,
                "exam", 20
        );

        return typePercentages.getOrDefault(taskType, 10);
    }

    @Override
    public TaskResponse createTask(TaskCreateRequest request, String instructorId) {

        try {
            // Validate that the course exists and instructor has access
            Course course = courseRepository.findById(request.getCourseId())
                    .orElseThrow(() -> new RuntimeException("Course not found: " + request.getCourseId()));

            if (!canUserModifyCourse(course, instructorId)) {
                throw new RuntimeException("Access denied: You don't have permission to create tasks in this course");
            }

            // Create task entity
            Task task = new Task();
            task.setTitle(request.getTitle().trim());
            task.setDescription(request.getDescription());
            task.setCourseId(request.getCourseId());
            task.setType(request.getType());
            task.setDueDate(request.getDueDate());
            task.setDueTime(request.getDueTime());
            task.setMaxPoints(request.getMaxPoints());
            task.setInstructions(request.getInstructions());
            task.setPriority(request.getPriority());
            task.setDifficulty(request.getDifficulty());
            task.setCategory(request.getCategory());
            task.setAllowSubmissions(request.getAllowSubmissions());
            task.setAllowLateSubmissions(request.getAllowLateSubmissions());
            task.setLatePenaltyPerDay(request.getLatePenaltyPerDay());
            task.setVisibleToStudents(request.getVisibleToStudents());
            task.setRequiresSubmission(request.getRequiresSubmission());
            task.setMaxAttempts(request.getMaxAttempts());
            task.setEstimatedDuration(request.getEstimatedDuration());
            task.setPublishDate(request.getPublishDate());
            task.setInstructorId(instructorId);
            task.setStatus("active");

            // Set tags and prerequisites
            if (request.getTags() != null) {
                task.setTags(request.getTags());
            }
            if (request.getPrerequisiteTasks() != null) {
                task.setPrerequisiteTasks(request.getPrerequisiteTasks());
            }

            // File attachment
            if (request.getFileUrl() != null) {
                task.setFileUrl(request.getFileUrl());
                task.setFileName(request.getFileName());
                task.setFileSize(request.getFileSize());
            }

            // Initialize statistics
            task.setSubmissionCount(0);
            task.setGradedCount(0);
            task.setAverageGrade(0.0);

            Task savedTask = taskRepository.save(task);

            // AUTO-CREATE GRADE COLUMN FOR THE TASK
            GradeColumn gradeColumn = autoCreateGradeColumnForTask(savedTask, instructorId);

            return convertToResponse(savedTask);

        } catch (RuntimeException e) {
            System.err.println("❌ Error creating task: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            System.err.println("❌ Unexpected error creating task: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to create task: " + e.getMessage());
        }
    }

    @Override
    public TaskResponse updateTask(String taskId, TaskUpdateRequest request, String instructorId) {

        try {
            Task task = taskRepository.findById(taskId)
                    .orElseThrow(() -> new RuntimeException("Task not found: " + taskId));

            if (!canUserModifyTask(taskId, instructorId, "1200")) {
                throw new RuntimeException("Access denied: You don't have permission to modify this task");
            }

            // Track title change for grade column update
            String oldTitle = task.getTitle();
            boolean titleChanged = false;

            // Update fields if provided
            if (request.getTitle() != null && !request.getTitle().trim().isEmpty()) {
                titleChanged = !oldTitle.equals(request.getTitle().trim());
                task.setTitle(request.getTitle().trim());
            }
            if (request.getDescription() != null) {
                task.setDescription(request.getDescription());
            }
            if (request.getType() != null) {
                task.setType(request.getType());
            }
            if (request.getDueDate() != null) {
                task.setDueDate(request.getDueDate());
            }
            if (request.getDueTime() != null) {
                task.setDueTime(request.getDueTime());
            }
            if (request.getMaxPoints() != null) {
                task.setMaxPoints(request.getMaxPoints());
            }
            if (request.getInstructions() != null) {
                task.setInstructions(request.getInstructions());
            }
            if (request.getStatus() != null) {
                task.setStatus(request.getStatus());
            }
            if (request.getPriority() != null) {
                task.setPriority(request.getPriority());
            }
            if (request.getDifficulty() != null) {
                task.setDifficulty(request.getDifficulty());
            }
            if (request.getCategory() != null) {
                task.setCategory(request.getCategory());
            }
            if (request.getAllowSubmissions() != null) {
                task.setAllowSubmissions(request.getAllowSubmissions());
            }
            if (request.getAllowLateSubmissions() != null) {
                task.setAllowLateSubmissions(request.getAllowLateSubmissions());
            }
            if (request.getLatePenaltyPerDay() != null) {
                task.setLatePenaltyPerDay(request.getLatePenaltyPerDay());
            }
            if (request.getVisibleToStudents() != null) {
                task.setVisibleToStudents(request.getVisibleToStudents());
            }
            if (request.getRequiresSubmission() != null) {
                task.setRequiresSubmission(request.getRequiresSubmission());
            }
            if (request.getMaxAttempts() != null) {
                task.setMaxAttempts(request.getMaxAttempts());
            }
            if (request.getEstimatedDuration() != null) {
                task.setEstimatedDuration(request.getEstimatedDuration());
            }
            if (request.getPublishDate() != null) {
                task.setPublishDate(request.getPublishDate());
            }
            if (request.getTags() != null) {
                task.setTags(request.getTags());
            }
            if (request.getPrerequisiteTasks() != null) {
                task.setPrerequisiteTasks(request.getPrerequisiteTasks());
            }

            // Update file attachment
            if (request.getFileUrl() != null) {
                task.setFileUrl(request.getFileUrl());
                task.setFileName(request.getFileName());
                task.setFileSize(request.getFileSize());
            }

            Task savedTask = taskRepository.save(task);

            // UPDATE CORRESPONDING GRADE COLUMN IF EXISTS
            Optional<GradeColumn> linkedColumn = gradeColumnRepository
                    .findByCourseIdAndLinkedAssignmentId(task.getCourseId(), taskId);

            if (linkedColumn.isPresent()) {
                GradeColumn column = linkedColumn.get();
                boolean columnChanged = false;

                if (titleChanged) {
                    column.setName(task.getTitle());
                    columnChanged = true;
                }

                if (request.getType() != null) {
                    column.setType(request.getType());
                    columnChanged = true;
                }

                if (request.getMaxPoints() != null) {
                    column.setMaxPoints(request.getMaxPoints());
                    columnChanged = true;
                }

                if (columnChanged) {
                    gradeColumnRepository.save(column);
                }
            }

            return convertToResponse(savedTask);

        } catch (RuntimeException e) {
            System.err.println("❌ Error updating task: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            System.err.println("❌ Unexpected error updating task: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to update task: " + e.getMessage());
        }
    }

    @Override
    public void deleteTask(String taskId, String instructorId) {

        try {
            Task task = taskRepository.findById(taskId)
                    .orElseThrow(() -> new RuntimeException("Task not found: " + taskId));

            if (!canUserModifyTask(taskId, instructorId, "1200")) {
                throw new RuntimeException("Access denied: You don't have permission to delete this task");
            }

            // Check if there are submissions
            List<TaskSubmission> submissions = taskSubmissionRepository.findByTaskId(taskId);
            if (!submissions.isEmpty()) {
                // Delete submissions as well
                taskSubmissionRepository.deleteAll(submissions);
            }

            // DELETE CORRESPONDING GRADE COLUMN IF EXISTS
            Optional<GradeColumn> linkedColumn = gradeColumnRepository
                    .findByCourseIdAndLinkedAssignmentId(task.getCourseId(), taskId);

            if (linkedColumn.isPresent()) {
                gradeColumnRepository.delete(linkedColumn.get());
            }

            taskRepository.deleteById(taskId);

        } catch (RuntimeException e) {
            System.err.println("❌ Error deleting task: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            System.err.println("❌ Unexpected error deleting task: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to delete task: " + e.getMessage());
        }
    }

    @Override
    public Optional<TaskResponse> getTaskById(String taskId) {
        try {
            return taskRepository.findById(taskId)
                    .map(this::convertToResponse);
        } catch (Exception e) {
            System.err.println("❌ Error fetching task: " + e.getMessage());
            return Optional.empty();
        }
    }

    // New method implementation for inter-service communication
    @Override
    public List<Task> findByCourseIdIn(List<String> courseIds) {
        return taskRepository.findByCourseIdInOrderByDueDateAsc(courseIds);
    }

    @Override
    public List<TaskResponse> getTasksByCourse(String courseId) {
        try {
            List<Task> tasks = taskRepository.findByCourseIdOrderByDueDateAsc(courseId);
            return tasks.stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("❌ Error fetching tasks by course: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public Page<TaskResponse> getTasksByCourse(String courseId, Pageable pageable) {
        try {
            Page<Task> tasksPage = taskRepository.findByCourseIdOrderByDueDateAsc(courseId, pageable);
            return tasksPage.map(this::convertToResponse);
        } catch (Exception e) {
            System.err.println("❌ Error fetching tasks page: " + e.getMessage());
            return Page.empty();
        }
    }

    @Override
    public List<TaskResponse> getTasksByStatus(String courseId, String status) {
        try {
            List<Task> tasks = taskRepository.findByCourseIdAndStatusOrderByDueDateAsc(courseId, status);
            return tasks.stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("❌ Error fetching tasks by status: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public List<TaskResponse> getOverdueTasks(String courseId) {
        try {
            LocalDate today = LocalDate.now();
            List<Task> tasks = taskRepository.findByCourseIdAndDueDateBeforeAndStatusActive(courseId, today);
            return tasks.stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("❌ Error fetching overdue tasks: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public List<TaskResponse> getUpcomingTasks(String courseId, int daysAhead) {
        try {
            LocalDate today = LocalDate.now();
            LocalDate endDate = today.plusDays(daysAhead);
            List<Task> tasks = taskRepository.findByCourseIdAndDueDateBetweenAndStatusActive(courseId, today, endDate);
            return tasks.stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("❌ Error fetching upcoming tasks: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public List<TaskResponse> searchTasks(String courseId, String searchTerm) {
        try {
            List<Task> tasks = taskRepository.searchByCourseIdAndTitleOrDescription(courseId, searchTerm);
            return tasks.stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("❌ Error searching tasks: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // NEW STUDENT-SPECIFIC METHODS

    @Override
    public List<TaskResponse> getTasksForStudent(String studentId, String courseId, String status) {
        try {

            List<Task> tasks;
            if (status != null && !status.trim().isEmpty()) {
                tasks = taskRepository.findByCourseIdAndStatusOrderByDueDateAsc(courseId, status);
            } else {
                tasks = taskRepository.findByCourseIdOrderByDueDateAsc(courseId);
            }

            // Filter to only show tasks visible to students and published
            List<Task> visibleTasks = tasks.stream()
                    .filter(task -> Boolean.TRUE.equals(task.getVisibleToStudents()))
                    .filter(Task::isPublished)
                    .collect(Collectors.toList());

            List<TaskResponse> taskResponses = new ArrayList<>();
            for (Task task : visibleTasks) {
                TaskResponse response = convertToResponse(task);

                // Add student-specific information
                enhanceTaskResponseForStudent(response, task, studentId);

                taskResponses.add(response);
            }

            return taskResponses;

        } catch (Exception e) {
            System.err.println("❌ Error fetching tasks for student: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public List<TaskResponse> getOverdueTasksForStudent(String studentId, String courseId) {
        try {

            LocalDateTime now = LocalDateTime.now();
            List<Task> tasks;

            if (courseId != null && !courseId.trim().isEmpty()) {
                tasks = taskRepository.findByCourseIdOrderByDueDateAsc(courseId);
            } else {
                // Get tasks from all courses the student is enrolled in
                tasks = taskRepository.findAllByOrderByDueDateAsc();
            }

            List<TaskResponse> overdueTasks = new ArrayList<>();
            for (Task task : tasks) {
                // Only include visible, published tasks
                if (!Boolean.TRUE.equals(task.getVisibleToStudents()) || !task.isPublished()) {
                    continue;
                }

                // Check if task is overdue
                if (task.getDueDateTime() != null && now.isAfter(task.getDueDateTime())) {
                    // Check if student has already submitted
                    Optional<TaskSubmission> submission = taskSubmissionRepository
                            .findByTaskIdAndStudentId(task.getId(), studentId);

                    if (submission.isEmpty()) {
                        TaskResponse response = convertToResponse(task);
                        enhanceTaskResponseForStudent(response, task, studentId);
                        overdueTasks.add(response);
                    }
                }
            }

            return overdueTasks;

        } catch (Exception e) {
            System.err.println("❌ Error fetching overdue tasks for student: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public List<TaskResponse> getUpcomingTasksForStudent(String studentId, String courseId, int daysAhead) {
        try {

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime endDate = now.plusDays(daysAhead);
            List<Task> tasks;

            if (courseId != null && !courseId.trim().isEmpty()) {
                tasks = taskRepository.findByCourseIdOrderByDueDateAsc(courseId);
            } else {
                // Get tasks from all courses the student is enrolled in
                tasks = taskRepository.findAllByOrderByDueDateAsc();
            }

            List<TaskResponse> upcomingTasks = new ArrayList<>();
            for (Task task : tasks) {
                // Only include visible, published tasks
                if (!Boolean.TRUE.equals(task.getVisibleToStudents()) || !task.isPublished()) {
                    continue;
                }

                // Check if task is upcoming (due within the specified period)
                if (task.getDueDateTime() != null &&
                        task.getDueDateTime().isAfter(now) &&
                        task.getDueDateTime().isBefore(endDate)) {

                    TaskResponse response = convertToResponse(task);
                    enhanceTaskResponseForStudent(response, task, studentId);
                    upcomingTasks.add(response);
                }
            }

            return upcomingTasks;

        } catch (Exception e) {
            System.err.println("❌ Error fetching upcoming tasks for student: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Enhance task response with student-specific information
     */
    private void enhanceTaskResponseForStudent(TaskResponse response, Task task, String studentId) {
        try {
            // Check if student has submitted
            Optional<TaskSubmission> submission = taskSubmissionRepository
                    .findByTaskIdAndStudentId(task.getId(), studentId);

            if (submission.isPresent()) {
                TaskSubmission sub = submission.get();
                response.setHasSubmission(true);
                response.setSubmissionId(sub.getId());
                response.setSubmissionGrade(sub.getGrade());
                response.setSubmissionStatus(sub.getStatus());
                response.setSubmittedAt(sub.getSubmittedAt());
                response.setSubmissionFeedback(sub.getFeedback());
                response.setIsLateSubmission(sub.getIsLate());
            } else {
                response.setHasSubmission(false);

                // Check if student can still submit
                response.setCanSubmit(canStudentSubmitToTask(task, studentId));
            }

            // Calculate time until due
            if (task.getDueDateTime() != null) {
                LocalDateTime now = LocalDateTime.now();
                if (now.isBefore(task.getDueDateTime())) {
                    long hoursUntilDue = java.time.Duration.between(now, task.getDueDateTime()).toHours();
                    response.setHoursUntilDue(hoursUntilDue);
                } else {
                    response.setOverdue(true);
                    long hoursOverdue = java.time.Duration.between(task.getDueDateTime(), now).toHours();
                    response.setHoursOverdue(hoursOverdue);
                }
            }

        } catch (Exception e) {
            System.err.println("❌ Error enhancing task response for student: " + e.getMessage());
        }
    }

    /**
     * Check if student can submit to a specific task
     */
    private boolean canStudentSubmitToTask(Task task, String studentId) {
        try {
            // Task must allow submissions
            if (Boolean.FALSE.equals(task.getAllowSubmissions())) {
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
                long existingAttempts = taskSubmissionRepository.countByTaskIdAndStudentId(task.getId(), studentId);
                return existingAttempts < task.getMaxAttempts();
            }

            return true;

        } catch (Exception e) {
            System.err.println("❌ Error checking if student can submit: " + e.getMessage());
            return false;
        }
    }

    // Helper methods
    private TaskResponse convertToResponse(Task task) {
        TaskResponse response = TaskResponse.fromEntity(task);

        // Enhance with additional data
        try {
            // Get course name
            courseRepository.findById(task.getCourseId()).ifPresent(course -> {
                response.setCourseName(course.getName());
            });

            // Get instructor name
            if (task.getInstructorId() != null) {
                userRepository.findById(task.getInstructorId()).ifPresent(instructor -> {
                    response.setInstructorName(instructor.getName());
                });
            }

            // Calculate enrolled students count
            courseRepository.findById(task.getCourseId()).ifPresent(course -> {
                int enrolledCount = course.getEnrollments().stream()
                        .mapToInt(enrollment -> enrollment.getStudentIds().size())
                        .sum();
                response.setEnrolledStudents(enrolledCount);
            });

        } catch (Exception e) {
            System.err.println("❌ Error enhancing task response: " + e.getMessage());
        }

        return response;
    }

    private boolean canUserModifyCourse(Course course, String userId) {
        // Admin can modify any course
        // Lecturer can modify their own courses
        return course.getLecturerId().equals(userId);
    }

    @Override
    public boolean canUserAccessTask(String taskId, String userId, String userRole) {
        try {
            Optional<Task> taskOpt = taskRepository.findById(taskId);
            if (taskOpt.isEmpty()) return false;

            Task task = taskOpt.get();

            // Admin can access any task
            if ("1100".equals(userRole)) return true;

            // Lecturer can access tasks in their courses
            if ("1200".equals(userRole)) {
                Optional<Course> courseOpt = courseRepository.findById(task.getCourseId());
                return courseOpt.isPresent() && courseOpt.get().getLecturerId().equals(userId);
            }

            // Students can access published, visible tasks in their enrolled courses
            if ("1300".equals(userRole)) {
                Optional<Course> courseOpt = courseRepository.findById(task.getCourseId());
                if (courseOpt.isEmpty()) return false;

                Course course = courseOpt.get();
                boolean isEnrolled = course.getEnrollments().stream()
                        .anyMatch(enrollment -> enrollment.getStudentIds().contains(userId));

                return isEnrolled && task.getVisibleToStudents() && task.isPublished();
            }

            return false;
        } catch (Exception e) {
            System.err.println("❌ Error checking task access: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean canUserModifyTask(String taskId, String userId, String userRole) {
        try {
            Optional<Task> taskOpt = taskRepository.findById(taskId);
            if (taskOpt.isEmpty()) return false;

            Task task = taskOpt.get();

            // Admin can modify any task
            if ("1100".equals(userRole)) return true;

            // Lecturer can modify tasks they created or in their courses
            if ("1200".equals(userRole)) {
                if (task.getInstructorId().equals(userId)) return true;

                Optional<Course> courseOpt = courseRepository.findById(task.getCourseId());
                return courseOpt.isPresent() && courseOpt.get().getLecturerId().equals(userId);
            }

            // Students cannot modify tasks
            return false;
        } catch (Exception e) {
            System.err.println("❌ Error checking task modification rights: " + e.getMessage());
            return false;
        }
    }

    // Implement remaining interface methods...
    @Override
    public TaskDetailResponse getTaskDetails(String taskId, String instructorId) {

        try {
            Task task = taskRepository.findById(taskId)
                    .orElseThrow(() -> new RuntimeException("Task not found: " + taskId));

            if (!canUserAccessTask(taskId, instructorId, "1200")) {
                throw new RuntimeException("Access denied: You don't have permission to view this task");
            }

            TaskDetailResponse response = new TaskDetailResponse();

            // Copy all properties from TaskResponse
            TaskResponse baseResponse = convertToResponse(task);
            response.setId(baseResponse.getId());
            response.setTitle(baseResponse.getTitle());
            response.setDescription(baseResponse.getDescription());
            response.setCourseId(baseResponse.getCourseId());
            response.setCourseName(baseResponse.getCourseName());
            response.setType(baseResponse.getType());
            response.setDueDate(baseResponse.getDueDate());
            response.setDueTime(baseResponse.getDueTime());
            response.setDueDateTime(baseResponse.getDueDateTime());
            response.setMaxPoints(baseResponse.getMaxPoints());
            response.setStatus(baseResponse.getStatus());
            response.setPriority(baseResponse.getPriority());
            response.setDifficulty(baseResponse.getDifficulty());
            response.setCategory(baseResponse.getCategory());
            response.setInstructions(baseResponse.getInstructions());
            response.setEstimatedDuration(baseResponse.getEstimatedDuration());
            response.setFileUrl(baseResponse.getFileUrl());
            response.setFileName(baseResponse.getFileName());
            response.setFileSize(baseResponse.getFileSize());
            response.setHasAttachment(baseResponse.isHasAttachment());
            response.setAllowSubmissions(baseResponse.getAllowSubmissions());
            response.setAllowLateSubmissions(baseResponse.getAllowLateSubmissions());
            response.setLatePenaltyPerDay(baseResponse.getLatePenaltyPerDay());
            response.setVisibleToStudents(baseResponse.getVisibleToStudents());
            response.setRequiresSubmission(baseResponse.getRequiresSubmission());
            response.setMaxAttempts(baseResponse.getMaxAttempts());
            response.setPublishDate(baseResponse.getPublishDate());
            response.setSubmissionCount(baseResponse.getSubmissionCount());
            response.setGradedCount(baseResponse.getGradedCount());
            response.setAverageGrade(baseResponse.getAverageGrade());
            response.setEnrolledStudents(baseResponse.getEnrolledStudents());
            response.setCompletionRate(baseResponse.getCompletionRate());
            response.setOverdue(baseResponse.isOverdue());
            response.setPublished(baseResponse.isPublished());
            response.setAcceptsSubmissions(baseResponse.isAcceptsSubmissions());
            response.setInstructorId(baseResponse.getInstructorId());
            response.setInstructorName(baseResponse.getInstructorName());
            response.setTags(baseResponse.getTags());
            response.setPrerequisiteTasks(baseResponse.getPrerequisiteTasks());
            response.setProgress(baseResponse.getProgress());
            response.setCreatedAt(baseResponse.getCreatedAt());
            response.setUpdatedAt(baseResponse.getUpdatedAt());

            // Add detailed information
            try {
                // Get recent submissions
                List<TaskSubmission> submissions = taskSubmissionRepository.findByTaskId(taskId);
                if (submissions.size() > 10) {
                    submissions = submissions.subList(0, 10); // Limit to 10 most recent
                }

                List<TaskDetailResponse.TaskSubmissionSummary> submissionSummaries = submissions.stream()
                        .map(this::convertToSubmissionSummary)
                        .collect(Collectors.toList());

                response.setRecentSubmissions(submissionSummaries);

                // Calculate statistics
                TaskDetailResponse.TaskStatistics stats = calculateTaskStatistics(task, submissions);
                response.setStatistics(stats);

                // Get prerequisite task details
                if (task.getPrerequisiteTasks() != null && !task.getPrerequisiteTasks().isEmpty()) {
                    List<TaskResponse> prerequisiteDetails = task.getPrerequisiteTasks().stream()
                            .map(taskRepository::findById)
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            .map(this::convertToResponse)
                            .collect(Collectors.toList());

                    response.setPrerequisiteTaskDetails(prerequisiteDetails);
                }

            } catch (Exception e) {
                System.err.println("⚠️ Error loading additional task details: " + e.getMessage());
                // Continue without additional details
            }

            return response;

        } catch (RuntimeException e) {
            System.err.println("❌ Error fetching task details: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            System.err.println("❌ Unexpected error fetching task details: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to fetch task details: " + e.getMessage());
        }
    }

    private TaskDetailResponse.TaskSubmissionSummary convertToSubmissionSummary(TaskSubmission submission) {
        TaskDetailResponse.TaskSubmissionSummary summary = new TaskDetailResponse.TaskSubmissionSummary();
        summary.setId(submission.getId());
        summary.setStudentId(submission.getStudentId());
        summary.setSubmittedAt(submission.getSubmittedAt());
        summary.setGrade(submission.getGrade());
        summary.setStatus(submission.getStatus());
        summary.setLate(submission.getIsLate());
        summary.setAttemptNumber(submission.getAttemptNumber());
        summary.setFeedback(submission.getFeedback());

        // Get student name
        try {
            userRepository.findById(submission.getStudentId()).ifPresent(student -> {
                summary.setStudentName(student.getName());
            });
        } catch (Exception e) {
            summary.setStudentName("Unknown Student");
        }

        return summary;
    }

    private TaskDetailResponse.TaskStatistics calculateTaskStatistics(Task task, List<TaskSubmission> submissions) {
        TaskDetailResponse.TaskStatistics stats = new TaskDetailResponse.TaskStatistics();

        try {
            // Get enrolled students count
            int enrolledStudents = 0;
            try {
                Optional<Course> courseOpt = courseRepository.findById(task.getCourseId());
                if (courseOpt.isPresent()) {
                    enrolledStudents = courseOpt.get().getEnrollments().stream()
                            .mapToInt(enrollment -> enrollment.getStudentIds().size())
                            .sum();
                }
            } catch (Exception e) {
                System.err.println("⚠️ Could not calculate enrolled students: " + e.getMessage());
            }

            stats.setTotalStudents(enrolledStudents);
            stats.setSubmittedCount(submissions.size());

            // Calculate graded count
            int gradedCount = (int) submissions.stream()
                    .filter(s -> s.getGrade() != null)
                    .count();
            stats.setGradedCount(gradedCount);
            stats.setPendingCount(submissions.size() - gradedCount);

            // Calculate late submissions
            int lateSubmissions = (int) submissions.stream()
                    .filter(s -> s.getIsLate() != null && s.getIsLate())
                    .count();
            stats.setLateSubmissions(lateSubmissions);

            // Calculate completion rate
            double completionRate = enrolledStudents > 0 ?
                    (submissions.size() * 100.0) / enrolledStudents : 0.0;
            stats.setCompletionRate(Math.round(completionRate * 100.0) / 100.0);

            // Calculate grade statistics
            List<Integer> grades = submissions.stream()
                    .filter(s -> s.getGrade() != null)
                    .map(TaskSubmission::getGrade)
                    .collect(Collectors.toList());

            if (!grades.isEmpty()) {
                double averageGrade = grades.stream().mapToInt(Integer::intValue).average().orElse(0.0);
                stats.setAverageGrade(Math.round(averageGrade * 100.0) / 100.0);
                stats.setHighestGrade(grades.stream().mapToInt(Integer::intValue).max().orElse(0));
                stats.setLowestGrade(grades.stream().mapToInt(Integer::intValue).min().orElse(0));

                // Calculate grade distribution
                TaskDetailResponse.GradeDistribution distribution = calculateGradeDistribution(grades);
                stats.setGradeDistribution(distribution);
            }

            // Additional statistics can be calculated here
            stats.setStudentsStarted(submissions.size());
            stats.setStudentsCompleted(gradedCount);

        } catch (Exception e) {
            System.err.println("⚠️ Error calculating task statistics: " + e.getMessage());
        }

        return stats;
    }

    private TaskDetailResponse.GradeDistribution calculateGradeDistribution(List<Integer> grades) {
        int gradeA = 0, gradeB = 0, gradeC = 0, gradeD = 0, gradeF = 0;

        for (Integer grade : grades) {
            if (grade >= 90) gradeA++;
            else if (grade >= 80) gradeB++;
            else if (grade >= 70) gradeC++;
            else if (grade >= 60) gradeD++;
            else gradeF++;
        }

        return new TaskDetailResponse.GradeDistribution(gradeA, gradeB, gradeC, gradeD, gradeF);
    }

    @Override
    public List<TaskResponse> getTasksByInstructor(String instructorId) {
        try {
            List<Task> tasks = taskRepository.findByInstructorIdOrderByDueDateAsc(instructorId);
            return tasks.stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("❌ Error fetching tasks by instructor: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public List<TaskResponse> getAvailableTasksForStudents(String courseId) {
        try {
            LocalDateTime now = LocalDateTime.now();
            List<Task> tasks = taskRepository.findAvailableTasksForStudents(courseId, now);
            return tasks.stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("❌ Error fetching available tasks for students: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public List<TaskResponse> getTasksByCategory(String courseId, String category) {
        try {
            List<Task> tasks = taskRepository.findByCourseIdAndCategoryOrderByDueDateAsc(courseId, category);
            return tasks.stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("❌ Error fetching tasks by category: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public List<TaskResponse> getTasksByPriority(String courseId, String priority) {
        try {
            List<Task> tasks = taskRepository.findByCourseIdAndPriorityOrderByDueDateAsc(courseId, priority);
            return tasks.stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("❌ Error fetching tasks by priority: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public List<TaskResponse> getTasksNeedingGrading(String courseId) {
        try {
            List<Task> tasks = taskRepository.findTasksNeedingGrading(courseId);
            return tasks.stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("❌ Error fetching tasks needing grading: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // Placeholder implementations for remaining methods
    @Override
    public TaskDetailResponse.TaskStatistics getTaskStatistics(String taskId) {
        return null; // Implement based on your analytics needs
    }

    @Override
    public TaskResponse attachFileToTask(String taskId, String fileUrl, String fileName, Long fileSize, String instructorId) {
        return null; // Implement file attachment logic
    }

    @Override
    public void removeFileFromTask(String taskId, String instructorId) {
        // Implement file removal logic
    }

    @Override
    public void updateTaskVisibility(String courseId, List<String> taskIds, boolean visible, String instructorId) {
        // Implement batch visibility update
    }

    @Override
    public void updateTaskStatus(String courseId, List<String> taskIds, String status, String instructorId) {
        // Implement batch status update
    }

    @Override
    public void recalculateTaskStatistics(String taskId) {
        // Implement statistics recalculation
    }

    @Override
    public void recalculateAllTaskStatisticsForCourse(String courseId) {
        // Implement bulk statistics recalculation
    }
}