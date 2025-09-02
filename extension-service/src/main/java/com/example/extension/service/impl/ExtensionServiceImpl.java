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
            List<Map<String, Object>> userCourses = eduSphereClient.getUserCourses(userId, userRole);
            List<String> courseIds = userCourses.stream()
                    .map(course -> (String) course.get("id"))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            if (courseIds.isEmpty()) {
                return new ExtensionDashboardResponse(new ArrayList<>(), new ExtensionStatsResponse());
            }

            List<ExtensionItemResponse> items = new ArrayList<>();

            List<Map<String, Object>> allTasks = eduSphereClient.getTasksByCourseIds(courseIds, userId);
            List<ExtensionItemResponse> taskItems = allTasks.stream()
                    .filter(task -> canUserSeeTask(task, userId, userRole))
                    .map(this::convertTaskToExtensionItem)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            items.addAll(taskItems);

            List<Map<String, Object>> allMeetings = eduSphereClient.getMeetingsByCourseIds(courseIds, userId);
            List<ExtensionItemResponse> meetingItems = allMeetings.stream()
                    .filter(meeting -> canUserSeeMeeting(meeting, userId, userRole))
                    .map(this::convertMeetingToExtensionItem)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            items.addAll(meetingItems);

            List<Map<String, Object>> allAnnouncements = eduSphereClient.getAnnouncementsForUser(userId);
            List<ExtensionItemResponse> announcementItems = allAnnouncements.stream()
                    .map(this::convertAnnouncementToExtensionItem)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            items.addAll(announcementItems);

            items = items.stream()
                    .sorted(this::compareItemsByPriority)
                    .collect(Collectors.toList());

            ExtensionStatsResponse stats = calculateStats(items);

            return new ExtensionDashboardResponse(items, stats);

        } catch (Exception e) {
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
            List<Map<String, Object>> userCourses = eduSphereClient.getUserCourses(userId, userRole);
            List<String> courseIds = userCourses.stream()
                    .map(course -> (String) course.get("id"))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            if (courseIds.isEmpty()) {
                return new ArrayList<>();
            }

            List<ExtensionItemResponse> items = new ArrayList<>();
            List<Map<String, Object>> allTasks = eduSphereClient.getTasksByCourseIds(courseIds, userId);
            List<ExtensionItemResponse> taskItems = allTasks.stream()
                    .filter(task -> canUserSeeTask(task, userId, userRole))
                    .map(this::convertTaskToExtensionItem)
                    .filter(Objects::nonNull)
                    .filter(item -> "task".equals(item.getType()))
                    .collect(Collectors.toList());
            items.addAll(taskItems);

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

            List<ExtensionItemResponse> filteredItems = items.stream()
                    .filter(item -> matchesStatusFilter(item, status))
                    .filter(item -> matchesPriorityFilter(item, priority))
                    .sorted(this::compareItemsByPriority)
                    .limit(limit)
                    .collect(Collectors.toList());

            return filteredItems;

        } catch (Exception e) {
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
            throw new RuntimeException("Failed to get announcements: " + e.getMessage());
        }
    }

    @Override
    public ExtensionStatsResponse getUserStats(String userId, String userRole) {
        try {
            ExtensionDashboardResponse dashboardData = getDashboardData(userId, userRole);
            return dashboardData.getStats();
        } catch (Exception e) {
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
            throw new RuntimeException("Failed to get urgent items: " + e.getMessage());
        }
    }

    @Override
    public boolean canUserAccessCourse(String userId, String userRole, String courseId) {
        return eduSphereClient.canUserAccessCourse(userId, userRole, courseId);
    }

    private boolean canUserSeeTask(Map<String, Object> task, String userId, String userRole) {
        try {
            if ("1100".equals(userRole)) return true;
            if ("1200".equals(userRole)) {
                String courseId = (String) task.get("courseId");
                return courseId != null && eduSphereClient.canUserAccessCourse(userId, userRole, courseId);
            }
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
            return false;
        }
    }

    private boolean canUserSeeMeeting(Map<String, Object> meeting, String userId, String userRole) {
        try {
            if ("1100".equals(userRole)) return true;
            if ("1200".equals(userRole)) {
                String courseId = (String) meeting.get("courseId");
                String createdBy = (String) meeting.get("createdBy");
                String lecturerId = (String) meeting.get("lecturerId");
                return (courseId != null && eduSphereClient.canUserAccessCourse(userId, userRole, courseId)) ||
                        userId.equals(createdBy) ||
                        userId.equals(lecturerId);
            }
            if ("1300".equals(userRole)) {
                String courseId = (String) meeting.get("courseId");
                @SuppressWarnings("unchecked")
                List<String> participants = (List<String>) meeting.get("participants");
                return (courseId != null && eduSphereClient.canUserAccessCourse(userId, userRole, courseId)) ||
                        (participants != null && participants.contains(userId));
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private ExtensionItemResponse convertTaskToExtensionItem(Map<String, Object> task) {
        try {
            ExtensionItemResponse item = new ExtensionItemResponse();
            item.setId((String) task.get("id"));
            item.setName((String) task.get("title"));
            item.setDescription((String) task.get("description"));
            item.setType("task");
            String dueDateStr = convertToDateString(task.get("dueDate"));
            item.setDueDate(dueDateStr);
            String courseId = (String) task.get("courseId");
            item.setCourse(eduSphereClient.getCourseName(courseId));
            String status = (String) task.getOrDefault("status", "pending");
            item.setStatus(status);
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
            item.setFileUrl((String) meeting.get("invitationLink"));
            return item;
        } catch (Exception e) {
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
            if (announcement.get("scheduledDate") != null) {
                item.setDueDate(convertToDateString(announcement.get("scheduledDate")));
            } else if (announcement.get("expiryDate") != null) {
                item.setDueDate(convertToDateString(announcement.get("expiryDate")));
            } else {
                item.setDueDate(LocalDate.now().toString());
            }
            String targetCourseId = (String) announcement.get("targetCourseId");
            item.setCourse(targetCourseId != null ? eduSphereClient.getCourseName(targetCourseId) : "General Announcement");
            String status = (String) announcement.getOrDefault("status", "pending");
            item.setStatus(status);
            String priority = (String) announcement.getOrDefault("priority", "safe");
            item.setPriority(priority);
            item.setCategory("announcement");
            item.setAnnouncementType((String) announcement.get("targetAudienceType"));
            item.setIsImportant("high".equals(priority) || "urgent".equals(priority) || "active".equals(status));
            return item;
        } catch (Exception e) {
            return null;
        }
    }

    private String convertToDateString(Object dateObj) {
        if (dateObj == null) {
            return LocalDate.now().toString();
        }
        String dateStr = dateObj.toString();
        try {
            if (dateStr.contains("T")) {
                return LocalDateTime.parse(dateStr).toLocalDate().toString();
            } else if (dateStr.matches("\\d{4}-\\d{2}-\\d{2}")) {
                return LocalDate.parse(dateStr).toString();
            } else {
                return LocalDate.now().toString();
            }
        } catch (Exception e) {
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

    private ExtensionStatsResponse calculateStats(List<ExtensionItemResponse> items) {
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