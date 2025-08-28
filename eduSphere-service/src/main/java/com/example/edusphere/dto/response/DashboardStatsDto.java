package com.example.edusphere.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for holding the summary statistics for the dashboard's top cards.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDto {

    /**
     * The total number of active users in the system.
     * Maps to the "User Management" card.
     */
    private long activeUsers;

    /**
     * A string describing the system's current status or performance.
     * Maps to the "System Analytics" card.
     */
    private String systemHealth;

    /**
     * The total number of active departments.
     * Maps to the "Institution Overview" card.
     */
    private long activeDepartments;

}