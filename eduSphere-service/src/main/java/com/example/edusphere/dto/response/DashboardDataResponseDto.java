package com.example.edusphere.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO (Data Transfer Object) for sending all dashboard data to the frontend.
 * This acts as a container for all the different pieces of information the dashboard needs.
 */
@Data // A Lombok annotation that creates getters, setters, toString, etc.
@NoArgsConstructor // Creates a no-argument constructor
@AllArgsConstructor // Creates a constructor with all arguments
public class DashboardDataResponseDto {

    /**
     * Holds the summary statistics for the top cards on the dashboard.
     * e.g., user count, department count.
     */
    private DashboardStatsDto stats;

    /**
     * Holds the data for all the charts on the dashboard.
     */
    private DashboardChartsDto charts;

    /**
     * Holds a list of upcoming assignments.
     */
    private List<AssignmentResponseDto> assignments;

}