package com.example.edusphere.controller;

import com.example.edusphere.entity.GradeColumn;
import com.example.edusphere.entity.StudentGrade;
import com.example.edusphere.service.GradeService;
import com.example.edusphere.service.impl.GradeServiceImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "false")
public class GradeController {

    private final GradeService gradeService;
    private final GradeServiceImpl gradeServiceImpl;

    public GradeController(GradeService gradeService, GradeServiceImpl gradeServiceImpl) {
        this.gradeService = gradeService;
        this.gradeServiceImpl = gradeServiceImpl;
    }

    // Grade Columns Endpoints

    /**
     * GET /api/courses/{courseId}/grade-columns : Get all grade columns for a course
     */
    @GetMapping("/courses/{courseId}/grade-columns")
    @PreAuthorize("hasRole('LECTURER') or hasRole('STUDENT')")
    public ResponseEntity<?> getGradeColumns(@PathVariable String courseId) {
        try {

            List<GradeColumn> columns = gradeService.getGradeColumnsByCourse(courseId);

            return ResponseEntity.ok(columns);
        } catch (Exception e) {
            System.err.println("Error fetching grade columns: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch grade columns: " + e.getMessage()));
        }
    }

    /**
     * POST /api/grade-columns : Create a new grade column
     */
    @PostMapping("/grade-columns")
    @PreAuthorize("hasRole('LECTURER')")
    public ResponseEntity<?> createGradeColumn(@RequestBody GradeColumn gradeColumn) {
        try {

            // Enhanced validation
            List<String> errors = new ArrayList<>();

            if (gradeColumn.getName() == null || gradeColumn.getName().trim().isEmpty()) {
                errors.add("Grade column name is required");
            }
            if (gradeColumn.getPercentage() == null || gradeColumn.getPercentage() <= 0 || gradeColumn.getPercentage() > 100) {
                errors.add("Percentage must be between 1 and 100");
            }
            if (gradeColumn.getCourseId() == null || gradeColumn.getCourseId().trim().isEmpty()) {
                errors.add("Course ID is required");
            }
            if (gradeColumn.getType() == null || gradeColumn.getType().trim().isEmpty()) {
                gradeColumn.setType("assignment"); // Set default
            }

            if (!errors.isEmpty()) {
                System.err.println("Validation errors: " + errors);
                return ResponseEntity.badRequest().body(Map.of("errors", errors));
            }

            GradeColumn created = gradeService.createGradeColumn(gradeColumn);

            return new ResponseEntity<>(created, HttpStatus.CREATED);

        } catch (RuntimeException e) {
            System.err.println("Runtime error: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }

    /**
     * PUT /api/grade-columns/{columnId} : Update a grade column
     */
    @PutMapping("/grade-columns/{columnId}")
    @PreAuthorize("hasRole('LECTURER')")
    public ResponseEntity<?> updateGradeColumn(
            @PathVariable String columnId,
            @RequestBody GradeColumn updates) {
        try {

            GradeColumn updated = gradeService.updateGradeColumn(columnId, updates);

            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            System.err.println("Runtime error: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }

    /**
     * DELETE /api/grade-columns/{columnId} : Delete a grade column
     */
    @DeleteMapping("/grade-columns/{columnId}")
    @PreAuthorize("hasRole('LECTURER')")
    public ResponseEntity<?> deleteGradeColumn(@PathVariable String columnId) {
        try {

            gradeService.deleteGradeColumn(columnId);

            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            System.err.println("Runtime error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }

    // Student Grades Endpoints

    /**
     * GET /api/courses/{courseId}/grades : Get all student grades for a course
     */
    @GetMapping("/courses/{courseId}/grades")
    @PreAuthorize("hasRole('LECTURER') or hasRole('STUDENT')")
    public ResponseEntity<?> getCourseGrades(@PathVariable String courseId) {
        try {

            List<StudentGrade> grades = gradeService.getGradesByCourse(courseId);

            return ResponseEntity.ok(grades);
        } catch (Exception e) {
            System.err.println("Error fetching course grades: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch grades: " + e.getMessage()));
        }
    }

    /**
     * PUT /api/students/{studentId}/grades/{columnId} : Update a specific student's grade
     * ENHANCED VERSION with detailed logging and validation
     */
    @PutMapping("/students/{studentId}/grades/{columnId}")
    @PreAuthorize("hasRole('LECTURER')")
    public ResponseEntity<?> updateGrade(
            @PathVariable String studentId,
            @PathVariable String columnId,
            @RequestBody Map<String, Object> request) {
        try {

            // Enhanced grade parsing with detailed logging
            Double grade = null;
            Object gradeObj = request.get("grade");

            if (gradeObj != null) {
                if (gradeObj instanceof Number) {
                    grade = ((Number) gradeObj).doubleValue();
                } else if (gradeObj instanceof String) {
                    String gradeStr = (String) gradeObj;

                    if (gradeStr.trim().isEmpty() || "null".equalsIgnoreCase(gradeStr.trim())) {
                        grade = null;
                    } else {
                        try {
                            grade = Double.parseDouble(gradeStr.trim());
                        } catch (NumberFormatException e) {
                            System.err.println("Number format error: " + e.getMessage());
                            return ResponseEntity.badRequest()
                                    .body(Map.of("error", "Invalid grade format: '" + gradeStr + "'"));
                        }
                    }
                } else {
                    System.err.println("Unexpected grade object type: " + gradeObj.getClass());
                    return ResponseEntity.badRequest()
                            .body(Map.of("error", "Unsupported grade type: " + gradeObj.getClass().getSimpleName()));
                }
            } else {
            }

            // Validate grade range (allow null for removing grades)
            if (grade != null && (grade < 0 || grade > 100)) {
                System.err.println("Grade out of range: " + grade);
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Grade must be between 0 and 100, got: " + grade));
            }

            // Validate that the grade column exists
            if (!gradeServiceImpl.columnExists(columnId)) {
                System.err.println("Grade column does not exist: " + columnId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Grade column not found: " + columnId));
            }

            // Update the grade
            StudentGrade updated = gradeService.updateStudentGrade(studentId, columnId, grade);

            return ResponseEntity.ok(updated);

        } catch (RuntimeException e) {
            System.err.println("Runtime error updating grade: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Grade update failed: " + e.getMessage()));
        } catch (Exception e) {
            System.err.println("Unexpected error updating grade: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }

    /**
     * GET /api/students/{studentId}/final-grade/{courseId} : Calculate final grade for a student
     */
    @GetMapping("/students/{studentId}/final-grade/{courseId}")
    @PreAuthorize("hasRole('LECTURER') or (hasRole('STUDENT') and #studentId == authentication.name)")
    public ResponseEntity<?> getFinalGrade(
            @PathVariable String studentId,
            @PathVariable String courseId) {
        try {

            Double finalGrade = gradeService.calculateFinalGrade(studentId, courseId);
            String letterGrade = gradeService.calculateLetterGrade(finalGrade);

            Map<String, Object> result = Map.of(
                    "studentId", studentId,
                    "courseId", courseId,
                    "finalGrade", finalGrade,
                    "letterGrade", letterGrade,
                    "calculatedAt", LocalDateTime.now().toString()
            );
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            System.err.println("Error calculating final grade: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to calculate final grade: " + e.getMessage()));
        }
    }

    /**
     * POST /api/courses/{courseId}/grades/calculate-final : Recalculate all final grades for a course
     */
    @PostMapping("/courses/{courseId}/grades/calculate-final")
    @PreAuthorize("hasRole('LECTURER')")
    public ResponseEntity<?> calculateAllFinalGrades(@PathVariable String courseId) {
        try {

            gradeServiceImpl.recalculateAllGradesForCourse(courseId);

            return ResponseEntity.ok(Map.of(
                    "message", "Final grades recalculated successfully",
                    "courseId", courseId,
                    "timestamp", LocalDateTime.now().toString()
            ));
        } catch (Exception e) {
            System.err.println("Error recalculating final grades: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to recalculate grades: " + e.getMessage()));
        }
    }

    // ADMIN AND DEBUG ENDPOINTS

    /**
     * ADMIN ENDPOINT: Fix all incorrect grades in the database
     */
    @PostMapping("/admin/fix-all-grades")
    @PreAuthorize("hasRole('ADMIN') or hasRole('LECTURER')")
    public ResponseEntity<?> fixAllIncorrectGrades() {
        try {

            gradeServiceImpl.fixAllIncorrectGrades();

            return ResponseEntity.ok(Map.of(
                    "message", "All incorrect grades have been fixed successfully",
                    "status", "success",
                    "timestamp", LocalDateTime.now().toString()
            ));
        } catch (Exception e) {
            System.err.println("Error fixing grades: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fix grades: " + e.getMessage()));
        }
    }

    /**
     * ADMIN ENDPOINT: Recalculate grades for a specific course
     */
    @PostMapping("/admin/recalculate-course/{courseId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('LECTURER')")
    public ResponseEntity<?> recalculateCourseGrades(@PathVariable String courseId) {
        try {

            gradeServiceImpl.recalculateAllGradesForCourse(courseId);

            return ResponseEntity.ok(Map.of(
                    "message", "Course grades recalculated successfully",
                    "courseId", courseId,
                    "timestamp", LocalDateTime.now().toString()
            ));
        } catch (Exception e) {
            System.err.println("Error recalculating course grades: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to recalculate course grades: " + e.getMessage()));
        }
    }

    /**
     * DEBUG ENDPOINT: Get detailed grade information for a student
     */
    @GetMapping("/debug/student/{studentId}/course/{courseId}")
    @PreAuthorize("hasRole('LECTURER')")
    public ResponseEntity<?> debugStudentGrades(
            @PathVariable String studentId,
            @PathVariable String courseId) {
        try {

            // Get grade columns
            List<GradeColumn> columns = gradeService.getGradeColumnsByCourse(courseId);

            // Get student grades
            List<StudentGrade> courseGrades = gradeService.getGradesByCourse(courseId);
            StudentGrade studentGrade = courseGrades.stream()
                    .filter(sg -> sg.getStudentId().equals(studentId))
                    .findFirst()
                    .orElse(null);

            // Calculate final grade
            Double calculatedFinalGrade = gradeService.calculateFinalGrade(studentId, courseId);
            String calculatedLetterGrade = gradeService.calculateLetterGrade(calculatedFinalGrade);

            // Calculate total percentage for debugging
            double totalPercentage = columns.stream()
                    .mapToDouble(col -> col.getPercentage().doubleValue())
                    .sum();

            Map<String, Object> debug = new HashMap<>();
            debug.put("studentId", studentId);
            debug.put("courseId", courseId);
            debug.put("gradeColumns", columns);
            debug.put("totalPercentage", totalPercentage);
            debug.put("studentGradeRecord", studentGrade);
            debug.put("calculatedFinalGrade", calculatedFinalGrade);
            debug.put("calculatedLetterGrade", calculatedLetterGrade);
            debug.put("timestamp", LocalDateTime.now().toString());

            // Add validation checks
            List<String> issues = new ArrayList<>();
            List<String> warnings = new ArrayList<>();

            if (columns.isEmpty()) {
                issues.add("No grade columns found for course");
            }
            if (studentGrade == null) {
                issues.add("No grade record found for student");
            }
            if (studentGrade != null && studentGrade.getGrades().isEmpty()) {
                issues.add("Student has no grades entered");
            }
            if (totalPercentage > 100.0) {
                warnings.add("Total percentage exceeds 100% - grades will be normalized");
            }
            if (totalPercentage < 100.0) {
                warnings.add("Total percentage is less than 100%");
            }

            debug.put("issues", issues);
            debug.put("warnings", warnings);
            debug.put("hasIssues", !issues.isEmpty());
            debug.put("hasWarnings", !warnings.isEmpty());

            return ResponseEntity.ok(debug);
        } catch (Exception e) {
            System.err.println("Error debugging student grades: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Debug failed: " + e.getMessage()));
        }
    }

    /**
     * DEBUG ENDPOINT: Validate course grade configuration
     */
    @GetMapping("/debug/course/{courseId}/validation")
    @PreAuthorize("hasRole('LECTURER')")
    public ResponseEntity<?> validateCourse(@PathVariable String courseId) {
        try {

            List<GradeColumn> columns = gradeService.getGradeColumnsByCourse(courseId);
            List<StudentGrade> grades = gradeService.getGradesByCourse(courseId);

            // Calculate total percentage
            double totalPercentage = columns.stream()
                    .mapToDouble(GradeColumn::getPercentage)
                    .sum();

            // Validation checks
            List<String> issues = new ArrayList<>();
            List<String> warnings = new ArrayList<>();

            if (totalPercentage > 100) {
                warnings.add("Total percentage exceeds 100%: " + totalPercentage + "% - grades will be normalized");
            } else if (totalPercentage < 100) {
                warnings.add("Total percentage is less than 100%: " + totalPercentage + "%");
            }

            if (columns.isEmpty()) {
                issues.add("No grade columns found for course");
            }

            // Check for orphaned grades
            Set<String> validColumnIds = columns.stream()
                    .map(GradeColumn::getId)
                    .collect(Collectors.toSet());

            for (StudentGrade sg : grades) {
                for (String gradeColumnId : sg.getGrades().keySet()) {
                    if (!validColumnIds.contains(gradeColumnId)) {
                        issues.add("Student " + sg.getStudentId() +
                                " has grade for non-existent column: " + gradeColumnId);
                    }
                }
            }

            Map<String, Object> validation = new HashMap<>();
            validation.put("courseId", courseId);
            validation.put("totalColumns", columns.size());
            validation.put("totalStudentRecords", grades.size());
            validation.put("totalPercentage", totalPercentage);
            validation.put("percentageStatus", totalPercentage == 100 ? "perfect" :
                    totalPercentage > 100 ? "over" : "under");
            validation.put("gradeColumns", columns);
            validation.put("issues", issues);
            validation.put("warnings", warnings);
            validation.put("hasIssues", !issues.isEmpty());
            validation.put("hasWarnings", !warnings.isEmpty());
            validation.put("timestamp", LocalDateTime.now().toString());

            return ResponseEntity.ok(validation);
        } catch (Exception e) {
            System.err.println("Error validating course: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Validation failed: " + e.getMessage()));
        }
    }

    /**
     * ADMIN ENDPOINT: Clean up orphaned grades
     */
    @PostMapping("/admin/cleanup-orphaned-grades/{courseId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('LECTURER')")
    public ResponseEntity<?> cleanupOrphanedGrades(@PathVariable String courseId) {
        try {

            gradeServiceImpl.cleanupOrphanedGrades(courseId);

            return ResponseEntity.ok(Map.of(
                    "message", "Orphaned grades cleaned up successfully",
                    "courseId", courseId,
                    "timestamp", LocalDateTime.now().toString()
            ));
        } catch (Exception e) {
            System.err.println("Error cleaning up orphaned grades: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Cleanup failed: " + e.getMessage()));
        }
    }

    /**
     * Bulk operations placeholder
     */
    @PostMapping("/grades/bulk-update")
    @PreAuthorize("hasRole('LECTURER')")
    public ResponseEntity<?> bulkUpdateGrades(@RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(Map.of(
                "message", "Bulk update functionality not yet implemented",
                "status", "placeholder"
        ));
    }
}