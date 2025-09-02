package com.example.edusphere.controller;

import com.example.edusphere.dto.request.CourseRequestDto;
import com.example.edusphere.dto.request.EnrollmentRequest;
import com.example.edusphere.dto.request.EnrollmentRequestDto;
import com.example.edusphere.dto.request.UnenrollmentRequest;
import com.example.edusphere.dto.response.CourseDetailsResponse;
import com.example.edusphere.dto.response.CourseResponseDto;
import com.example.edusphere.dto.response.CourseSummaryResponse;
import com.example.edusphere.dto.response.EnrollmentResponseDto;
import com.example.edusphere.entity.Course;
import com.example.edusphere.service.CourseService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/courses")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "false")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    /**
     * GET /api/courses : Gets a list of courses based on the logged-in user's role.
     */
    @GetMapping
    public List<CourseSummaryResponse> getAllCourses(@AuthenticationPrincipal UserDetails userDetails) {
        return courseService.findAllCoursesForUser(userDetails);
    }

    /**
     * GET /api/courses/user-courses : Gets a list of courses for a specific user, used for inter-service communication.
     */
    @GetMapping("/user-courses")
    public List<Course> getUserCourses(@RequestParam String userId, @RequestParam String userRole) {
        return courseService.getUserCourses(userId, userRole);
    }

    /**
     * GET /api/courses/can-access : Checks if a user has access to a course, used for inter-service communication.
     */
    @GetMapping("/can-access")
    public boolean canUserAccessCourse(@RequestParam String userId, @RequestParam String userRole, @RequestParam String courseId) {
        return courseService.canUserAccessCourse(userId, userRole, courseId);
    }

    /**
     * GET /api/courses/name/{courseId} : Gets the name of a course by its ID, used for inter-service communication.
     */
    @GetMapping("/name/{courseId}")
    public String getCourseName(@PathVariable String courseId) {
        return courseService.getCourseName(courseId);
    }

    /**
     * GET /api/courses/by-id/{id} : Get a single course by its ID, used for inter-service communication.
     */
    @GetMapping("/by-id/{id}")
    public ResponseEntity<Optional<Course>> getCourseByIdForService(@PathVariable String id) {
        try {
            Optional<Course> course = courseService.findById(id);
            if (course.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(course);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/courses/{id} : Get a single course by its ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<CourseDetailsResponse> getCourseById(@PathVariable String id) {
        try {
            CourseDetailsResponse courseDetails = courseService.findCourseDetailsById(id);
            return ResponseEntity.ok(courseDetails);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * POST /api/courses : Create a new course.
     */
    @PostMapping
    public ResponseEntity<Course> createCourse(@RequestBody Course course) {
        Course createdCourse = courseService.createCourse(course);
        return new ResponseEntity<>(createdCourse, HttpStatus.CREATED);
    }

    /**
     * PUT /api/courses/{id} : Update an existing course.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Course> updateCourse(@PathVariable String id, @RequestBody Course courseDetails) {
        try {
            Course updatedCourse = courseService.updateCourse(id, courseDetails);
            return ResponseEntity.ok(updatedCourse);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * DELETE /api/courses/{id} : Delete a course by its ID.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCourse(@PathVariable String id) {
        try {
            courseService.deleteCourse(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * POST /{courseId}/enroll : Enroll a student into a course for a specific year.
     */
    @PostMapping("/{courseId}/enroll")
    public ResponseEntity<Course> enrollStudent(@PathVariable String courseId, @RequestBody EnrollmentRequest enrollmentRequest) {
        try {
            Course updatedCourse = courseService.enrollStudent(courseId, enrollmentRequest);
            return ResponseEntity.ok(updatedCourse);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * DELETE /api/courses/{courseId}/enrollments : Unenroll one or more students from a course.
     */
    @DeleteMapping("/{courseId}/enrollments")
    public ResponseEntity<Course> unenrollStudents(
            @PathVariable String courseId,
            @RequestBody UnenrollmentRequest request) {
        try {
            if (request == null || request.getStudentIds() == null || request.getStudentIds().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            Course updatedCourse = courseService.unenrollStudents(courseId, request.getStudentIds());
            return ResponseEntity.ok(updatedCourse);

        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/student-enrollments/{studentId}")
    public ResponseEntity<List<EnrollmentResponseDto>> getStudentEnrollments(@PathVariable String studentId) {
        List<EnrollmentResponseDto> enrollments = courseService.getStudentEnrollments(studentId);
        return ResponseEntity.ok(enrollments);
    }

    /**
     * POST /api/enrollments : Add a new student enrollment
     */
    @PostMapping("/enrollments")
    public ResponseEntity<EnrollmentResponseDto> addStudentEnrollment(@RequestBody EnrollmentRequestDto enrollmentDto) {
        EnrollmentResponseDto newEnrollment = courseService.addStudentEnrollment(enrollmentDto);
        return new ResponseEntity<>(newEnrollment, HttpStatus.CREATED);
    }

    /**
     * PUT /api/enrollments/{courseId} : Update an existing student enrollment status
     */
    @PutMapping("/enrollments/{courseId}")
    public ResponseEntity<EnrollmentResponseDto> updateStudentEnrollment(@PathVariable String courseId, @RequestBody EnrollmentRequestDto enrollmentDto) {
        EnrollmentResponseDto updatedEnrollment = courseService.updateStudentEnrollment(courseId, enrollmentDto);
        return ResponseEntity.ok(updatedEnrollment);
    }

    //  Endpoints for Lecturer Profile Courses
    @GetMapping("/by-lecturer/{lecturerId}")
    @PreAuthorize("hasRole('LECTURER') or hasRole('ADMIN')")
    public ResponseEntity<List<CourseResponseDto>> getCoursesByLecturer(@PathVariable String lecturerId) {
        List<CourseResponseDto> courses = courseService.getLecturerCourses(lecturerId);
        return ResponseEntity.ok(courses);
    }

    @PostMapping("/assign")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CourseResponseDto> assignCourseToLecturer(@RequestBody CourseRequestDto courseDto) {
        CourseResponseDto assignedCourse = courseService.assignCourseToLecturer(courseDto);
        return new ResponseEntity<>(assignedCourse, HttpStatus.CREATED);
    }

    @PutMapping("/{courseId}/lecturer")
    @PreAuthorize("hasRole('LECTURER') or hasRole('ADMIN')")
    public ResponseEntity<CourseResponseDto> updateLecturerCourse(@PathVariable String courseId, @RequestBody CourseRequestDto courseDto) {
        CourseResponseDto updatedCourse = courseService.updateLecturerCourse(courseId, courseDto);
        return ResponseEntity.ok(updatedCourse);
    }

    @DeleteMapping("/unassign/{courseId}/{lecturerId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> unassignCourseFromLecturer(@PathVariable String courseId, @PathVariable String lecturerId) {
        courseService.unassignCourseFromLecturer(courseId, lecturerId);
        return ResponseEntity.noContent().build();
    }
}