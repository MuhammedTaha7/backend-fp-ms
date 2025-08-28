package com.example.edusphere.service.impl;

import com.example.edusphere.dto.request.AssignmentRequestDto;
import com.example.edusphere.dto.response.AssignmentResponseDto;
import com.example.edusphere.dto.response.DashboardChartsDto;
import com.example.edusphere.dto.response.DashboardDataResponseDto;
import com.example.edusphere.dto.response.DashboardStatsDto;
import com.example.edusphere.entity.Assignment;
import com.example.edusphere.entity.Course;
import com.example.edusphere.entity.StudentGrade;
import com.example.common.entity.UserEntity;
import com.example.edusphere.repository.AssignmentRepository;
import com.example.edusphere.repository.CourseRepository;
import com.example.edusphere.repository.StudentGradeRepository;
import com.example.common.repository.UserRepository;
import com.example.edusphere.service.DashboardService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DashboardServiceImpl implements DashboardService {

    private final UserRepository userRepository;
    private final AssignmentRepository assignmentRepository;
    private final CourseRepository courseRepository;
    private final StudentGradeRepository studentGradeRepository;

    public DashboardServiceImpl(UserRepository userRepository,
                                AssignmentRepository assignmentRepository,
                                CourseRepository courseRepository,
                                StudentGradeRepository studentGradeRepository) {
        this.userRepository = userRepository;
        this.assignmentRepository = assignmentRepository;
        this.courseRepository = courseRepository;
        this.studentGradeRepository = studentGradeRepository;
    }

    @Override
    public DashboardDataResponseDto getDashboardDataForRole(String userRole) {
        List<Course> allCourses = courseRepository.findAll();

        long activeUserCount = userRepository.count();
        Set<String> departments = allCourses.stream()
                .map(Course::getDepartment)
                .collect(Collectors.toSet());
        long activeDepartmentCount = departments.size();

        DashboardStatsDto statsDto = new DashboardStatsDto(activeUserCount, "System Optimal", activeDepartmentCount);
        DashboardChartsDto chartsDto = getChartData(allCourses);
        List<AssignmentResponseDto> assignments = getUpcomingAssignments();

        return new DashboardDataResponseDto(statsDto, chartsDto, assignments);
    }

    private DashboardChartsDto getChartData(List<Course> allCourses) {
        // --- Department Enrollment Chart Logic ---
        // Get all students (users with role "1300")
        List<UserEntity> students = userRepository.findByRole("1300");

        // Count students by department
        Map<String, Long> enrollmentByDept = students.stream()
                .filter(student -> student.getDepartment() != null && !student.getDepartment().trim().isEmpty())
                .collect(Collectors.groupingBy(
                        UserEntity::getDepartment,
                        Collectors.counting()
                ));

        // Convert to the required format for the chart
        List<Map<String, Object>> departmentEnrollmentData = enrollmentByDept.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> dataPoint = new HashMap<>();
                    dataPoint.put("name", entry.getKey());
                    dataPoint.put("value", entry.getValue());
                    return dataPoint;
                })
                .collect(Collectors.toList());

        // --- System Usage Pie Chart Logic ---
        long studentCount = userRepository.findByRole("1300").size();
        long lecturerCount = userRepository.findByRole("1200").size();
        long adminCount = userRepository.findByRole("1100").size();
        List<Map<String, Object>> systemUsageData = List.of(
                Map.of("name", "Students", "value", studentCount),
                Map.of("name", "Lecturers", "value", lecturerCount),
                Map.of("name", "Admins", "value", adminCount)
        );

        // --- Department GPA Chart Logic (CHANGED FROM ANNUAL ENROLLMENT) ---
        List<Map<String, Object>> departmentGpaData = getDepartmentGpaData();

        return new DashboardChartsDto(departmentEnrollmentData, systemUsageData, departmentGpaData);
    }

    private List<Map<String, Object>> getDepartmentGpaData() {
        // Get all courses with their departments
        List<Course> allCourses = courseRepository.findAll();
        Map<String, String> courseDepartmentMap = allCourses.stream()
                .collect(Collectors.toMap(Course::getId, Course::getDepartment));

        // Get all student grades
        List<StudentGrade> allGrades = studentGradeRepository.findAll();

        // Group grades by department and calculate average GPA
        Map<String, List<Double>> gradesByDepartment = new HashMap<>();

        for (StudentGrade grade : allGrades) {
            if (grade.getFinalGrade() != null && grade.getCourseId() != null) {
                String department = courseDepartmentMap.get(grade.getCourseId());
                if (department != null && !department.trim().isEmpty()) {
                    gradesByDepartment.computeIfAbsent(department, k -> new java.util.ArrayList<>())
                            .add(grade.getFinalGrade());
                }
            }
        }

        // Calculate average GPA for each department
        return gradesByDepartment.entrySet().stream()
                .map(entry -> {
                    String department = entry.getKey();
                    List<Double> grades = entry.getValue();

                    // Calculate average GPA (assuming grades are out of 4.0)
                    double avgGpa = grades.stream()
                            .mapToDouble(Double::doubleValue)
                            .average()
                            .orElse(0.0);

                    // Round to 2 decimal places
                    avgGpa = Math.round(avgGpa * 100.0) / 100.0;

                    Map<String, Object> dataPoint = new HashMap<>();
                    dataPoint.put("name", department);
                    dataPoint.put("value", avgGpa);
                    return dataPoint;
                })
                .filter(dataPoint -> (Double) dataPoint.get("value") > 0) // Only include departments with grades
                .collect(Collectors.toList());
    }

    // --- Assignment-related methods (no changes) ---
    @Override
    public AssignmentResponseDto createAssignment(AssignmentRequestDto assignmentRequestDto) {
        Assignment newAssignment = new Assignment();
        mapToAssignmentEntity(newAssignment, assignmentRequestDto);
        Assignment savedAssignment = assignmentRepository.save(newAssignment);
        return mapToAssignmentResponseDto(savedAssignment);
    }

    @Override
    public AssignmentResponseDto updateAssignment(String id, AssignmentRequestDto assignmentRequestDto) {
        Assignment existingAssignment = assignmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Assignment not found with id: " + id));
        mapToAssignmentEntity(existingAssignment, assignmentRequestDto);
        Assignment savedAssignment = assignmentRepository.save(existingAssignment);
        return mapToAssignmentResponseDto(savedAssignment);
    }

    @Override
    public void deleteAssignment(String id) {
        if (!assignmentRepository.existsById(id)) {
            throw new RuntimeException("Assignment not found with id: " + id);
        }
        assignmentRepository.deleteById(id);
    }

    private List<AssignmentResponseDto> getUpcomingAssignments() {
        List<Assignment> upcomingAssignments = assignmentRepository.findByDueDateAfter(LocalDate.now().minusDays(1));
        return upcomingAssignments.stream()
                .map(this::mapToAssignmentResponseDto)
                .collect(Collectors.toList());
    }

    private void mapToAssignmentEntity(Assignment assignment, AssignmentRequestDto requestDto) {
        if (requestDto.getTitle() != null) assignment.setTitle(requestDto.getTitle());
        if (requestDto.getDescription() != null) assignment.setDescription(requestDto.getDescription());
        if (requestDto.getCourse() != null) assignment.setCourse(requestDto.getCourse());
        if (requestDto.getType() != null) assignment.setType(requestDto.getType());
        if (requestDto.getDueDate() != null) assignment.setDueDate(requestDto.getDueDate());
        if (requestDto.getDueTime() != null) assignment.setDueTime(requestDto.getDueTime());
        assignment.setProgress(Integer.parseInt(requestDto.getProgress()));
        if (requestDto.getStatus() != null) assignment.setStatus(requestDto.getStatus());
        if (requestDto.getPriority() != null) assignment.setPriority(requestDto.getPriority());
        if (requestDto.getInstructorId() != null) assignment.setInstructorId(requestDto.getInstructorId());
        if (requestDto.getDifficulty() != null) assignment.setDifficulty(requestDto.getDifficulty());
        if (requestDto.getSemester() != null) assignment.setSemester(requestDto.getSemester());
        if (requestDto.getBadges() != null) assignment.setBadges(requestDto.getBadges());
    }

    private AssignmentResponseDto mapToAssignmentResponseDto(Assignment assignment) {
        return new AssignmentResponseDto(
                assignment.getId(),
                assignment.getTitle(),
                assignment.getType(),
                assignment.getDueDate(),
                assignment.getDescription(),
                assignment.getProgress(),
                assignment.getBadges()
        );
    }
}