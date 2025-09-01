// AnnouncementController.java
package com.example.edusphere.controller;

import com.example.edusphere.dto.request.AnnouncementRequest;
import com.example.edusphere.dto.response.AnnouncementResponse;
import com.example.edusphere.dto.response.CourseDto;
import com.example.edusphere.dto.response.DepartmentDto;
import com.example.common.entity.UserEntity;
import com.example.edusphere.service.AnnouncementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/announcements")
public class AnnouncementController {

    @Autowired
    private AnnouncementService announcementService;

    // --- Endpoints for all authenticated users ---

    @GetMapping
    public ResponseEntity<List<AnnouncementResponse>> getAnnouncementsForUser(@AuthenticationPrincipal UserDetails userDetails) {
        UserEntity currentUser = (UserEntity) userDetails;
        List<AnnouncementResponse> announcements = announcementService.getAnnouncementsForUser(currentUser.getId(), currentUser.getRole());
        return ResponseEntity.ok(announcements);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AnnouncementResponse> getAnnouncementById(@PathVariable String id) {
        AnnouncementResponse announcement = announcementService.getAnnouncementById(id);
        return ResponseEntity.ok(announcement);
    }

    // --- Endpoints for Admins and Lecturers ---

    @GetMapping("/my-announcements")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_LECTURER')")
    public ResponseEntity<List<AnnouncementResponse>> getMyAnnouncements(@AuthenticationPrincipal UserDetails userDetails) {
        UserEntity currentUser = (UserEntity) userDetails;
        List<AnnouncementResponse> announcements = announcementService.getMyAnnouncements(currentUser.getId());
        return ResponseEntity.ok(announcements);
    }

    @GetMapping("/departments")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_LECTURER')")
    public ResponseEntity<List<DepartmentDto>> getDepartmentsForUser(@AuthenticationPrincipal UserDetails userDetails) {
        UserEntity currentUser = (UserEntity) userDetails;
        List<DepartmentDto> departments = announcementService.getDepartmentsForUser(currentUser.getId(), currentUser.getRole());
        return ResponseEntity.ok(departments);
    }

    @GetMapping("/courses/{departmentName}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_LECTURER')")
    public ResponseEntity<List<CourseDto>> getCoursesByDepartmentForUser(@PathVariable String departmentName, @AuthenticationPrincipal UserDetails userDetails) {
        UserEntity currentUser = (UserEntity) userDetails;
        List<CourseDto> courses = announcementService.getCoursesByDepartmentForUser(currentUser.getId(), currentUser.getRole(), departmentName);
        return ResponseEntity.ok(courses);
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_LECTURER')")
    public ResponseEntity<Map<String, Object>> createAnnouncement(@RequestBody AnnouncementRequest request, @AuthenticationPrincipal UserDetails userDetails) {
        UserEntity currentUser = (UserEntity) userDetails;
        AnnouncementResponse createdAnnouncement = announcementService.createAnnouncement(request, currentUser.getId(), currentUser.getName());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Announcement created successfully");
        response.put("announcement", createdAnnouncement);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_LECTURER')")
    public ResponseEntity<Map<String, Object>> updateAnnouncement(@PathVariable String id, @RequestBody AnnouncementRequest request, @AuthenticationPrincipal UserDetails userDetails) {
        UserEntity currentUser = (UserEntity) userDetails;
        AnnouncementResponse updatedAnnouncement = announcementService.updateAnnouncement(id, request, currentUser.getId(), currentUser.getRole());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Announcement updated successfully");
        response.put("announcement", updatedAnnouncement);

        return ResponseEntity.ok(response);
    }

    // 🆕 NEW: Endpoint for duplicating and re-sending an announcement
    @PostMapping("/{id}/duplicate")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_LECTURER')")
    public ResponseEntity<Map<String, Object>> duplicateAnnouncement(
            @PathVariable String id,
            @RequestBody AnnouncementRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        UserEntity currentUser = (UserEntity) userDetails;
        // Pass currentUser.getRole() to the service method
        AnnouncementResponse newAnnouncement = announcementService.duplicateAnnouncement(id, request, currentUser.getId(), currentUser.getName(), currentUser.getRole());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Announcement duplicated and sent successfully");
        response.put("announcement", newAnnouncement);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_LECTURER')")
    public ResponseEntity<Void> deleteAnnouncement(@PathVariable String id, @AuthenticationPrincipal UserDetails userDetails) {
        UserEntity currentUser = (UserEntity) userDetails;
        announcementService.deleteAnnouncement(id, currentUser.getId(), currentUser.getRole());
        return ResponseEntity.noContent().build();
    }
}