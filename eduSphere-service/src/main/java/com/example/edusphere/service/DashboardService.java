package com.example.edusphere.service;

import com.example.edusphere.dto.request.AssignmentRequestDto;
import com.example.edusphere.dto.response.AssignmentResponseDto;
import com.example.edusphere.dto.response.DashboardDataResponseDto;

/**
 * Service interface for handling business logic related to the dashboard.
 * This defines the contract for our dashboard operations.
 */
public interface DashboardService {

    /**
     * Gathers and returns all the necessary data for the dashboard based on the user's role.
     *
     * @param userRole The role of the user (e.g., "1100" for admin).
     * @return A DTO containing all the data required to populate the dashboard.
     */
    DashboardDataResponseDto getDashboardDataForRole(String userRole);

    /**
     * Creates a new assignment based on the data received from the frontend.
     *
     * @param assignmentRequestDto The DTO containing the new assignment's data.
     * @return A DTO representing the newly created assignment, including its generated ID.
     */
    AssignmentResponseDto createAssignment(AssignmentRequestDto assignmentRequestDto);

    /**
     * Updates an existing assignment with new data.
     *
     * @param id The ID of the assignment to update.
     * @param assignmentRequestDto The DTO containing the updated assignment data.
     * @return A DTO representing the updated assignment.
     */
    AssignmentResponseDto updateAssignment(String id, AssignmentRequestDto assignmentRequestDto);

    /**
     * Deletes an assignment from the database.
     *
     * @param id The ID of the assignment to delete.
     */
    void deleteAssignment(String id);

}
