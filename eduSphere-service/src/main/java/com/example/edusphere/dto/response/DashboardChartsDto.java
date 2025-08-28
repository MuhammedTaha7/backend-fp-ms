package com.example.edusphere.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * DTO for holding the data for all charts on the dashboard.
 * Each field corresponds to a specific chart.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardChartsDto {

    /**
     * Data for the "Department Enrollment Statistics" bar chart.
     * This will likely be a list of objects, where each object has a department name and a count.
     * Example: [{"name": "Computer Science", "value": 150}, {"name": "Physics", "value": 80}]
     */
    private List<Map<String, Object>> departmentEnrollment;

    /**
     * Data for the "System Usage Distribution" pie chart.
     * This will be a list of objects, where each object has a category and a percentage/value.
     * Example: [{"name": "Students", "value": 70}, {"name": "Lecturers", "value": 20}]
     */
    private List<Map<String, Object>> systemUsage;

    /**
     * Data for the "Annual Enrollment Trends" line chart.
     * This will be a list of objects, where each object represents a data point over time.
     * Example: [{"year": "2023", "enrollment": 300}, {"year": "2024", "enrollment": 350}]
     */
    private List<Map<String, Object>> annualEnrollment;

}