package com.example.edusphere.controller;

import com.example.edusphere.dto.response.LecturerStatsDto;
import com.example.edusphere.dto.response.StudentStatsDto;
import com.example.edusphere.entity.StudentGrade;
import com.example.edusphere.service.AnalyticsService;
import com.example.edusphere.service.impl.ProfileServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "false")
public class ProfileAnalyticsController {

    private final AnalyticsService analyticsService;
    private final ProfileServiceImpl profileService;

    @GetMapping("/profile-analytics/{entityType}/{id}/stats")
    @PreAuthorize("hasRole('ADMIN') or #entityType.equals('student') and #id.equals(authentication.name) or #entityType.equals('lecturer') and #id.equals(authentication.name)")
    public ResponseEntity<?> getStats(@PathVariable String entityType, @PathVariable String id) {
        if ("student".equalsIgnoreCase(entityType)) {
            StudentStatsDto stats = analyticsService.getStudentStats(id);
            return ResponseEntity.ok(stats);
        } else if ("lecturer".equalsIgnoreCase(entityType)) {
            LecturerStatsDto stats = analyticsService.getLecturerStats(id);
            return ResponseEntity.ok(stats);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid entity type provided.");
        }
    }

    //  Add grades endpoints that your frontend is expecting
    @GetMapping("/grades/by-student/{studentId}")
    @PreAuthorize("hasRole('ADMIN') or #studentId.equals(authentication.name)")
    public ResponseEntity<List<StudentGrade>> getStudentGrades(@PathVariable String studentId) {
        try {
            log.info("Fetching grades for student: {}", studentId);
            List<StudentGrade> grades = profileService.getStudentGrades(studentId);
            log.info("Found {} grades for student: {}", grades.size(), studentId);
            return ResponseEntity.ok(grades);
        } catch (Exception e) {
            log.error("Error fetching grades for student {}: {}", studentId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error fetching student grades");
        }
    }

    @GetMapping("/grades/by-lecturer/{lecturerId}")
    @PreAuthorize("hasRole('ADMIN') or #lecturerId.equals(authentication.name)")
    public ResponseEntity<List<StudentGrade>> getLecturerGrades(@PathVariable String lecturerId) {
        try {
            log.info("Fetching grades for lecturer: {}", lecturerId);
            List<StudentGrade> grades = profileService.getLecturerGrades(lecturerId);
            log.info("Found {} grades for lecturer: {}", grades.size(), lecturerId);
            return ResponseEntity.ok(grades);
        } catch (Exception e) {
            log.error("Error fetching grades for lecturer {}: {}", lecturerId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error fetching lecturer grades");
        }
    }

    //  Optional: Add detailed grades endpoints with course information
    @GetMapping("/grades/by-student/{studentId}/detailed")
    @PreAuthorize("hasRole('ADMIN') or #studentId.equals(authentication.name)")
    public ResponseEntity<List<ProfileServiceImpl.StudentGradeWithCourseInfo>> getStudentGradesDetailed(@PathVariable String studentId) {
        try {
            log.info("Fetching detailed grades for student: {}", studentId);
            List<ProfileServiceImpl.StudentGradeWithCourseInfo> grades = profileService.getStudentGradesWithCourseInfo(studentId);
            log.info("Found {} detailed grades for student: {}", grades.size(), studentId);
            return ResponseEntity.ok(grades);
        } catch (Exception e) {
            log.error("Error fetching detailed grades for student {}: {}", studentId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error fetching detailed student grades");
        }
    }

    @GetMapping("/grades/by-lecturer/{lecturerId}/detailed")
    @PreAuthorize("hasRole('ADMIN') or #lecturerId.equals(authentication.name)")
    public ResponseEntity<List<ProfileServiceImpl.LecturerGradeWithCourseInfo>> getLecturerGradesDetailed(@PathVariable String lecturerId) {
        try {
            log.info("Fetching detailed grades for lecturer: {}", lecturerId);
            List<ProfileServiceImpl.LecturerGradeWithCourseInfo> grades = profileService.getLecturerGradesWithCourseInfo(lecturerId);
            log.info("Found {} detailed grades for lecturer: {}", grades.size(), lecturerId);
            return ResponseEntity.ok(grades);
        } catch (Exception e) {
            log.error("Error fetching detailed grades for lecturer {}: {}", lecturerId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error fetching detailed lecturer grades");
        }
    }
}