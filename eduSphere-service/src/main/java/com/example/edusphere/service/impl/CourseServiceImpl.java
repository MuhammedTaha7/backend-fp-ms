package com.example.edusphere.service.impl;

import com.example.edusphere.dto.request.CourseRequestDto;
import com.example.edusphere.dto.request.EnrollmentRequest;
import com.example.edusphere.dto.request.EnrollmentRequestDto;
import com.example.edusphere.dto.response.*;
import com.example.edusphere.entity.Assignment;
import com.example.edusphere.entity.Course;
import com.example.common.entity.UserEntity;
import com.example.edusphere.entity.YearlyEnrollment;
import com.example.edusphere.repository.AssignmentRepository;
import com.example.edusphere.repository.CourseRepository;
import com.example.common.repository.UserRepository;
import com.example.edusphere.service.CourseService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final AssignmentRepository assignmentRepository;

    public CourseServiceImpl(CourseRepository courseRepository, UserRepository userRepository, AssignmentRepository assignmentRepository) {
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
        this.assignmentRepository = assignmentRepository;
    }

    @Override
    public List<CourseSummaryResponse> findAllCoursesForUser(UserDetails userDetails) {
        UserEntity currentUser = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found: " + userDetails.getUsername()));

        String role = currentUser.getRole();
        List<Course> courses;

        if ("1100".equals(role)) { // Admin
            courses = courseRepository.findAll();
        } else if ("1200".equals(role)) { // Lecturer
            courses = courseRepository.findByLecturerId(currentUser.getId());
        } else if ("1300".equals(role)) { // Student
            courses = courseRepository.findByEnrollments_StudentIds(currentUser.getId());
        } else {
            courses = Collections.emptyList();
        }

        return mapCoursesToSummaryResponses(courses);
    }

    // New method implementations for inter-service communication
    @Override
    public List<Course> getUserCourses(String userId, String userRole) {
        if ("1100".equals(userRole) || "ADMIN".equals(userRole)) {
            return courseRepository.findAll();
        } else if ("1200".equals(userRole) || "LECTURER".equals(userRole)) {
            return courseRepository.findByLecturerId(userId);
        } else if ("1300".equals(userRole) || "STUDENT".equals(userRole)) {
            return courseRepository.findByEnrollments_StudentIds(userId);
        }
        return new ArrayList<>();
    }

    @Override
    public boolean canUserAccessCourse(String userId, String userRole, String courseId) {
        Optional<Course> courseOpt = courseRepository.findById(courseId);
        if (courseOpt.isEmpty()) {
            return false;
        }

        Course course = courseOpt.get();
        if ("1100".equals(userRole)) {
            return true;
        }
        if ("1200".equals(userRole)) {
            return userId.equals(course.getLecturerId());
        }
        if ("1300".equals(userRole)) {
            return course.getEnrollments().stream()
                    .anyMatch(enrollment -> enrollment.getStudentIds().contains(userId));
        }
        return false;
    }

    @Override
    public String getCourseName(String courseId) {
        return courseRepository.findById(courseId)
                .map(Course::getName)
                .orElse("Unknown Course");
    }

    @Override
    public Optional<Course> findById(String id) {
        return courseRepository.findById(id);
    }

    private List<CourseSummaryResponse> mapCoursesToSummaryResponses(List<Course> courses) {
        // To avoid N+1 queries, get all unique lecturer IDs first
        List<String> lecturerIds = courses.stream()
                .map(Course::getLecturerId)
                .filter(id -> id != null && !id.isEmpty())
                .distinct()
                .collect(Collectors.toList());

        // Fetch all required lecturers in a single query
        List<UserEntity> lecturers = userRepository.findAllById(lecturerIds);

        // Map courses to DTOs
        return courses.stream().map(course -> {
            CourseSummaryResponse dto = new CourseSummaryResponse();
            dto.setId(course.getId());
            dto.setName(course.getName());
            dto.setCode(course.getCode());
            dto.setImageUrl(course.getImageUrl());
            dto.setDepartment(course.getDepartment());
            dto.setCredits(course.getCredits());
            dto.setEnrollments(course.getEnrollments());

            // Find the lecturer's name from our pre-fetched list
            lecturers.stream()
                    .filter(l -> l.getId().equals(course.getLecturerId()))
                    .findFirst()
                    .ifPresent(l -> dto.setLecturerName(l.getName()));

            return dto;
        }).collect(Collectors.toList());
    }

