package com.example.edusphere.service;

import com.example.edusphere.dto.request.CourseRequestDto;
import com.example.edusphere.dto.request.EnrollmentRequest;
import com.example.edusphere.dto.request.EnrollmentRequestDto;
import com.example.edusphere.dto.request.UnenrollmentRequest;
import com.example.edusphere.dto.response.CourseDetailsResponse;
import com.example.edusphere.dto.response.CourseResponseDto;
import com.example.edusphere.dto.response.CourseSummaryResponse;
import com.example.edusphere.dto.response.EnrollmentResponseDto;
import com.example.edusphere.entity.Course;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.util.Optional;

public interface CourseService {
    List<CourseSummaryResponse> findAllCoursesForUser(UserDetails userDetails);

    // New methods for inter-service communication
    List<Course> getUserCourses(String userId, String userRole);
    boolean canUserAccessCourse(String userId, String userRole, String courseId);
    String getCourseName(String courseId);
    Optional<Course> findById(String id);

    CourseDetailsResponse findCourseDetailsById(String id);
    Course createCourse(Course course);
    Course updateCourse(String id, Course courseDetails);
    void deleteCourse(String id);
    Course enrollStudent(String courseId, EnrollmentRequest enrollmentRequest);
    Course unenrollStudents(String courseId, List<String> studentIds);
    List<EnrollmentResponseDto> getStudentEnrollments(String studentId);
    EnrollmentResponseDto addStudentEnrollment(EnrollmentRequestDto enrollmentDto);
    EnrollmentResponseDto updateStudentEnrollment(String courseId, EnrollmentRequestDto enrollmentDto);
    List<CourseResponseDto> getLecturerCourses(String lecturerId);
    CourseResponseDto assignCourseToLecturer(CourseRequestDto courseDto);
    CourseResponseDto updateLecturerCourse(String courseId, CourseRequestDto courseDto);
    void unassignCourseFromLecturer(String courseId, String lecturerId);
}