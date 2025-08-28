package com.example.edusphere.service.impl;

import com.example.edusphere.dto.response.LecturerProfileDto;
import com.example.edusphere.dto.response.StudentProfileDto;
import com.example.common.entity.UserEntity;
import com.example.edusphere.entity.StudentGrade;
import com.example.edusphere.entity.Course;
import com.example.common.repository.UserRepository;
import com.example.edusphere.repository.StudentGradeRepository;
import com.example.edusphere.repository.CourseRepository;
import com.example.edusphere.service.ProfileService;
import com.example.edusphere.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.ArrayList;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final UserRepository userRepository;
    private final StudentGradeRepository studentGradeRepository;
    private final CourseRepository courseRepository;
    private final AnalyticsService analyticsService;

    @Override
    public StudentProfileDto getStudentProfile(String id) {
        UserEntity student = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found with ID: " + id));

        if (!"1300".equals(student.getRole())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is not a student");
        }

        StudentProfileDto dto = new StudentProfileDto();
        dto.setId(student.getId());
        dto.setName(student.getName());
        dto.setEmail(student.getEmail());
        dto.setPhoneNumber(student.getPhoneNumber());
        dto.setProfilePic(student.getProfilePic());
        dto.setDateOfBirth(student.getDateOfBirth());
        dto.setAcademicYear(student.getAcademicYear());
        dto.setDepartment(student.getDepartment());
        dto.setStatus(student.getStatus());

        try {
            // Calculate and set GPA using AnalyticsService
            double gpa = analyticsService.calculateStudentGPA(id);
            dto.setGpa(gpa);

            log.info("Successfully retrieved profile for student {} with GPA {}", id, gpa);

        } catch (Exception e) {
            log.error("Error retrieving profile for student {}: {}", id, e.getMessage());
            dto.setGpa(0.0);
        }

        return dto;
    }

    @Override
    public LecturerProfileDto getLecturerProfile(String id) {
        UserEntity lecturer = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lecturer not found with ID: " + id));

        if (!"1200".equals(lecturer.getRole())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is not a lecturer");
        }

        LecturerProfileDto dto = new LecturerProfileDto();
        dto.setId(lecturer.getId());
        dto.setName(lecturer.getName());
        dto.setEmail(lecturer.getEmail());
        dto.setPhoneNumber(lecturer.getPhoneNumber());
        dto.setProfilePic(lecturer.getProfilePic());
        dto.setDateOfBirth(lecturer.getDateOfBirth());
        dto.setDepartment(lecturer.getDepartment());
        dto.setSpecialization(lecturer.getSpecialization());
        dto.setEmploymentType(lecturer.getEmploymentType());
        dto.setExperience(lecturer.getExperience());

        try {
            // Get courses taught by lecturer
            List<Course> lecturerCourses = courseRepository.findByLecturerId(id);
            dto.setActiveCourses(lecturerCourses.size());

            // Set rating from user entity
            dto.setRating(lecturer.getRating());

            log.info("Successfully retrieved profile for lecturer {} with {} courses", id, lecturerCourses.size());

        } catch (Exception e) {
            log.error("Error retrieving profile for lecturer {}: {}", id, e.getMessage());
            dto.setActiveCourses(0);
        }

        return dto;
    }

    /**
     * Helper method to get all student grades with course information
     * This can be used by other services that need detailed grade information
     */
    public List<StudentGradeWithCourseInfo> getStudentGradesWithCourseInfo(String studentId) {
        try {
            List<StudentGrade> studentGrades = studentGradeRepository.findByStudentId(studentId);

            List<String> courseIds = studentGrades.stream()
                    .map(StudentGrade::getCourseId)
                    .distinct()
                    .collect(Collectors.toList());

            List<Course> courses = courseRepository.findByIdIn(courseIds);
            Map<String, Course> courseMap = courses.stream()
                    .collect(Collectors.toMap(Course::getId, course -> course));

            List<StudentGradeWithCourseInfo> gradeInfoList = new ArrayList<>();
            for (StudentGrade grade : studentGrades) {
                Course course = courseMap.get(grade.getCourseId());
                if (course != null) {
                    StudentGradeWithCourseInfo gradeInfo = new StudentGradeWithCourseInfo();
                    gradeInfo.setId(grade.getId());
                    gradeInfo.setCourseId(grade.getCourseId());
                    gradeInfo.setCourseCode(course.getCode());
                    gradeInfo.setCourseName(course.getName());
                    gradeInfo.setCredits(course.getCredits());
                    gradeInfo.setSemester(course.getSemester());
                    gradeInfo.setAcademicYear(course.getAcademicYear());
                    gradeInfo.setDepartment(course.getDepartment());
                    gradeInfo.setGrades(grade.getGrades());
                    gradeInfo.setFinalGrade(grade.getFinalGrade());
                    gradeInfo.setFinalLetterGrade(grade.getFinalLetterGrade());
                    gradeInfo.setCreatedAt(grade.getCreatedAt());
                    gradeInfo.setUpdatedAt(grade.getUpdatedAt());

                    gradeInfoList.add(gradeInfo);
                }
            }

            return gradeInfoList;
        } catch (Exception e) {
            log.error("Error retrieving grades with course info for student {}: {}", studentId, e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Helper method to get lecturer grades with course information
     * This can be used by other services that need detailed grade information
     */
    public List<LecturerGradeWithCourseInfo> getLecturerGradesWithCourseInfo(String lecturerId) {
        try {
            List<Course> lecturerCourses = courseRepository.findByLecturerId(lecturerId);
            List<String> courseIds = lecturerCourses.stream()
                    .map(Course::getId)
                    .collect(Collectors.toList());

            List<StudentGrade> lecturerGrades = new ArrayList<>();
            if (!courseIds.isEmpty()) {
                lecturerGrades = studentGradeRepository.findByCourseIdIn(courseIds);
            }

            Map<String, Course> courseMap = lecturerCourses.stream()
                    .collect(Collectors.toMap(Course::getId, course -> course));

            List<LecturerGradeWithCourseInfo> courseGradeInfoList = new ArrayList<>();
            for (StudentGrade grade : lecturerGrades) {
                Course course = courseMap.get(grade.getCourseId());
                if (course != null) {
                    LecturerGradeWithCourseInfo gradeInfo = new LecturerGradeWithCourseInfo();
                    gradeInfo.setId(grade.getId());
                    gradeInfo.setStudentId(grade.getStudentId());
                    gradeInfo.setCourseId(grade.getCourseId());
                    gradeInfo.setCourseCode(course.getCode());
                    gradeInfo.setCourseName(course.getName());
                    gradeInfo.setCredits(course.getCredits());
                    gradeInfo.setSemester(course.getSemester());
                    gradeInfo.setAcademicYear(course.getAcademicYear());
                    gradeInfo.setDepartment(course.getDepartment());
                    gradeInfo.setGrades(grade.getGrades());
                    gradeInfo.setFinalGrade(grade.getFinalGrade());
                    gradeInfo.setFinalLetterGrade(grade.getFinalLetterGrade());
                    gradeInfo.setCreatedAt(grade.getCreatedAt());
                    gradeInfo.setUpdatedAt(grade.getUpdatedAt());

                    courseGradeInfoList.add(gradeInfo);
                }
            }

            return courseGradeInfoList;
        } catch (Exception e) {
            log.error("Error retrieving grades with course info for lecturer {}: {}", lecturerId, e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Get basic student grades (without detailed course info)
     */
    public List<StudentGrade> getStudentGrades(String studentId) {
        try {
            return studentGradeRepository.findByStudentId(studentId);
        } catch (Exception e) {
            log.error("Error retrieving grades for student {}: {}", studentId, e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Get basic lecturer grades (without detailed course info)
     */
    public List<StudentGrade> getLecturerGrades(String lecturerId) {
        try {
            List<Course> lecturerCourses = courseRepository.findByLecturerId(lecturerId);
            List<String> courseIds = lecturerCourses.stream()
                    .map(Course::getId)
                    .collect(Collectors.toList());

            if (!courseIds.isEmpty()) {
                return studentGradeRepository.findByCourseIdIn(courseIds);
            }
            return new ArrayList<>();
        } catch (Exception e) {
            log.error("Error retrieving grades for lecturer {}: {}", lecturerId, e.getMessage());
            return new ArrayList<>();
        }
    }

    // Helper classes for detailed grade information
    @lombok.Data
    public static class StudentGradeWithCourseInfo {
        private String id;
        private String courseId;
        private String courseCode;
        private String courseName;
        private int credits;
        private String semester;
        private String academicYear;
        private String department;
        private Map<String, Double> grades;
        private Double finalGrade;
        private String finalLetterGrade;
        private java.time.LocalDateTime createdAt;
        private java.time.LocalDateTime updatedAt;
    }

    @lombok.Data
    public static class LecturerGradeWithCourseInfo {
        private String id;
        private String studentId;
        private String courseId;
        private String courseCode;
        private String courseName;
        private int credits;
        private String semester;
        private String academicYear;
        private String department;
        private Map<String, Double> grades;
        private Double finalGrade;
        private String finalLetterGrade;
        private java.time.LocalDateTime createdAt;
        private java.time.LocalDateTime updatedAt;
    }
}