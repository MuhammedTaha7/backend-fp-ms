package com.example.extension.service;

import com.example.extension.dto.response.ExtensionDashboardResponse;
import com.example.extension.dto.response.ExtensionItemResponse;
import com.example.extension.dto.response.ExtensionStatsResponse;

import java.util.List;
import java.util.Map;

public interface ExtensionService {

    /**
     * Get dashboard data for extension
     */
    ExtensionDashboardResponse getDashboardData(String userId, String userRole);

    /**
     * Get tasks with filtering
     */
    List<ExtensionItemResponse> getTasks(String userId, String userRole, String status,
                                         String priority, String type, int limit);

    /**
     * Get announcements
     */
    List<ExtensionItemResponse> getAnnouncements(String userId, String userRole, int limit);

    /**
     * Get user statistics
     */
    ExtensionStatsResponse getUserStats(String userId, String userRole);

    /**
     * Get urgent items
     */
    List<ExtensionItemResponse> getUrgentItems(String userId, String userRole);

    /**
     * Check if user can access course
     */
    boolean canUserAccessCourse(String userId, String userRole, String courseId);

    /**
     * Get specific meeting details via API call to edusphere-service
     */
    Map<String, Object> getMeetingDetails(String meetingId, String email);
}