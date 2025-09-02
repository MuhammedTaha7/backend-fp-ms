package com.example.extension.service.impl;

import com.example.extension.client.EduSphereClient;
import com.example.extension.dto.response.ExtensionDashboardResponse;
import com.example.extension.dto.response.ExtensionItemResponse;
import com.example.extension.dto.response.ExtensionStatsResponse;
import com.example.extension.service.ExtensionService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExtensionServiceImpl implements ExtensionService {

    private final EduSphereClient eduSphereClient;

    @Autowired
    public ExtensionServiceImpl(EduSphereClient eduSphereClient) {
        this.eduSphereClient = eduSphereClient;
    }

    @Override
    public ExtensionDashboardResponse getDashboardData(String userId, String userRole) {
        try {
            System.out.println("📊 Getting dashboard data for user: " + userId + ", role: " + userRole);

            // Get user's courses by calling the edusphere-service API
            List<Map<String, Object>> userCourses = eduSphereClient.getUserCourses(userId, userRole);
            System.out.println("📚 Found " + userCourses.size() + " courses for user");

            List<String> courseIds = userCourses.stream()
                    .map(course -> (String) course.get("id"))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            if (courseIds.isEmpty()) {
                System.out.println("⚠️ No course IDs found, returning empty dashboard");
                return new ExtensionDashboardResponse(new ArrayList<>(), new ExtensionStatsResponse());
            }

            List<ExtensionItemResponse> items = new ArrayList<>();

            // Get all tasks from user's courses by calling the edusphere-service API
            System.out.println("🔍 Fetching tasks for courses: " + courseIds);
            List<Map<String, Object>> allTasks = eduSphereClient.getTasksByCourseIds(courseIds, userId);
            System.out.println(" Found " + allTasks.size() + " tasks");

            // Convert tasks to extension items with proper filtering
            List<ExtensionItemResponse> taskItems = allTasks.stream()
                    .filter(task -> canUserSeeTask(task, userId, userRole))
                    .map(this::convertTaskToExtensionItem)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            items.addAll(taskItems);
            System.out.println("📝 Added " + taskItems.size() + " task items");

            // Get all meetings from user's courses by calling the edusphere-service API
            System.out.println("🔍 Fetching meetings for courses: " + courseIds);
            List<Map<String, Object>> allMeetings = eduSphereClient.getMeetingsByCourseIds(courseIds, userId);
            System.out.println(" Found " + allMeetings.size() + " meetings");

            // Convert meetings to extension items with proper filtering
            List<ExtensionItemResponse> meetingItems = allMeetings.stream()
                    .filter(meeting -> canUserSeeMeeting(meeting, userId, userRole))
                    .map(this::convertMeetingToExtensionItem)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            items.addAll(meetingItems);
            System.out.println("🎥 Added " + meetingItems.size() + " meeting items");

            // Get announcements by calling the edusphere-service API
            System.out.println("📢 Fetching announcements for user: " + userId);
            List<Map<String, Object>> allAnnouncements = eduSphereClient.getAnnouncementsForUser(userId);
            System.out.println(" Found " + allAnnouncements.size() + " announcements");

            List<ExtensionItemResponse> announcementItems = allAnnouncements.stream()
                    .map(this::convertAnnouncementToExtensionItem)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            items.addAll(announcementItems);
            System.out.println("📢 Added " + announcementItems.size() + " announcement items");

            // Sort all items by priority and due date
            items = items.stream()
                    .sorted(this::compareItemsByPriority)
                    .collect(Collectors.toList());

            // Calculate statistics
            ExtensionStatsResponse stats = calculateStats(items, userId, userRole);

            System.out.println("📊 Dashboard data prepared with " + items.size() + " total items");
            return new ExtensionDashboardResponse(items, stats);

        } catch (Exception e) {
            System.err.println("Error getting dashboard data: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to get dashboard data: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getMeetingDetails(String meetingId, String email) {
        return eduSphereClient.getMeetingById(meetingId, email);
    }

    @Override
    public List<ExtensionItemResponse> getTasks(String userId, String userRole, String status,
                                                String priority, String type, int limit) {
        try {
            // Get user's courses via API client
            List<Map<String, Object>> userCourses = eduSphereClient.getUserCourses(userId, userRole);
            List<String> courseIds = userCourses.stream()
                    .map(course -> (String) course.get("id"))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            if (courseIds.isEmpty()) {
                return new ArrayList<>();
            }

            List<ExtensionItemResponse> items = new ArrayList<>();

            // Get tasks from all courses via API client
            List<Map<String, Object>> allTasks = eduSphereClient.getTasksByCourseIds(courseIds, userId);

            // Convert tasks to extension items with filtering
            List<ExtensionItemResponse> taskItems = allTasks.stream()
                    .filter(task -> canUserSeeTask(task, userId, userRole))
                    .map(this::convertTaskToExtensionItem)
                    .filter(Objects::nonNull)
                    .filter(item -> "task".equals(item.getType()))
                    .collect(Collectors.toList());

            items.addAll(taskItems);

            // Get meetings from all courses if type allows
            if (type == null || "all".equals(type) || "meeting".equals(type)) {
                List<Map<String, Object>> allMeetings = eduSphereClient.getMeetingsByCourseIds(courseIds, userId);

                List<ExtensionItemResponse> meetingItems = allMeetings.stream()
                        .filter(meeting -> canUserSeeMeeting(meeting, userId, userRole))
                        .map(this::convertMeetingToExtensionItem)
                        .filter(Objects::nonNull)
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

            return filteredItems;

        } catch (Exception e) {
            System.err.println("Error getting tasks: " + e.getMessage());
            throw new RuntimeException("Failed to get tasks: " + e.getMessage());
        }
    }

    @Override
    public List<ExtensionItemResponse> getAnnouncements(String userId, String userRole, int limit) {
        try {
            List<Map<String, Object>> allAnnouncements = eduSphereClient.getAnnouncementsForUser(userId);

            List<ExtensionItemResponse> announcements = allAnnouncements.stream()
                    .map(this::convertAnnouncementToExtensionItem)
                    .filter(Objects::nonNull)
                    .limit(limit)
                    .collect(Collectors.toList());

            return announcements;

        } catch (Exception e) {
            System.err.println("Error getting announcements: " + e.getMessage());
            throw new RuntimeException("Failed to get announcements: " + e.getMessage());
        }
    }

    @Override
    public ExtensionStatsResponse getUserStats(String userId, String userRole) {
        try {
            ExtensionDashboardResponse dashboardData = getDashboardData(userId, userRole);
            return dashboardData.getStats();
        } catch (Exception e) {
            System.err.println("Error getting stats: " + e.getMessage());
            throw new RuntimeException("Failed to get stats: " + e.getMessage());
        }
    }

    @Override
    public List<ExtensionItemResponse> getUrgentItems(String userId, String userRole) {
        try {
            ExtensionDashboardResponse dashboardData = getDashboardData(userId, userRole);
            List<ExtensionItemResponse> urgentItems = dashboardData.getItems().stream()
                    .filter(item -> "urgent".equals(item.getPriority()))
                    .sorted(this::compareItemsByDueDate)
                    .collect(Collectors.toList());

            return urgentItems;
        } catch (Exception e) {
            System.err.println("Error getting urgent items: " + e.getMessage());
            throw new RuntimeException("Failed to get urgent items: " + e.getMessage());
        }
    }

    @Override
    public boolean canUserAccessCourse(String userId, String userRole, String courseId) {
        return eduSphereClient.canUserAccessCourse(userId, userRole, courseId);
    }

    // Enhanced task visibility logic
    private boolean canUserSeeTask(Map<String, Object> task, String userId, String userRole) {
        try {
            // Admin can see all tasks
            if ("1100".equals(userRole)) return true;

            // Lecturer can see tasks in their courses
            if ("1200".equals(userRole)) {
                String courseId = (String) task.get("courseId");
                return courseId != null && eduSphereClient.canUserAccessCourse(userId, userRole, courseId);
            }

            // Students can only see published, visible tasks
            if ("1300".equals(userRole)) {
                String courseId = (String) task.get("courseId");
                Boolean visibleToStudents = (Boolean) task.get("visibleToStudents");
                Boolean isPublished = (Boolean) task.get("published");

                return courseId != null &&
                        Boolean.TRUE.equals(visibleToStudents) &&
                        Boolean.TRUE.equals(isPublished) &&
                        eduSphereClient.canUserAccessCourse(userId, userRole, courseId);
            }

            return false;
        } catch (Exception e) {
            System.err.println("Error checking task visibility: " + e.getMessage());
            return false;
        }
    }

    // Enhanced meeting visibility logic
    private boolean canUserSeeMeeting(Map<String, Object> meeting, String userId, String userRole) {
        try {
            // Admin can see all meetings
            if ("1100".equals(userRole)) return true;

            // Lecturer can see meetings in their courses or meetings they created
            if ("1200".equals(userRole)) {
                String courseId = (String) meeting.get("courseId");
                String createdBy = (String) meeting.get("createdBy");
                String lecturerId = (String) meeting.get("lecturerId");

                return (courseId != null && eduSphereClient.canUserAccessCourse(userId, userRole, courseId)) ||
                        userId.equals(createdBy) ||
                        userId.equals(lecturerId);
            }

            // Students can see meetings in courses they're enrolled in
            if ("1300".equals(userRole)) {
                String courseId = (String) meeting.get("courseId");
                @SuppressWarnings("unchecked")
                List<String> participants = (List<String>) meeting.get("participants");

                return (courseId != null && eduSphereClient.canUserAccessCourse(userId, userRole, courseId)) ||
                        (participants != null && participants.contains(userId));
            }

            return false;
        } catch (Exception e) {
            System.err.println("Error checking meeting visibility: " + e.getMessage());
            return false;
        }
    }

    // Enhanced conversion methods with better error handling
    private ExtensionItemResponse convertTaskToExtensionItem(Map<String, Object> task) {
        try {
            ExtensionItemResponse item = new ExtensionItemResponse();

            item.setId((String) task.get("id"));
            item.setName((String) task.get("title"));
            item.setDescription((String) task.get("description"));
            item.setType("task");

            // Handle date conversion properly
            String dueDateStr = convertToDateString(task.get("dueDate"));
            item.setDueDate(dueDateStr);

            String courseId = (String) task.get("courseId");
            item.setCourse(eduSphereClient.getCourseName(courseId));

            // Determine status
            String status = (String) task.getOrDefault("status", "pending");
            item.setStatus(status);

            // Calculate priority
            LocalDate dueDate = LocalDate.parse(dueDateStr);
            String priority = calculatePriority(dueDate, status);
            item.setPriority(priority);

            item.setCategory((String) task.get("category"));
            Object maxPointsObj = task.get("maxPoints");
            if (maxPointsObj instanceof Number) {
                item.setMaxPoints(((Number) maxPointsObj).intValue());
            }
            item.setFileUrl((String) task.get("fileUrl"));
            item.setFileName((String) task.get("fileName"));

            return item;
        } catch (Exception e) {
            System.err.println("Error converting task to extension item: " + e.getMessage());
            return null;
        }
    }

    private ExtensionItemResponse convertMeetingToExtensionItem(Map<String, Object> meeting) {
        try {
            ExtensionItemResponse item = new ExtensionItemResponse();

            item.setId((String) meeting.get("id"));
            item.setName((String) meeting.get("title"));
            item.setDescription((String) meeting.get("description"));
            item.setType("meeting");

            // Handle datetime conversion
            String datetime = (String) meeting.get("datetime");
            String scheduledAt = (String) meeting.get("scheduledAt");
            String dueDateStr;

            if (datetime != null) {
                dueDateStr = convertToDateString(datetime);
            } else if (scheduledAt != null) {
                dueDateStr = convertToDateString(scheduledAt);
            } else {
                dueDateStr = LocalDate.now().toString();
            }

            item.setDueDate(dueDateStr);

            String courseId = (String) meeting.get("courseId");
            item.setCourse(eduSphereClient.getCourseName(courseId));

            String status = (String) meeting.getOrDefault("status", "pending");
            item.setStatus(status);

            LocalDate meetingDate = LocalDate.parse(dueDateStr);
            String priority = calculatePriority(meetingDate, status);
            item.setPriority(priority);

            item.setCategory("meeting");
            item.setAnnouncementType((String) meeting.get("type"));
            item.setLocation((String) meeting.getOrDefault("location", "Online Meeting"));
            item.setIsImportant("active".equals(status));

            // Use invitationLink for joining meetings
            item.setFileUrl((String) meeting.get("invitationLink"));

            return item;
        } catch (Exception e) {
            System.err.println("Error converting meeting to extension item: " + e.getMessage());
            return null;
        }
    }

    private ExtensionItemResponse convertAnnouncementToExtensionItem(Map<String, Object> announcement) {
        try {
            ExtensionItemResponse item = new ExtensionItemResponse();

            item.setId((String) announcement.get("id"));
            item.setName((String) announcement.get("title"));
            item.setDescription((String) announcement.get("content"));
            item.setType("announcement");

            // Check scheduled and expiry dates to determine due date
            if (announcement.get("scheduledDate") != null) {
                item.setDueDate(convertToDateString(announcement.get("scheduledDate")));
            } else if (announcement.get("expiryDate") != null) {
                item.setDueDate(convertToDateString(announcement.get("expiryDate")));
            } else {
                item.setDueDate(LocalDate.now().toString());
            }

            // Set the course name if available, otherwise a general name
            String targetCourseId = (String) announcement.get("targetCourseId");
            item.setCourse(targetCourseId != null ? eduSphereClient.getCourseName(targetCourseId) : "General Announcement");

            String status = (String) announcement.getOrDefault("status", "pending");
            item.setStatus(status);

            // Calculate priority based on announcement priority field
            String priority = (String) announcement.getOrDefault("priority", "safe");
            item.setPriority(priority);

            item.setCategory("announcement");
            item.setAnnouncementType((String) announcement.get("targetAudienceType"));
            item.setIsImportant("high".equals(priority) || "urgent".equals(priority) || "active".equals(status));

            return item;
        } catch (Exception e) {
            System.err.println("Error converting announcement to extension item: " + e.getMessage());
            return null;
        }
    }

    // Helper method to convert various date formats to LocalDate string
    private String convertToDateString(Object dateObj) {
        if (dateObj == null) {
            return LocalDate.now().toString();
        }

        String dateStr = dateObj.toString();

        try {
            // Handle LocalDateTime format (2024-01-15T10:30:00)
            if (dateStr.contains("T")) {
                return LocalDateTime.parse(dateStr).toLocalDate().toString();
            }
            // Handle LocalDate format (2024-01-15)
            else if (dateStr.matches("\\d{4}-\\d{2}-\\d{2}")) {
                return LocalDate.parse(dateStr).toString();
            }
            // Handle other formats - try common patterns
            else {
                return LocalDate.now().toString();
            }
        } catch (Exception e) {
            System.err.println("Error parsing date: " + dateStr + ", using current date");
            return LocalDate.now().toString();
        }
    }

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