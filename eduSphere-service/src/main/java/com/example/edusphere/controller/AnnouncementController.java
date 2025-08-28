package com.example.edusphere.controller;

import com.example.edusphere.dto.request.AnnouncementRequest;
import com.example.edusphere.dto.response.AnnouncementResponse;
import com.example.edusphere.dto.response.CourseDto;
import com.example.edusphere.dto.response.DepartmentDto;
import com.example.common.entity.UserEntity;
import com.example.edusphere.service.AnnouncementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public ResponseEntity<AnnouncementResponse> createAnnouncement(@RequestBody AnnouncementRequest request, @AuthenticationPrincipal UserDetails userDetails) {
        UserEntity currentUser = (UserEntity) userDetails;
        AnnouncementResponse createdAnnouncement = announcementService.createAnnouncement(request, currentUser.getId(), currentUser.getName());
        return ResponseEntity.ok(createdAnnouncement);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_LECTURER')")
    public ResponseEntity<AnnouncementResponse> updateAnnouncement(@PathVariable String id, @RequestBody AnnouncementRequest request, @AuthenticationPrincipal UserDetails userDetails) {
        UserEntity currentUser = (UserEntity) userDetails;
        AnnouncementResponse updatedAnnouncement = announcementService.updateAnnouncement(id, request, currentUser.getId(), currentUser.getRole());
        return ResponseEntity.ok(updatedAnnouncement);
    }

    // 🆕 NEW: Endpoint for duplicating and re-sending an announcement
    @PostMapping("/{id}/duplicate")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_LECTURER')")
    public ResponseEntity<AnnouncementResponse> duplicateAnnouncement(
            @PathVariable String id,
            @RequestBody AnnouncementRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        UserEntity currentUser = (UserEntity) userDetails;
        // Pass currentUser.getRole() to the service method
        AnnouncementResponse newAnnouncement = announcementService.duplicateAnnouncement(id, request, currentUser.getId(), currentUser.getName(), currentUser.getRole());
        return ResponseEntity.ok(newAnnouncement);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_LECTURER')")
    public ResponseEntity<Void> deleteAnnouncement(@PathVariable String id, @AuthenticationPrincipal UserDetails userDetails) {
        UserEntity currentUser = (UserEntity) userDetails;
        announcementService.deleteAnnouncement(id, currentUser.getId(), currentUser.getRole());
        return ResponseEntity.noContent().build();
    }
}