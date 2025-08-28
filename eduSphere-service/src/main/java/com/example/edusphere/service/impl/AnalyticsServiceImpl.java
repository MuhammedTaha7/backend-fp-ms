package com.example.edusphere.service.impl;

import com.example.edusphere.dto.response.LecturerStatsDto;
import com.example.edusphere.dto.response.StudentStatsDto;
import com.example.edusphere.entity.StudentGrade;
import com.example.edusphere.entity.Course;
import com.example.edusphere.repository.CourseRepository;
import com.example.edusphere.repository.StudentGradeRepository;
import com.example.edusphere.repository.LecturerResourceRepository;
import com.example.edusphere.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private final StudentGradeRepository studentGradeRepository;
    private final CourseRepository courseRepository;
    private final LecturerResourceRepository lecturerResourceRepository;

    @Override
    public StudentStatsDto getStudentStats(String studentId) {
        StudentStatsDto dto = new StudentStatsDto();

        try {
            // Get all grades for the student
            List<StudentGrade> studentGrades = studentGradeRepository.findByStudentId(studentId);

            // Get course details for credit calculation
            List<String> courseIds = studentGrades.stream()
                    .map(StudentGrade::getCourseId)
                    .distinct()
                    .collect(Collectors.toList());

            List<Course> courses = courseRepository.findByIdIn(courseIds);
            Map<String, Course> courseMap = courses.stream()
                    .collect(Collectors.toMap(Course::getId, course -> course));

            // Calculate GPA and stats
            double totalWeightedGrades = 0.0;
            int totalCredits = 0;
            int completedCourses = 0;

            for (StudentGrade grade : studentGrades) {
                Course course = courseMap.get(grade.getCourseId());
                if (course != null && grade.getFinalGrade() != null) {
                    int courseCredits = course.getCredits();
                    totalWeightedGrades += grade.getFinalGrade() * courseCredits;
                    totalCredits += courseCredits;
                    completedCourses++;
                }
            }

            // Calculate GPA (weighted average by credits)
            double gpa = totalCredits > 0 ? totalWeightedGrades / totalCredits : 0.0;

            // Round GPA to 2 decimal places
            gpa = Math.round(gpa * 100.0) / 100.0;

            // Set calculated values
            dto.setGpa(gpa);
            dto.setTotalCredits(totalCredits);
            dto.setCompletedCourses(completedCourses);
            dto.setTotalCourses(studentGrades.size());
            dto.setEnrollmentStatus("Active"); // Default status

            log.info("Calculated stats for student {}: GPA={}, Credits={}, Completed={}",
                    studentId, gpa, totalCredits, completedCourses);

        } catch (Exception e) {
            log.error("Error calculating student stats for student {}: {}", studentId, e.getMessage());
            // Set default values in case of error
            dto.setGpa(0.0);
            dto.setTotalCredits(0);
            dto.setCompletedCourses(0);
            dto.setTotalCourses(0);
            dto.setEnrollmentStatus("Unknown");
        }

        return dto;
    }

    @Override
    public LecturerStatsDto getLecturerStats(String lecturerId) {
        LecturerStatsDto dto = new LecturerStatsDto();

        try {
            // Count active courses for lecturer
            List<Course> lecturerCourses = courseRepository.findByLecturerId(lecturerId);
            int activeCourses = lecturerCourses.size();

            // Count total students across all courses
            int totalStudents = 0;
            for (Course course : lecturerCourses) {
                if (course.getEnrollments() != null) {
                    totalStudents += course.getEnrollments().size();
                }
            }

            // Count publications/resources
            int totalPublications = lecturerResourceRepository.findByLecturerId(lecturerId).size();

            // Set calculated values
            dto.setActiveCourses(activeCourses);
            dto.setTotalStudents(totalStudents);
            dto.setTotalPublications(totalPublications);

            // Set default values for fields that require additional implementation
            dto.setAverageRating(4.7); // Placeholder - would need rating system
            dto.setEmploymentStatus("Full-Time"); // Default status

            log.info("Calculated stats for lecturer {}: Courses={}, Students={}, Publications={}",
                    lecturerId, activeCourses, totalStudents, totalPublications);

        } catch (Exception e) {
            log.error("Error calculating lecturer stats for lecturer {}: {}", lecturerId, e.getMessage());
            // Set default values in case of error
            dto.setActiveCourses(0);
            dto.setTotalStudents(0);
            dto.setAverageRating(0.0);
            dto.setTotalPublications(0);
            dto.setEmploymentStatus("Unknown");
        }

        return dto;
    }

    /**
     * Helper method to calculate GPA for a specific student
     * Can be used by other services that need GPA calculation
     */
    public double calculateStudentGPA(String studentId) {
        try {
            List<StudentGrade> studentGrades = studentGradeRepository.findByStudentId(studentId);

            List<String> courseIds = studentGrades.stream()
                    .map(StudentGrade::getCourseId)
                    .distinct()
                    .collect(Collectors.toList());

            List<Course> courses = courseRepository.findByIdIn(courseIds);
            Map<String, Course> courseMap = courses.stream()
                    .collect(Collectors.toMap(Course::getId, course -> course));

            double totalWeightedGrades = 0.0;
            int totalCredits = 0;

            for (StudentGrade grade : studentGrades) {
                Course course = courseMap.get(grade.getCourseId());
                if (course != null && grade.getFinalGrade() != null) {
                    int courseCredits = course.getCredits();
                    totalWeightedGrades += grade.getFinalGrade() * courseCredits;
                    totalCredits += courseCredits;
                }
            }

            return totalCredits > 0 ? Math.round((totalWeightedGrades / totalCredits) * 100.0) / 100.0 : 0.0;
        } catch (Exception e) {
            log.error("Error calculating GPA for student {}: {}", studentId, e.getMessage());
            return 0.0;
        }
    }

    /**
     * Helper method to get total credits earned by a student
     */
    public int calculateTotalCredits(String studentId) {
        try {
            List<StudentGrade> studentGrades = studentGradeRepository.findByStudentId(studentId);

            List<String> courseIds = studentGrades.stream()
                    .map(StudentGrade::getCourseId)
                    .distinct()
                    .collect(Collectors.toList());

            List<Course> courses = courseRepository.findByIdIn(courseIds);
            Map<String, Course> courseMap = courses.stream()
                    .collect(Collectors.toMap(Course::getId, course -> course));

            int totalCredits = 0;
            for (StudentGrade grade : studentGrades) {
                Course course = courseMap.get(grade.getCourseId());
                if (course != null && grade.getFinalGrade() != null) {
                    totalCredits += course.getCredits();
                }
            }

            return totalCredits;
        } catch (Exception e) {
            log.error("Error calculating total credits for student {}: {}", studentId, e.getMessage());
            return 0;
        }
    }
}