//    @Override
//    public Optional<Course> findCourseById(String id) {
//        return courseRepository.findById(id);
//    }

    @Override
    public Course createCourse(Course course) {
        if (course.getEnrollments() == null) {
            course.setEnrollments(new ArrayList<>());
        }
        return courseRepository.save(course);
    }

    @Override
    public Course updateCourse(String id, Course courseDetails) {
        Course existingCourse = courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found with id: " + id));

        // Update all metadata fields, but NOT the enrollments list
        if (courseDetails.getName() != null) existingCourse.setName(courseDetails.getName());
        if (courseDetails.getCode() != null) existingCourse.setCode(courseDetails.getCode());
        if (courseDetails.getDescription() != null) existingCourse.setDescription(courseDetails.getDescription());
        if (courseDetails.getImageUrl() != null) existingCourse.setImageUrl(courseDetails.getImageUrl());
        if (courseDetails.getAcademicYear() != null) existingCourse.setAcademicYear(courseDetails.getAcademicYear());
        if (courseDetails.getSemester() != null) existingCourse.setSemester(courseDetails.getSemester());
        if (courseDetails.getYear() != null) existingCourse.setYear(courseDetails.getYear());
        if (courseDetails.getSelectable() != null) existingCourse.setSelectable(courseDetails.getSelectable());
        if (courseDetails.getLecturerId() != null) existingCourse.setLecturerId(courseDetails.getLecturerId());
        if (courseDetails.getDepartment() != null) existingCourse.setDepartment(courseDetails.getDepartment());
        if (courseDetails.getLanguage() != null) existingCourse.setLanguage(courseDetails.getLanguage());
        if (courseDetails.getProgress() != null) existingCourse.setProgress(courseDetails.getProgress());
        if (courseDetails.getPrerequisites() != null) existingCourse.setPrerequisites(courseDetails.getPrerequisites());
        if (courseDetails.getFinalExam() != null) existingCourse.setFinalExam(courseDetails.getFinalExam());
        existingCourse.setCredits(courseDetails.getCredits());

        return courseRepository.save(existingCourse);
    }

    @Override
    public void deleteCourse(String id) {
        if (!courseRepository.existsById(id)) {
            throw new RuntimeException("Course not found with id: " + id);
        }
        courseRepository.deleteById(id);
    }

    @Override
    @Transactional
    public Course enrollStudent(String courseId, EnrollmentRequest enrollmentRequest) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found with id: " + courseId));

        int academicYear = enrollmentRequest.getAcademicYear();
        String studentId = enrollmentRequest.getStudentId();

        // Safety check: Initialize enrollments list if it's null (for older data)
        if (course.getEnrollments() == null) {
            course.setEnrollments(new ArrayList<>());
        }

        // Find if an enrollment object for this year already exists
        Optional<YearlyEnrollment> existingEnrollmentOpt = course.getEnrollments().stream()
                .filter(e -> e.getAcademicYear() == academicYear)
                .findFirst();

        YearlyEnrollment yearlyEnrollment;
        if (existingEnrollmentOpt.isPresent()) {
            // If it exists, use it
            yearlyEnrollment = existingEnrollmentOpt.get();
        } else {
            // If not, create a new one and add it to the course's list
            yearlyEnrollment = new YearlyEnrollment(academicYear);
            course.getEnrollments().add(yearlyEnrollment);
        }

        // Add the student to the list for the correct year, avoiding duplicates
        if (!yearlyEnrollment.getStudentIds().contains(studentId)) {
            yearlyEnrollment.getStudentIds().add(studentId);
        }

        return courseRepository.save(course);
    }

    @Override
    @Transactional
    public Course unenrollStudents(String courseId, List<String> studentIdsToUnenroll) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found with id: " + courseId));

        if (course.getEnrollments() != null) {
            // Iterate over each yearly enrollment list
            course.getEnrollments().forEach(yearlyEnrollment -> {
                // Remove all matching student IDs from the list for that year
                yearlyEnrollment.getStudentIds().removeAll(studentIdsToUnenroll);
            });
        }

        // Save the updated course with the students removed
        return courseRepository.save(course);
    }

    @Override
    public CourseDetailsResponse findCourseDetailsById(String id) {
        // Fetch the main course object
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found with id: " + id));

        // Convert the course entity to our base DTO
        CourseDetailsResponse responseDTO = CourseDetailsResponse.fromEntity(course);

        // Fetch the lecturer's name (if lecturerId exists)
        if (course.getLecturerId() != null && !course.getLecturerId().trim().isEmpty()) {

            Optional<UserEntity> lecturerOptional = userRepository.findById(course.getLecturerId());
            if (lecturerOptional.isPresent()) {
                UserEntity lecturer = lecturerOptional.get();
                responseDTO.setLecturerName(lecturer.getName());
                responseDTO.setInstructorName(lecturer.getName()); // Set both fields that frontend expects
                responseDTO.setLecturer(lecturer); // You can set the full lecturer object or just basic info
            } else {
                responseDTO.setLecturerName("Lecturer Not Found");
                responseDTO.setInstructorName("Lecturer Not Found");
                responseDTO.setLecturer(null);
            }
        } else {
            responseDTO.setLecturerName("No Lecturer Assigned");
            responseDTO.setInstructorName("No Lecturer Assigned");
            responseDTO.setLecturer(null);
        }

        // Fetch all assignments for this course
        List<Assignment> assignments = assignmentRepository.findByCourse(id);
        List<AssignmentResponse> assignmentDTOs = assignments.stream()
                .map(AssignmentResponse::fromEntity)
                .collect(Collectors.toList());
        responseDTO.setAssignments(assignmentDTOs);

        return responseDTO;
    }

    /**
     * Finds all courses a student is enrolled in and maps them to EnrollmentResponseDto.
     */
    @Override
    public List<EnrollmentResponseDto> getStudentEnrollments(String studentId) {
        List<Course> enrolledCourses = courseRepository.findByEnrollments_StudentIds(studentId);

        return enrolledCourses.stream()
                .map(course -> {
                    // Find the enrollment year for this student
                    Optional<YearlyEnrollment> studentEnrollment = course.getEnrollments().stream()
                            .filter(ye -> ye.getStudentIds().contains(studentId))
                            .findFirst();

                    // Get lecturer name - fixed version
                    String lecturerName = course.getLecturerId() != null ?
                            userRepository.findById(course.getLecturerId())
                                    .map(UserEntity::getName)
                                    .orElse("Not Assigned") :
                            "Not Assigned";

                    return studentEnrollment.map(ye -> {
                        EnrollmentResponseDto dto = new EnrollmentResponseDto();
                        dto.setId(course.getId());
                        dto.setStudentId(studentId);
                        dto.setCourseId(course.getId());
                        dto.setCourseCode(course.getCode());
                        dto.setCourseName(course.getName());
                        dto.setCredits(course.getCredits());
                        dto.setSemester(course.getSemester());
                        dto.setAcademicYear(ye.getAcademicYear());
                        dto.setLecturer(lecturerName);
                        dto.setStatus("enrolled");
                        return dto;
                    }).orElse(null);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public EnrollmentResponseDto addStudentEnrollment(EnrollmentRequestDto enrollmentDto) {
        Course course = courseRepository.findById(enrollmentDto.getCourseId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        // Check if student is already enrolled
        boolean isEnrolled = course.getEnrollments().stream()
                .anyMatch(ye -> ye.getStudentIds().contains(enrollmentDto.getStudentId()));

        if (isEnrolled) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Student is already enrolled in this course");
        }

        // Add student to the first enrollment year, or create a new one
        YearlyEnrollment currentYearEnrollment = course.getEnrollments().stream()
                .filter(ye -> ye.getAcademicYear() == 2025) // Assuming current year for simplicity
                .findFirst()
                .orElseGet(() -> {
                    YearlyEnrollment newEnrollment = new YearlyEnrollment(2025);
                    course.getEnrollments().add(newEnrollment);
                    return newEnrollment;
                });

        currentYearEnrollment.getStudentIds().add(enrollmentDto.getStudentId());

        Course updatedCourse = courseRepository.save(course);

        // Fetch lecturer name
        String lecturerName = "Not Assigned";
        if (updatedCourse.getLecturerId() != null) {
            UserEntity lecturer = userRepository.findById(updatedCourse.getLecturerId()).orElse(null);
            if (lecturer != null) {
                lecturerName = lecturer.getName();
            }
        }

        EnrollmentResponseDto responseDto = new EnrollmentResponseDto();
        responseDto.setId(updatedCourse.getId());
        responseDto.setStudentId(enrollmentDto.getStudentId());
        responseDto.setCourseId(updatedCourse.getId());
        responseDto.setCourseCode(updatedCourse.getCode());
        responseDto.setCourseName(updatedCourse.getName());
        responseDto.setCredits(updatedCourse.getCredits());
        responseDto.setSemester(updatedCourse.getSemester());
        responseDto.setLecturer(lecturerName);
        responseDto.setStatus("enrolled"); // Assuming new enrollment is 'enrolled'

        return responseDto;
    }

    @Override
    public EnrollmentResponseDto updateStudentEnrollment(String courseId, EnrollmentRequestDto enrollmentDto) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        // Check if the student is actually enrolled
        boolean isEnrolled = course.getEnrollments().stream()
                .anyMatch(ye -> ye.getStudentIds().contains(enrollmentDto.getStudentId()));

        if (!isEnrolled) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Student is not enrolled in this course");
        }

        // In this implementation, a PUT request would likely change the enrollment status.
        // As your current `YearlyEnrollment` entity only tracks student IDs and not status,
        // we'll simply return the enrollment, assuming the frontend handles the status display.
        // We can add a more robust status field to the entity later if needed.

        // For now, we'll just return the updated DTO
        // We'll need to fetch the lecturer name to return a complete DTO
        String lecturerName = "Not Assigned";
        if (course.getLecturerId() != null) {
            UserEntity lecturer = userRepository.findById(course.getLecturerId()).orElse(null);
            if (lecturer != null) {
                lecturerName = lecturer.getName();
            }
        }

        EnrollmentResponseDto responseDto = new EnrollmentResponseDto();
        responseDto.setId(course.getId());
        responseDto.setStudentId(enrollmentDto.getStudentId());
        responseDto.setCourseId(course.getId());
        responseDto.setCourseCode(course.getCode());
        responseDto.setCourseName(course.getName());
        responseDto.setCredits(course.getCredits());
        responseDto.setSemester(course.getSemester());
        responseDto.setLecturer(lecturerName);
        responseDto.setStatus(enrollmentDto.getStatus());

        return responseDto;
    }

    /**
     * Finds all courses assigned to a specific lecturer.
     */
    @Override
    public List<CourseResponseDto> getLecturerCourses(String lecturerId) {
        List<Course> lecturerCourses = courseRepository.findByLecturerId(lecturerId);

        if (lecturerCourses.isEmpty()) {
            return Collections.emptyList();
        }

        return lecturerCourses.stream()
                .map(course -> {
                    CourseResponseDto dto = new CourseResponseDto();
                    dto.setId(course.getId());
                    dto.setCourseCode(course.getCode());
                    dto.setCourseName(course.getName());
                    dto.setCredits(course.getCredits());
                    dto.setSemester(course.getSemester());
                    dto.setDepartment(course.getDepartment());

                    // Calculate class size from the list of enrollments
                    Integer classSize = course.getEnrollments().stream()
                            .flatMap(ye -> ye.getStudentIds().stream())
                            .distinct()
                            .collect(Collectors.toList())
                            .size();
                    dto.setClassSize(classSize);

                    // A simple way to determine status based on the current year
                    String status = (course.getYear() != null && course.getYear().equals(2025)) ? "Active" : "Archived";
                    dto.setStatus(status);

                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * Assigns an existing course to a lecturer.
     */
    @Override
    public CourseResponseDto assignCourseToLecturer(CourseRequestDto courseDto) {
        Course course = courseRepository.findById(courseDto.getCourseId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        if (course.getLecturerId() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Course is already assigned to a lecturer");
        }

        course.setLecturerId(courseDto.getLecturerId());
        course.setSemester(courseDto.getSemester());
        // You can update other fields from the DTO if needed
        Course updatedCourse = courseRepository.save(course);

        // Map the updated course to a DTO and return
        CourseResponseDto responseDto = new CourseResponseDto();
        responseDto.setId(updatedCourse.getId());
        responseDto.setCourseCode(updatedCourse.getCode());
        responseDto.setCourseName(updatedCourse.getName());
        responseDto.setCredits(updatedCourse.getCredits());
        responseDto.setSemester(updatedCourse.getSemester());
        responseDto.setDepartment(updatedCourse.getDepartment());
        responseDto.setClassSize(0); // A newly assigned course has no students yet
        responseDto.setStatus("Active");

        return responseDto;
    }

    /**
     * Updates an existing course that is assigned to a lecturer.
     */
    @Override
    public CourseResponseDto updateLecturerCourse(String courseId, CourseRequestDto courseDto) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        // Only update fields that can be changed
        if (courseDto.getSemester() != null) {
            course.setSemester(courseDto.getSemester());
        }

        Course updatedCourse = courseRepository.save(course);

        // Map the updated course to a DTO
        CourseResponseDto responseDto = new CourseResponseDto();
        responseDto.setId(updatedCourse.getId());
        responseDto.setCourseCode(updatedCourse.getCode());
        responseDto.setCourseName(updatedCourse.getName());
        responseDto.setCredits(updatedCourse.getCredits());
        responseDto.setSemester(updatedCourse.getSemester());
        responseDto.setDepartment(updatedCourse.getDepartment());

        Integer classSize = updatedCourse.getEnrollments().stream()
                .flatMap(ye -> ye.getStudentIds().stream())
                .distinct()
                .collect(Collectors.toList())
                .size();
        responseDto.setClassSize(classSize);
        responseDto.setStatus("Active");

        return responseDto;
    }

    /**
     * Unassigns a course from a lecturer by clearing the lecturerId field.
     */
    @Override
    public void unassignCourseFromLecturer(String courseId, String lecturerId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        if (!lecturerId.equals(course.getLecturerId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Course is not assigned to this lecturer");
        }

        course.setLecturerId(null);
        courseRepository.save(course);
    }

}