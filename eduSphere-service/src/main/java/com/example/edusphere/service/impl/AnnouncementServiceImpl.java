package com.example.edusphere.service.impl;

import com.example.common.exceptions.ResourceNotFoundException;
import com.example.edusphere.dto.request.AnnouncementRequest;
import com.example.edusphere.dto.response.AnnouncementResponse;
import com.example.edusphere.dto.response.CourseDto;
import com.example.edusphere.dto.response.DepartmentDto;
import com.example.edusphere.entity.*;
import com.example.edusphere.repository.AnnouncementRepository;
import com.example.edusphere.repository.CourseRepository;
import com.example.edusphere.repository.DepartmentRepository;
import com.example.common.repository.UserRepository;
import com.example.edusphere.service.AnnouncementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.HashSet;
import java.util.Set;

@Service
public class AnnouncementServiceImpl implements AnnouncementService {

    @Autowired
    private AnnouncementRepository announcementRepository;
    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private DepartmentRepository departmentRepository;
    @Autowired
    private UserRepository userRepository;

    @Override
    public List<AnnouncementResponse> getAnnouncementsForUser(String userId, String userRole) {
        Set<Announcement> announcementsSet = new HashSet<>();

        if ("1100".equals(userRole)) { // Admin
            announcementsSet.addAll(announcementRepository.findAll());
        } else if ("1200".equals(userRole)) { // Lecturer
            announcementsSet.addAll(announcementRepository.findByCreatorIdOrderByCreatedAtDesc(userId));

            List<String> lecturerCourses = courseRepository.findByLecturerId(userId).stream()
                    .map(Course::getId)
                    .collect(Collectors.toList());
            announcementsSet.addAll(announcementRepository.findByTargetAudienceTypeAndTargetCourseIdIn("course", lecturerCourses));

            announcementsSet.addAll(announcementRepository.findByTargetAudienceType("all"));
            announcementsSet.addAll(announcementRepository.findByTargetAudienceType("lecturer"));
            announcementsSet.addAll(announcementRepository.findByTargetUserId(userId));
        } else if ("1300".equals(userRole)) { // Student
            List<Course> enrolledCourses = courseRepository.findByEnrollments_StudentIds(userId);

            announcementsSet.addAll(announcementRepository.findByTargetAudienceType("all"));
            announcementsSet.addAll(announcementRepository.findByTargetAudienceType("student"));

            List<String> enrolledCourseIds = enrolledCourses.stream().map(Course::getId).collect(Collectors.toList());
            if (!enrolledCourseIds.isEmpty()) {
                announcementsSet.addAll(announcementRepository.findByTargetAudienceTypeAndTargetCourseIdIn("course", enrolledCourseIds));
            }

            announcementsSet.addAll(announcementRepository.findByTargetUserId(userId));
        }

        return announcementsSet.stream()
                .map(this::mapToResponse)
                .sorted((a1, a2) -> a2.getCreatedAt().compareTo(a1.getCreatedAt()))
                .collect(Collectors.toList());
    }

