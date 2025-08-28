package com.example.extension.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Statistics response for extension - includes meetings count
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExtensionStatsResponse {
    private int totalItems;
    private int urgentItems;
    private int pendingItems;
    private int completedItems;
    private int overdueItems;
    private int tasksCount;
    private int meetingsCount; // Added for meetings support
    private int announcementsCount;

    // Additional stats
    private double completionRate;
    private int thisWeekDue;
    private int nextWeekDue;

    // Constructor without meetingsCount for backward compatibility
    public ExtensionStatsResponse(int totalItems, int urgentItems, int pendingItems,
                                  int completedItems, int overdueItems, int tasksCount,
                                  int announcementsCount, double completionRate,
                                  int thisWeekDue, int nextWeekDue) {
        this.totalItems = totalItems;
        this.urgentItems = urgentItems;
        this.pendingItems = pendingItems;
        this.completedItems = completedItems;
        this.overdueItems = overdueItems;
        this.tasksCount = tasksCount;
        this.meetingsCount = 0; // Default to 0 if not provided
        this.announcementsCount = announcementsCount;
        this.completionRate = completionRate;
        this.thisWeekDue = thisWeekDue;
        this.nextWeekDue = nextWeekDue;
    }

    // Utility method to get total content items (tasks + meetings, excluding announcements)
    public int getTotalContentItems() {
        return tasksCount + meetingsCount;
    }

    // Utility method to get academic completion rate (excluding announcements)
    public double getAcademicCompletionRate() {
        int academicItems = getTotalContentItems();
        return academicItems > 0 ? (completedItems * 100.0) / academicItems : 0.0;
    }

    // Check if user has heavy workload
    public boolean hasHeavyWorkload() {
        return urgentItems > 3 || (totalItems > 10 && urgentItems > 1);
    }

    // Get workload assessment
    public String getWorkloadAssessment() {
        if (urgentItems == 0 && totalItems <= 3) {
            return "Light";
        } else if (urgentItems <= 2 && totalItems <= 8) {
            return "Moderate";
        } else if (urgentItems <= 5 && totalItems <= 15) {
            return "Heavy";
        } else {
            return "Very Heavy";
        }
    }
}