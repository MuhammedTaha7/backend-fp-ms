package com.example.edusphere.service.impl;

import com.example.edusphere.dto.response.ChartDataPoint;
import com.example.edusphere.entity.Assignment;
import com.example.edusphere.entity.Submission;
import com.example.edusphere.repository.AssignmentRepository;
import com.example.edusphere.repository.SubmissionRepository;
import com.example.edusphere.service.SubmissionService;
import org.springframework.stereotype.Service;
import com.example.edusphere.dto.response.CourseAnalyticsDTO;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Service
class SubmissionServiceImpl implements SubmissionService {
    private final SubmissionRepository submissionRepository;
    private final AssignmentRepository assignmentRepository;

    public SubmissionServiceImpl(SubmissionRepository submissionRepository, AssignmentRepository assignmentRepository) {
        this.submissionRepository = submissionRepository;
        this.assignmentRepository = assignmentRepository;
    }

    @Override
    public List<Submission> findSubmissionsByCourseId(String courseId) {
        return submissionRepository.findByCourseId(courseId);
    }

    @Override
    public Submission createSubmission(Submission submission) {
        // In a real application, you would add validation here
        return submissionRepository.save(submission);
    }

    @Override
    public List<Submission> findSubmissionsByStudentId(String studentId) {
        return submissionRepository.findByStudentId(studentId);
    }

    @Override
    public CourseAnalyticsDTO getCourseAnalytics(String courseId, int year) {
        // 1. Fetch all submissions for the given course
        LocalDateTime startOfYear = Year.of(year).atDay(1).atStartOfDay();
        LocalDateTime endOfYear = Year.of(year).atMonth(12).atDay(31).atTime(23, 59, 59);
        List<Submission> submissions = submissionRepository.findByCourseIdAndSubmittedAtBetween(courseId, startOfYear, endOfYear);
        if (submissions == null || submissions.isEmpty()) {
            return new CourseAnalyticsDTO(); // Return empty object if no data
        }

        // 2. Calculate average grade
        double totalGrade = 0;
        int submissionCountWithGrade = 0;
        for (Submission s : submissions) {
            if (s.getGrade() != null) {
                totalGrade += s.getGrade();
                submissionCountWithGrade++;
            }
        }
        double averageGrade = (submissionCountWithGrade > 0) ? (totalGrade / submissionCountWithGrade) : 0;


        // 3. Calculate grade distribution
        long[] gradeCounts = new long[5]; // 0: 90-100, 1: 80-89, 2: 70-79, 3: 60-69, 4: <60
        for (Submission s : submissions) {
            if (s.getGrade() != null) {
                int grade = s.getGrade();
                if (grade >= 90) gradeCounts[0]++;
                else if (grade >= 80) gradeCounts[1]++;
                else if (grade >= 70) gradeCounts[2]++;
                else if (grade >= 60) gradeCounts[3]++;
                else gradeCounts[4]++;
            }
        }

        List<String> labels = Arrays.asList("90-100", "80-89", "70-79", "60-69", "Below 60");
        List<Integer> data = Arrays.stream(gradeCounts).mapToInt(l -> (int) l).boxed().collect(Collectors.toList());
        CourseAnalyticsDTO.GradeDistribution distribution = new CourseAnalyticsDTO.GradeDistribution(labels, data);

        // 4. Assemble the DTO
        CourseAnalyticsDTO analyticsDTO = new CourseAnalyticsDTO();
        analyticsDTO.setTotalSubmissions(submissions.size());
        analyticsDTO.setAverageGrade(Math.round(averageGrade * 10.0) / 10.0); // Round to one decimal place
        analyticsDTO.setGradeDistribution(distribution);

        return analyticsDTO;
    }

    @Override
    public List<ChartDataPoint> getAssignmentTimeline(String courseId, int year) {
        // 1. Find all assignments for this course to get their names
        List<Assignment> assignments = assignmentRepository.findByCourse(courseId);

        // 2. Find all submissions for this course in the given year
        LocalDateTime startOfYear = Year.of(year).atDay(1).atStartOfDay();
        LocalDateTime endOfYear = Year.of(year).atMonth(12).atDay(31).atTime(23, 59, 59);
        List<Submission> submissions = submissionRepository.findByCourseIdAndSubmittedAtBetween(courseId, startOfYear, endOfYear);

        // 3. Group submissions by assignmentId and count them
        Map<String, Long> submissionCounts = submissions.stream()
                .collect(Collectors.groupingBy(Submission::getAssignmentId, Collectors.counting()));

        // 4. Map the counts to assignment names for the chart
        return assignments.stream()
                .map(assignment -> {
                    Long count = submissionCounts.getOrDefault(assignment.getId(), 0L);
                    return new ChartDataPoint(assignment.getTitle(), count);
                })
                .collect(Collectors.toList());
    }
}