    @Override
    public List<AnnouncementResponse> getMyAnnouncements(String userId) {
        List<Announcement> announcements = announcementRepository.findByCreatorIdOrderByCreatedAtDesc(userId);
        return announcements.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public AnnouncementResponse getAnnouncementById(String announcementId) {
        Announcement announcement = announcementRepository.findById(announcementId)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement not found with id " + announcementId));
        return mapToResponse(announcement);
    }

    @Override
    public AnnouncementResponse createAnnouncement(AnnouncementRequest announcementRequest, String creatorId, String creatorName) {
        Announcement announcement = new Announcement();
        announcement.setTitle(announcementRequest.getTitle());
        announcement.setContent(announcementRequest.getContent());
        announcement.setPriority(announcementRequest.getPriority());
        announcement.setStatus(announcementRequest.getScheduledDate() != null ? "scheduled" : "active");
        announcement.setCreatorId(creatorId);
        announcement.setCreatorName(creatorName);
        announcement.setExpiryDate(announcementRequest.getExpiryDate());
        announcement.setScheduledDate(announcementRequest.getScheduledDate());
        announcement.setTargetAudienceType(announcementRequest.getTargetAudienceType());
        announcement.setTargetDepartment(announcementRequest.getTargetDepartment());
        announcement.setTargetCourseId(announcementRequest.getTargetCourseId());
        announcement.setTargetAcademicYear(announcementRequest.getTargetAcademicYear());
        announcement.setTargetUserId(announcementRequest.getTargetUserId());

        // Explicitly set timestamps to ensure they're not null
        LocalDateTime now = LocalDateTime.now();
        announcement.setCreatedAt(now);
        announcement.setUpdatedAt(now);

        Announcement savedAnnouncement = announcementRepository.save(announcement);
        return mapToResponse(savedAnnouncement);
    }

    @Override
    public AnnouncementResponse updateAnnouncement(String announcementId, AnnouncementRequest announcementRequest, String updaterId, String userRole) {
        Announcement existingAnnouncement = announcementRepository.findById(announcementId)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement not found with id " + announcementId));

        if (!"1100".equals(userRole) && !existingAnnouncement.getCreatorId().equals(updaterId)) {
            throw new AccessDeniedException("You do not have permission to update this announcement.");
        }

        existingAnnouncement.setTitle(announcementRequest.getTitle());
        existingAnnouncement.setContent(announcementRequest.getContent());
        existingAnnouncement.setPriority(announcementRequest.getPriority());
        existingAnnouncement.setStatus(announcementRequest.getScheduledDate() != null ? "scheduled" : "active");
        existingAnnouncement.setExpiryDate(announcementRequest.getExpiryDate());
        existingAnnouncement.setScheduledDate(announcementRequest.getScheduledDate());
        existingAnnouncement.setTargetAudienceType(announcementRequest.getTargetAudienceType());
        existingAnnouncement.setTargetDepartment(announcementRequest.getTargetDepartment());
        existingAnnouncement.setTargetCourseId(announcementRequest.getTargetCourseId());
        existingAnnouncement.setTargetAcademicYear(announcementRequest.getTargetAcademicYear());
        existingAnnouncement.setTargetUserId(announcementRequest.getTargetUserId());
        existingAnnouncement.setUpdatedAt(LocalDateTime.now());

        Announcement updatedAnnouncement = announcementRepository.save(existingAnnouncement);
        return mapToResponse(updatedAnnouncement);
    }

    @Override
    public AnnouncementResponse duplicateAnnouncement(String announcementId, AnnouncementRequest newAnnouncementRequest, String creatorId, String creatorName, String userRole) {
        Announcement existingAnnouncement = announcementRepository.findById(announcementId)
                .orElseThrow(() -> new ResourceNotFoundException("Original announcement not found with id " + announcementId));

        if (!"1100".equals(userRole) && !existingAnnouncement.getCreatorId().equals(creatorId)) {
            throw new AccessDeniedException("You do not have permission to use this announcement as a template.");
        }

        Announcement newAnnouncement = new Announcement();

        newAnnouncement.setTitle(newAnnouncementRequest.getTitle());
        newAnnouncement.setContent(newAnnouncementRequest.getContent());
        newAnnouncement.setPriority(newAnnouncementRequest.getPriority());
        newAnnouncement.setStatus(newAnnouncementRequest.getScheduledDate() != null ? "scheduled" : "active");
        newAnnouncement.setExpiryDate(newAnnouncementRequest.getExpiryDate());
        newAnnouncement.setScheduledDate(newAnnouncementRequest.getScheduledDate());
        newAnnouncement.setTargetAudienceType(newAnnouncementRequest.getTargetAudienceType());
        newAnnouncement.setTargetDepartment(newAnnouncementRequest.getTargetDepartment());
        newAnnouncement.setTargetCourseId(newAnnouncementRequest.getTargetCourseId());
        newAnnouncement.setTargetAcademicYear(newAnnouncementRequest.getTargetAcademicYear());
        newAnnouncement.setTargetUserId(newAnnouncementRequest.getTargetUserId());

        newAnnouncement.setCreatorId(creatorId);
        newAnnouncement.setCreatorName(creatorName);

        // Explicitly set timestamps
        LocalDateTime now = LocalDateTime.now();
        newAnnouncement.setCreatedAt(now);
        newAnnouncement.setUpdatedAt(now);

        Announcement savedAnnouncement = announcementRepository.save(newAnnouncement);
        return mapToResponse(savedAnnouncement);
    }

    @Override
    public void deleteAnnouncement(String announcementId, String deleterId, String userRole) {
        Announcement announcement = announcementRepository.findById(announcementId)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement not found with id " + announcementId));

        if (!"1100".equals(userRole) && !announcement.getCreatorId().equals(deleterId)) {
            throw new AccessDeniedException("You do not have permission to delete this announcement.");
        }

        announcementRepository.delete(announcement);
    }

    @Override
    public List<DepartmentDto> getDepartmentsForUser(String userId, String userRole) {
        if ("1100".equals(userRole)) { // Admin
            List<Department> departments = departmentRepository.findAll();
            return departments.stream().map(d -> new DepartmentDto(d.getName())).collect(Collectors.toList());
        } else if ("1200".equals(userRole)) { // Lecturer
            List<String> departmentNames = courseRepository.findByLecturerId(userId).stream()
                    .map(Course::getDepartment)
                    .distinct()
                    .collect(Collectors.toList());
            return departmentNames.stream().map(DepartmentDto::new).collect(Collectors.toList());
        }
        return List.of();
    }

    @Override
    public List<CourseDto> getCoursesByDepartmentForUser(String userId, String userRole, String departmentName) {
        if ("1100".equals(userRole)) { // Admin
            List<Course> courses = courseRepository.findByDepartment(departmentName);
            return courses.stream().map(this::mapToCourseDto).collect(Collectors.toList());
        } else if ("1200".equals(userRole)) { // Lecturer
            List<Course> courses = courseRepository.findByLecturerIdAndDepartment(userId, departmentName);
            return courses.stream().map(this::mapToCourseDto).collect(Collectors.toList());
        }
        return List.of();
    }

    /**
     * Maps an Announcement entity to an AnnouncementResponse DTO using explicit setters.
     * This is more robust against changes in field order and better handles potential null values.
     * @param announcement The Announcement entity to map.
     * @return A new AnnouncementResponse DTO.
     */
    private AnnouncementResponse mapToResponse(Announcement announcement) {
        AnnouncementResponse response = new AnnouncementResponse();
        response.setId(announcement.getId());
        response.setTitle(announcement.getTitle());
        response.setContent(announcement.getContent());
        response.setCreatorId(announcement.getCreatorId());
        response.setCreatorName(announcement.getCreatorName());
        response.setPriority(announcement.getPriority());
        response.setStatus(announcement.getStatus());

        // Handle potential null timestamps gracefully
        response.setCreatedAt(announcement.getCreatedAt());
        response.setUpdatedAt(announcement.getUpdatedAt());
        response.setExpiryDate(announcement.getExpiryDate());
        response.setScheduledDate(announcement.getScheduledDate());

        response.setTargetAudienceType(announcement.getTargetAudienceType());
        response.setTargetDepartment(announcement.getTargetDepartment());
        response.setTargetCourseId(announcement.getTargetCourseId());
        response.setTargetAcademicYear(announcement.getTargetAcademicYear());
        response.setTargetUserId(announcement.getTargetUserId());
        return response;
    }

    private CourseDto mapToCourseDto(Course course) {
        return new CourseDto(course.getId(), course.getName(), course.getCode());
    }
}