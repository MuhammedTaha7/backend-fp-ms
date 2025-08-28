package com.example.edusphere.service;

import com.example.edusphere.dto.request.TaskCreateRequest;
import com.example.edusphere.dto.request.TaskUpdateRequest;
import com.example.edusphere.dto.response.TaskDetailResponse;
import com.example.edusphere.dto.response.TaskResponse;
import com.example.edusphere.entity.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface TaskService {

    /**
     * Create a new task
     */
    TaskResponse createTask(TaskCreateRequest request, String instructorId);

    /**
     * Update an existing task
     */
    TaskResponse updateTask(String taskId, TaskUpdateRequest request, String instructorId);

    /**
     * Delete a task
     */
    void deleteTask(String taskId, String instructorId);

    /**
     * Get a task by ID
     */
    Optional<TaskResponse> getTaskById(String taskId);

    // New method for inter-service communication
    List<Task> findByCourseIdIn(List<String> courseIds);

    /**
     * Get detailed task information
     */
    TaskDetailResponse getTaskDetails(String taskId, String instructorId);

    // ====================================
    // TASK RETRIEVAL METHODS
    // ====================================

    /**
     * Get all tasks for a course
     */
    List<TaskResponse> getTasksByCourse(String courseId);

    /**
     * Get tasks for a course with pagination
     */
    Page<TaskResponse> getTasksByCourse(String courseId, Pageable pageable);

    /**
     * Get tasks by status
     */
    List<TaskResponse> getTasksByStatus(String courseId, String status);

    /**
     * Get tasks by category
     */
    List<TaskResponse> getTasksByCategory(String courseId, String category);

    /**
     * Get tasks by priority
     */
    List<TaskResponse> getTasksByPriority(String courseId, String priority);

    /**
     * Get overdue tasks for a course
     */
    List<TaskResponse> getOverdueTasks(String courseId);

    /**
     * Get upcoming tasks for a course
     */
    List<TaskResponse> getUpcomingTasks(String courseId, int daysAhead);

    /**
     * Get tasks needing grading
     */
    List<TaskResponse> getTasksNeedingGrading(String courseId);

    /**
     * Get tasks by instructor
     */
    List<TaskResponse> getTasksByInstructor(String instructorId);

    /**
     * Get available tasks for students (visible and published)
     */
    List<TaskResponse> getAvailableTasksForStudents(String courseId);

    /**
     * Search tasks by title or description
     */
    List<TaskResponse> searchTasks(String courseId, String searchTerm);

    // ====================================
    // STUDENT-SPECIFIC METHODS
    // ====================================

    /**
     * Get tasks for a specific student in a course
     * Only returns visible and published tasks
     */
    List<TaskResponse> getTasksForStudent(String studentId, String courseId, String status);

    /**
     * Get overdue tasks for a specific student
     */
    List<TaskResponse> getOverdueTasksForStudent(String studentId, String courseId);

    /**
     * Get upcoming tasks for a specific student
     */
    List<TaskResponse> getUpcomingTasksForStudent(String studentId, String courseId, int daysAhead);

    // ====================================
    // PERMISSION AND ACCESS CONTROL
    // ====================================

    /**
     * Check if user can access a task
     */
    boolean canUserAccessTask(String taskId, String userId, String userRole);

    /**
     * Check if user can modify a task
     */
    boolean canUserModifyTask(String taskId, String userId, String userRole);

    // ====================================
    // TASK STATISTICS AND ANALYTICS
    // ====================================

    /**
     * Get task statistics
     */
    TaskDetailResponse.TaskStatistics getTaskStatistics(String taskId);

    /**
     * Recalculate task statistics
     */
    void recalculateTaskStatistics(String taskId);

    /**
     * Recalculate all task statistics for a course
     */
    void recalculateAllTaskStatisticsForCourse(String courseId);

    // ====================================
    // FILE MANAGEMENT
    // ====================================

    /**
     * Attach file to task
     */
    TaskResponse attachFileToTask(String taskId, String fileUrl, String fileName, Long fileSize, String instructorId);

    /**
     * Remove file from task
     */
    void removeFileFromTask(String taskId, String instructorId);

    // ====================================
    // BATCH OPERATIONS
    // ====================================

    /**
     * Update visibility for multiple tasks
     */
    void updateTaskVisibility(String courseId, List<String> taskIds, boolean visible, String instructorId);

    /**
     * Update status for multiple tasks
     */
    void updateTaskStatus(String courseId, List<String> taskIds, String status, String instructorId);
}