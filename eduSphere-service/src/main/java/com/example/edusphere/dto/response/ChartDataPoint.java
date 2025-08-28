package com.example.edusphere.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ChartDataPoint {
    private String name;  // Generic name for the X-axis label (e.g., "Assignment 1")
    private Long value; // Generic name for the Y-axis value (e.g., 25)
}