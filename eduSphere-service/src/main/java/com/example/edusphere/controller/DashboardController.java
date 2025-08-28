package com.example.edusphere.controller;

import com.example.edusphere.dto.request.AssignmentRequestDto;
import com.example.edusphere.dto.response.AssignmentResponseDto;
import com.example.edusphere.dto.response.DashboardDataResponseDto;
import com.example.edusphere.service.DashboardService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/complete/{userRole}")
    public ResponseEntity<DashboardDataResponseDto> getDashboardData(@PathVariable String userRole) {
        DashboardDataResponseDto data = dashboardService.getDashboardDataForRole(userRole);
        return ResponseEntity.ok(data);
    }

    @PostMapping("/assignments")
    public ResponseEntity<AssignmentResponseDto> createAssignment(@RequestBody AssignmentRequestDto assignmentRequestDto) {
        AssignmentResponseDto newAssignment = dashboardService.createAssignment(assignmentRequestDto);
        return new ResponseEntity<>(newAssignment, HttpStatus.CREATED);
    }

    @PutMapping("/assignments/{id}")
    public ResponseEntity<AssignmentResponseDto> updateAssignment(
            @PathVariable String id,
            @RequestBody AssignmentRequestDto assignmentRequestDto) {
        AssignmentResponseDto updatedAssignment = dashboardService.updateAssignment(id, assignmentRequestDto);
        return ResponseEntity.ok(updatedAssignment);
    }

    /**
     * Endpoint to delete an existing assignment.
     * This is mapped to a DELETE request at the URL: /api/dashboard/assignments/{id}
     *
     * @param id The ID of the assignment to delete, taken from the URL path.
     * @return A response with no content and a "No Content" (204) HTTP status.
     */
    @DeleteMapping("/assignments/{id}")
    public ResponseEntity<Void> deleteAssignment(@PathVariable String id) {
        // Delegate the deletion logic to the service layer
        dashboardService.deleteAssignment(id);
        // Return a 204 No Content status, which is the standard for successful deletions.
        return ResponseEntity.noContent().build();
    }
}
