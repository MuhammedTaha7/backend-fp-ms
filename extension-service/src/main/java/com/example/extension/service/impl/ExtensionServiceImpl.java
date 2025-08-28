package com.example.extension.service.impl;

import com.example.extension.client.EduSphereClient;
import com.example.extension.dto.response.ExtensionDashboardResponse;
import com.example.extension.dto.response.ExtensionItemResponse;
import com.example.extension.dto.response.ExtensionStatsResponse;
import com.example.extension.service.ExtensionService;

// These imports are no longer needed
// import com.example.edusphere.entity.Course;
// import com.example.edusphere.entity.Task;
// import com.example.edusphere.entity.TaskSubmission;
// import com.example.edusphere.entity.Meeting;
// import com.example.edusphere.repository.CourseRepository;
// import com.example.edusphere.repository.TaskRepository;
// import com.example.edusphere.repository.TaskSubmissionRepository;
// import com.example.edusphere.repository.MeetingRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExtensionServiceImpl implements ExtensionService {

    private final EduSphereClient eduSphereClient;
    // We no longer inject the repositories directly
    // private final TaskRepository taskRepository;
    // private final CourseRepository courseRepository;
    // private final TaskSubmissionRepository taskSubmissionRepository;
    // private final MeetingRepository meetingRepository;

    @Autowired
    public ExtensionServiceImpl(EduSphereClient eduSphereClient) {
        this.eduSphereClient = eduSphereClient;
    }

    @Override
    public ExtensionDashboardResponse getDashboardData(String userId, String userRole) {
        System.out.println("📚 === GETTING EXTENSION DASHBOARD DATA ===");
        System.out.println("User ID: " + userId + ", Role: " + userRole);

        try {
            // Get user's courses by calling the edusphere-service API
            List<Map<String, Object>> userCourses = eduSphereClient.getUserCourses(userId, userRole);
            List<String> courseIds = userCourses.stream()
                    .map(course -> (String) course.get("id"))
                    .collect(Collectors.toList());

            System.out.println("📋 User has access to " + courseIds.size() + " courses");

            if (courseIds.isEmpty()) {
                return new ExtensionDashboardResponse(new ArrayList<>(), new ExtensionStatsResponse());
            }

            List<ExtensionItemResponse> items = new ArrayList<>();

            // Get all tasks from user's courses by calling the edusphere-service API
            List<Map<String, Object>> allTasks = eduSphereClient.getTasksByCourseIds(courseIds);

            System.out.println("✅ Found " + allTasks.size() + " accessible tasks");

            // Convert tasks to extension items
            List<ExtensionItemResponse> taskItems = allTasks.stream()
                    .map(this::convertTaskToExtensionItem)
                    .collect(Collectors.toList());

            items.addAll(taskItems);

            // Get all meetings from user's courses by calling the edusphere-service API
            List<Map<String, Object>> allMeetings = eduSphereClient.getMeetingsByCourseIds(courseIds);

            System.out.println("✅ Found " + allMeetings.size() + " accessible meetings");

            // Convert meetings to extension items
            List<ExtensionItemResponse> meetingItems = allMeetings.stream()
                    .map(this::convertMeetingToExtensionItem)
                    .collect(Collectors.toList());

            items.addAll(meetingItems);

            // Sort all items by priority and due date
            items = items.stream()
                    .sorted(this::compareItemsByPriority)
                    .collect(Collectors.toList());

            // Add mock announcements (this doesn't require API calls to edusphere-service)
            items.addAll(generateMockAnnouncements(userCourses));

            // Calculate statistics
            ExtensionStatsResponse stats = calculateStats(items, userId, userRole);

            ExtensionDashboardResponse response = new ExtensionDashboardResponse(items, stats);

            System.out.println("✅ Dashboard data prepared with " + items.size() + " total items");
            return response;

        } catch (Exception e) {
            System.err.println("❌ Error getting dashboard data: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to get dashboard data: " + e.getMessage());
        }
    }

    // New method to handle getting meeting details from the EduSphereClient
    public Map<String, Object> getMeetingDetails(String meetingId, String email) {
        // You'll need to define this endpoint in EduSphereController.java
        return eduSphereClient.getMeetingById(meetingId, email);
    }

    @Override
    public List<ExtensionItemResponse> getTasks(String userId, String userRole, String status,
                                                String priority, String type, int limit) {
        try {
            System.out.println("✅ === GETTING EXTENSION TASKS ===");
            System.out.println("Filters - Status: " + status + ", Priority: " + priority + ", Type: " + type);

            // Get user's courses via API client
            List<Map<String, Object>> userCourses = eduSphereClient.getUserCourses(userId, userRole);
            List<String> courseIds = userCourses.stream()
                    .map(course -> (String) course.get("id"))
                    .collect(Collectors.toList());

            if (courseIds.isEmpty()) {
                return new ArrayList<>();
            }

            List<ExtensionItemResponse> items = new ArrayList<>();

            // Get tasks from all courses via API client
            List<Map<String, Object>> allTasks = eduSphereClient.getTasksByCourseIds(courseIds);

            // Convert tasks to extension items
            List<ExtensionItemResponse> taskItems = allTasks.stream()
                    .map(this::convertTaskToExtensionItem)
                    .filter(item -> "task".equals(item.getType()))
                    .collect(Collectors.toList());

            items.addAll(taskItems);

            // Get meetings from all courses if type allows
            if (type == null || "all".equals(type) || "meeting".equals(type)) {
                // Get meetings via API client
                List<Map<String, Object>> allMeetings = eduSphereClient.getMeetingsByCourseIds(courseIds);

                // Convert meetings to extension items
                List<ExtensionItemResponse> meetingItems = allMeetings.stream()
                        .map(this::convertMeetingToExtensionItem)
                        .filter(item -> "meeting".equals(item.getType()))
                        .collect(Collectors.toList());

                items.addAll(meetingItems);
            }

            // Apply filters and limit
            List<ExtensionItemResponse> filteredItems = items.stream()
                    .filter(item -> matchesStatusFilter(item, status))
                    .filter(item -> matchesPriorityFilter(item, priority))
                    .sorted(this::compareItemsByPriority)
                    .limit(limit)
                    .collect(Collectors.toList());

            System.out.println("✅ Filtered items: " + filteredItems.size());
            return filteredItems;

        } catch (Exception e) {
            System.err.println("❌ Error getting tasks: " + e.getMessage());
            throw new RuntimeException("Failed to get tasks: " + e.getMessage());
        }
    }

    @Override
    public List<ExtensionItemResponse> getAnnouncements(String userId, String userRole, int limit) {
        try {
            System.out.println("📢 === GETTING EXTENSION ANNOUNCEMENTS ===");

            // Get user's courses via API client
            List<Map<String, Object>> userCourses = eduSphereClient.getUserCourses(userId, userRole);

            // Generate mock announcements (implement real announcements later)
            List<ExtensionItemResponse> announcements = generateMockAnnouncements(userCourses)
                    .stream()
                    .limit(limit)
                    .collect(Collectors.toList());

            System.out.println("✅ Generated " + announcements.size() + " announcements");
            return announcements;

        } catch (Exception e) {
            System.err.println("❌ Error getting announcements: " + e.getMessage());
            throw new RuntimeException("Failed to get announcements: " + e.getMessage());
        }
    }

    @Override
    public ExtensionStatsResponse getUserStats(String userId, String userRole) {
        try {
            System.out.println("📊 === CALCULATING EXTENSION STATS ===");

            // Get dashboard data to calculate stats
            ExtensionDashboardResponse dashboardData = getDashboardData(userId, userRole);
            return dashboardData.getStats();

        } catch (Exception e) {
            System.err.println("❌ Error getting stats: " + e.getMessage());
            throw new RuntimeException("Failed to get stats: " + e.getMessage());
        }
    }

    @Override
    public List<ExtensionItemResponse> getUrgentItems(String userId, String userRole) {
        try {
            System.out.println("🚨 === GETTING URGENT ITEMS ===");

            // Get all items and filter for urgent ones
            ExtensionDashboardResponse dashboardData = getDashboardData(userId, userRole);
            List<ExtensionItemResponse> urgentItems = dashboardData.getItems().stream()
                    .filter(item -> "urgent".equals(item.getPriority()))
                    .sorted(this::compareItemsByDueDate)
                    .collect(Collectors.toList());

            System.out.println("✅ Found " + urgentItems.size() + " urgent items");
            return urgentItems;

        } catch (Exception e) {
            System.err.println("❌ Error getting urgent items: " + e.getMessage());
            throw new RuntimeException("Failed to get urgent items: " + e.getMessage());
        }
    }

    @Override
    public boolean canUserAccessCourse(String userId, String userRole, String courseId) {
        // This logic is now handled by the edusphere-service API.
        // The extension service should not contain this logic.
        return eduSphereClient.canUserAccessCourse(userId, userRole, courseId);
    }

    // Helper methods that now handle Maps instead of Entities
    private List<Map<String, Object>> getUserCourses(String userId, String userRole) {
        try {
            // We'll call the client to get the courses, rather than using a repository directly.
            // This method is now a client call, so we rename it to avoid confusion.
            return eduSphereClient.getUserCourses(userId, userRole);
        } catch (Exception e) {
            System.err.println("❌ Error getting user courses: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private boolean canUserSeeTask(Map<String, Object> task, String userId, String userRole) {
        // Admin can see all tasks
        if ("1100".equals(userRole)) return true;

        // Lecturer can see tasks in their courses
        if ("1200".equals(userRole)) {
            String courseId = (String) task.get("courseId");
            return eduSphereClient.canUserAccessCourse(userId, userRole, courseId);
        }

        // Students can only see published, visible tasks
        if ("1300".equals(userRole)) {
            String courseId = (String) task.get("courseId");
            boolean visibleToStudents = (boolean) task.getOrDefault("visibleToStudents", false);
            boolean isPublished = (boolean) task.getOrDefault("published", false);
            return visibleToStudents && isPublished && eduSphereClient.canUserAccessCourse(userId, userRole, courseId);
        }

        return false;
    }

    private boolean canUserSeeMeeting(Map<String, Object> meeting, String userId, String userRole) {
        // Admin can see all meetings
        if ("1100".equals(userRole)) return true;

        // Lecturer can see meetings in their courses or meetings they created
        if ("1200".equals(userRole)) {
            String courseId = (String) meeting.get("courseId");
            String createdBy = (String) meeting.get("createdBy");
            String lecturerId = (String) meeting.get("lecturerId");
            return eduSphereClient.canUserAccessCourse(userId, userRole, courseId) ||
                    userId.equals(createdBy) ||
                    userId.equals(lecturerId);
        }

        // Students can see meetings in courses they're enrolled in or meetings they're participating in
        if ("1300".equals(userRole)) {
            String courseId = (String) meeting.get("courseId");
            List<String> participants = (List<String>) meeting.get("participants");
            return eduSphereClient.canUserAccessCourse(userId, userRole, courseId) ||
                    (participants != null && participants.contains(userId));
        }

        return false;
    }

    private ExtensionItemResponse convertTaskToExtensionItem(Map<String, Object> task) {
        ExtensionItemResponse item = new ExtensionItemResponse();

        item.setId((String) task.get("id"));
        item.setName((String) task.get("title"));
        item.setDescription((String) task.get("description"));
        item.setType("task");
        item.setDueDate((String) task.get("dueDate"));
        item.setCourse(eduSphereClient.getCourseName((String) task.get("courseId")));

        // The rest of the logic remains the same, but it's now using a Map
        String status = (String) task.getOrDefault("status", "pending");
        item.setStatus(status);

        String priority = calculatePriority(LocalDate.parse(item.getDueDate()), status);
        item.setPriority(priority);

        item.setCategory((String) task.get("category"));
        item.setMaxPoints((Integer) task.get("maxPoints"));
        item.setFileUrl((String) task.get("fileUrl"));
        item.setFileName((String) task.get("fileName"));

        // For students, check submission status via API call
        // This is a complex logic that would require a dedicated API endpoint in edusphere-service
        // For now, we'll simplify this or add a new method to the EduSphereClient
        // For demonstration purposes, this part of the logic needs to be re-evaluated
        // as it requires a specific API endpoint to be defined.

        return item;
    }

    private ExtensionItemResponse convertMeetingToExtensionItem(Map<String, Object> meeting) {
        ExtensionItemResponse item = new ExtensionItemResponse();

        item.setId((String) meeting.get("id"));
        item.setName((String) meeting.get("title"));
        item.setDescription((String) meeting.get("description"));
        item.setType("meeting");

        String datetime = (String) meeting.get("datetime");
        String scheduledAt = (String) meeting.get("scheduledAt");

        if (datetime != null) {
            item.setDueDate(LocalDate.parse(datetime.split("T")[0]).toString());
        } else if (scheduledAt != null) {
            item.setDueDate(LocalDate.parse(scheduledAt.split("T")[0]).toString());
        } else {
            item.setDueDate(LocalDate.now().toString());
        }

        item.setCourse(eduSphereClient.getCourseName((String) meeting.get("courseId")));

        String status = (String) meeting.getOrDefault("status", "pending");
        item.setStatus(status);

        LocalDate meetingDate = datetime != null ?
                LocalDate.parse(datetime.split("T")[0]) :
                (scheduledAt != null ? LocalDate.parse(scheduledAt.split("T")[0]) : LocalDate.now());

        String priority = calculatePriority(meetingDate, status);
        item.setPriority(priority);

        item.setCategory("meeting");
        item.setAnnouncementType((String) meeting.get("type"));
        item.setLocation("Online Meeting");
        item.setIsImportant("active".equals(status));

        item.setFileUrl((String) meeting.get("invitationLink"));

        return item;
    }

    // The rest of the helper methods (determineTaskStatus, etc.) remain largely the same,
    // but they will now use the Map<String, Object> instead of the specific entity objects.
    // The canUserAccessCourse and getCourseName methods are now handled by the client.
    // I've removed the repository calls from these methods and replaced them with client calls.

    private String calculatePriority(LocalDate dueDate, String status) {
        if ("overdue".equals(status)) {
            return "urgent";
        }

        if (dueDate != null) {
            long daysUntilDue = ChronoUnit.DAYS.between(LocalDate.now(), dueDate);

            if (daysUntilDue < 0) return "urgent";
            if (daysUntilDue <= 3) return "urgent";
            if (daysUntilDue <= 7) return "warning";
        }

        return "safe";
    }

    private List<ExtensionItemResponse> generateMockAnnouncements(List<Map<String, Object>> courses) {
        List<ExtensionItemResponse> announcements = new ArrayList<>();

        LocalDate today = LocalDate.now();

        for (Map<String, Object> course : courses.subList(0, Math.min(3, courses.size()))) {
            ExtensionItemResponse careerFair = new ExtensionItemResponse();
            careerFair.setId("ann_" + UUID.randomUUID().toString().substring(0, 8));
            careerFair.setName("Campus Career Fair");
            careerFair.setDescription("Annual career fair with 50+ companies recruiting students");
            careerFair.setType("announcement");
            careerFair.setDueDate(today.plusDays(10).toString());
            careerFair.setStatus("pending");
            careerFair.setPriority("warning");
            careerFair.setCourse("Career Services");
            careerFair.setAnnouncementType("event");
            careerFair.setLocation("Main Campus Hall");
            careerFair.setIsImportant(true);
            announcements.add(careerFair);

            ExtensionItemResponse libraryMaint = new ExtensionItemResponse();
            libraryMaint.setId("ann_" + UUID.randomUUID().toString().substring(0, 8));
            libraryMaint.setName("Library System Maintenance");
            libraryMaint.setDescription("Digital library will be offline for system updates");
            libraryMaint.setType("announcement");
            libraryMaint.setDueDate(today.plusDays(5).toString());
            libraryMaint.setStatus("pending");
            libraryMaint.setPriority("safe");
            libraryMaint.setCourse("Library");
            libraryMaint.setAnnouncementType("maintenance");
            libraryMaint.setLocation("Digital Library");
            libraryMaint.setIsImportant(false);
            announcements.add(libraryMaint);

            break;
        }

        return announcements;
    }

    private ExtensionStatsResponse calculateStats(List<ExtensionItemResponse> items, String userId, String userRole) {
        ExtensionStatsResponse stats = new ExtensionStatsResponse();

        int totalItems = items.size();
        int urgentItems = (int) items.stream().filter(item -> "urgent".equals(item.getPriority())).count();
        int pendingItems = (int) items.stream().filter(item -> "pending".equals(item.getStatus())).count();
        int completedItems = (int) items.stream().filter(item -> "completed".equals(item.getStatus())).count();
        int overdueItems = (int) items.stream().filter(item -> "overdue".equals(item.getStatus())).count();
        int tasksCount = (int) items.stream().filter(item -> "task".equals(item.getType())).count();
        int meetingsCount = (int) items.stream().filter(item -> "meeting".equals(item.getType())).count();
        int announcementsCount = (int) items.stream().filter(item -> "announcement".equals(item.getType())).count();

        double completionRate = totalItems > 0 ? (completedItems * 100.0) / totalItems : 0.0;

        LocalDate today = LocalDate.now();

        int thisWeekDue = (int) items.stream()
                .filter(item -> {
                    try {
                        LocalDate dueDate = LocalDate.parse(item.getDueDate());
                        return !dueDate.isBefore(today) && !dueDate.isAfter(today.plusWeeks(1));
                    } catch (Exception e) {
                        return false;
                    }
                })
                .count();

        int nextWeekDue = (int) items.stream()
                .filter(item -> {
                    try {
                        LocalDate dueDate = LocalDate.parse(item.getDueDate());
                        return dueDate.isAfter(today.plusWeeks(1)) && !dueDate.isAfter(today.plusWeeks(2));
                    } catch (Exception e) {
                        return false;
                    }
                })
                .count();

        stats.setTotalItems(totalItems);
        stats.setUrgentItems(urgentItems);
        stats.setPendingItems(pendingItems);
        stats.setCompletedItems(completedItems);
        stats.setOverdueItems(overdueItems);
        stats.setTasksCount(tasksCount);
        stats.setAnnouncementsCount(announcementsCount);
        stats.setCompletionRate(Math.round(completionRate * 100.0) / 100.0);
        stats.setThisWeekDue(thisWeekDue);
        stats.setNextWeekDue(nextWeekDue);
        stats.setMeetingsCount(meetingsCount);

        return stats;
    }

    private boolean matchesStatusFilter(ExtensionItemResponse item, String statusFilter) {
        return statusFilter == null || "all".equals(statusFilter) || statusFilter.equals(item.getStatus());
    }

    private boolean matchesPriorityFilter(ExtensionItemResponse item, String priorityFilter) {
        return priorityFilter == null || "all".equals(priorityFilter) || priorityFilter.equals(item.getPriority());
    }

    private int compareItemsByPriority(ExtensionItemResponse a, ExtensionItemResponse b) {
        Map<String, Integer> priorityOrder = Map.of(
                "urgent", 0,
                "warning", 1,
                "safe", 2
        );

        int aPriority = priorityOrder.getOrDefault(a.getPriority(), 3);
        int bPriority = priorityOrder.getOrDefault(b.getPriority(), 3);

        if (aPriority != bPriority) {
            return Integer.compare(aPriority, bPriority);
        }

        return compareItemsByDueDate(a, b);
    }

    private int compareItemsByDueDate(ExtensionItemResponse a, ExtensionItemResponse b) {
        try {
            LocalDate dateA = LocalDate.parse(a.getDueDate());
            LocalDate dateB = LocalDate.parse(b.getDueDate());
            return dateA.compareTo(dateB);
        } catch (Exception e) {
            return 0;
        }
    }
}