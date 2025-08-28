package com.example.edusphere.service.impl;

import com.example.edusphere.entity.GradeColumn;
import com.example.edusphere.entity.StudentGrade;
import com.example.edusphere.repository.GradeColumnRepository;
import com.example.edusphere.repository.StudentGradeRepository;
import com.example.edusphere.service.GradeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class GradeServiceImpl implements GradeService {

    private final GradeColumnRepository gradeColumnRepository;
    private final StudentGradeRepository studentGradeRepository;

    public GradeServiceImpl(GradeColumnRepository gradeColumnRepository,
                            StudentGradeRepository studentGradeRepository) {
        this.gradeColumnRepository = gradeColumnRepository;
        this.studentGradeRepository = studentGradeRepository;
    }

    @Override
    public List<GradeColumn> getGradeColumnsByCourse(String courseId) {
        try {
            List<GradeColumn> columns = gradeColumnRepository.findByCourseIdAndIsActiveTrue(courseId);
            System.out.println("📊 Found " + columns.size() + " active grade columns for course: " + courseId);
            return columns;
        } catch (Exception e) {
            System.err.println("❌ Error fetching grade columns: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public GradeColumn createGradeColumn(GradeColumn gradeColumn) {
        System.out.println("➕ Creating grade column: " + gradeColumn.getName() + " (" + gradeColumn.getPercentage() + "%)");

        // Validate required fields
        if (gradeColumn.getName() == null || gradeColumn.getName().trim().isEmpty()) {
            throw new RuntimeException("Grade column name is required");
        }
        if (gradeColumn.getPercentage() == null || gradeColumn.getPercentage() <= 0) {
            throw new RuntimeException("Valid percentage is required");
        }
        if (gradeColumn.getCourseId() == null || gradeColumn.getCourseId().trim().isEmpty()) {
            throw new RuntimeException("Course ID is required");
        }

        // Validate percentage doesn't exceed 100%
        if (!validateGradeColumnPercentage(gradeColumn.getCourseId(),
                gradeColumn.getPercentage(), null)) {
            throw new RuntimeException("Total percentage would exceed 100%");
        }

        // Set display order
        List<GradeColumn> existingColumns = gradeColumnRepository
                .findByCourseIdOrderByDisplayOrderAsc(gradeColumn.getCourseId());
        gradeColumn.setDisplayOrder(existingColumns.size() + 1);

        // Set default values if not provided
        if (gradeColumn.getIsActive() == null) {
            gradeColumn.setIsActive(true);
        }
        if (gradeColumn.getType() == null || gradeColumn.getType().trim().isEmpty()) {
            gradeColumn.setType("assignment");
        }

        GradeColumn savedColumn = gradeColumnRepository.save(gradeColumn);
        System.out.println("✅ Created grade column with ID: " + savedColumn.getId());

        // Recalculate all student grades for this course after adding new column
        recalculateAllGradesForCourse(gradeColumn.getCourseId());

        return savedColumn;
    }

    @Override
    public GradeColumn updateGradeColumn(String columnId, GradeColumn updates) {
        System.out.println("🔄 Updating grade column: " + columnId);

        GradeColumn existingColumn = gradeColumnRepository.findById(columnId)
                .orElseThrow(() -> new RuntimeException("Grade column not found with ID: " + columnId));

        // Validate percentage if being updated
        if (updates.getPercentage() != null) {
            if (!validateGradeColumnPercentage(existingColumn.getCourseId(),
                    updates.getPercentage(), columnId)) {
                throw new RuntimeException("Total percentage would exceed 100%");
            }
        }

        // Update fields safely
        if (updates.getName() != null && !updates.getName().trim().isEmpty()) {
            existingColumn.setName(updates.getName().trim());
        }
        if (updates.getType() != null && !updates.getType().trim().isEmpty()) {
            existingColumn.setType(updates.getType().trim());
        }
        if (updates.getPercentage() != null && updates.getPercentage() > 0) {
            existingColumn.setPercentage(updates.getPercentage());
        }
        if (updates.getMaxPoints() != null && updates.getMaxPoints() > 0) {
            existingColumn.setMaxPoints(updates.getMaxPoints());
        }
        if (updates.getDescription() != null) {
            existingColumn.setDescription(updates.getDescription());
        }

        GradeColumn savedColumn = gradeColumnRepository.save(existingColumn);
        System.out.println("✅ Updated grade column successfully");

        // Recalculate all student grades for this course after updating column
        recalculateAllGradesForCourse(existingColumn.getCourseId());

        return savedColumn;
    }

    @Override
    public void deleteGradeColumn(String columnId) {
        System.out.println("🗑️ Deleting grade column: " + columnId);

        GradeColumn column = gradeColumnRepository.findById(columnId)
                .orElseThrow(() -> new RuntimeException("Grade column not found with ID: " + columnId));

        String courseId = column.getCourseId();

        // Remove this column's grades from all students
        List<StudentGrade> studentGrades = studentGradeRepository.findByCourseId(courseId);
        for (StudentGrade studentGrade : studentGrades) {
            studentGrade.removeGrade(columnId);
            studentGradeRepository.save(studentGrade);
        }

        // Delete the column
        gradeColumnRepository.deleteById(columnId);
        System.out.println("✅ Deleted grade column successfully");

        // Recalculate all final grades after deleting column
        recalculateAllGradesForCourse(courseId);
    }

    @Override
    public List<StudentGrade> getGradesByCourse(String courseId) {
        try {
            List<StudentGrade> grades = studentGradeRepository.findByCourseId(courseId);
            System.out.println("📊 Found " + grades.size() + " student grade records for course: " + courseId);
            return grades;
        } catch (Exception e) {
            System.err.println("❌ Error fetching grades: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public StudentGrade updateStudentGrade(String studentId, String columnId, Double grade) {
        System.out.println("🔧 === UPDATING STUDENT GRADE ===");
        System.out.println("Student: " + studentId);
        System.out.println("Column: " + columnId);
        System.out.println("Grade: " + grade);

        try {
            // Get the column to find the course and validate it exists
            GradeColumn column = gradeColumnRepository.findById(columnId)
                    .orElseThrow(() -> new RuntimeException("Grade column not found with ID: " + columnId));

            if (!column.getIsActive()) {
                throw new RuntimeException("Cannot update grade for inactive column: " + columnId);
            }

            String courseId = column.getCourseId();
            System.out.println("Course: " + courseId);

            // Validate grade value
            if (grade != null && (grade < 0 || grade > 100)) {
                throw new RuntimeException("Grade must be between 0 and 100, got: " + grade);
            }

            // FIXED: Handle duplicates properly
            StudentGrade studentGrade = findOrCreateStudentGradeRecord(studentId, courseId);

            // Update the specific grade
            System.out.println("📊 Before update - Student grades: " + studentGrade.getGrades());

            if (grade == null) {
                studentGrade.removeGrade(columnId);
                System.out.println("🗑️ Removed grade for column: " + columnId);
            } else {
                studentGrade.setGrade(columnId, grade);
                System.out.println("✏️ Set grade " + grade + " for column: " + columnId);
            }

            System.out.println("📊 After update - Student grades: " + studentGrade.getGrades());

            // FIXED: Calculate final grade using the UPDATED record, not fetching from database again
            Double finalGrade = calculateFinalGradeFromRecord(studentGrade, courseId);
            String letterGrade = calculateLetterGrade(finalGrade);

            // Update the final grades
            studentGrade.setFinalGrade(finalGrade);
            studentGrade.setFinalLetterGrade(letterGrade);

            System.out.println("🎯 Calculated final grade: " + finalGrade + "% (" + letterGrade + ")");

            // Save the updated record
            StudentGrade savedGrade = studentGradeRepository.save(studentGrade);
            System.out.println("💾 Saved grade record successfully");
            System.out.println("✅ === GRADE UPDATE COMPLETE ===");

            return savedGrade;

        } catch (Exception e) {
            System.err.println("❌ Error updating student grade: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to update student grade: " + e.getMessage());
        }
    }

    /**
     * FIXED: Calculate final grade from an existing StudentGrade record
     * Now properly handles total percentages > 100% by normalizing to 100%
     */
    private Double calculateFinalGradeFromRecord(StudentGrade studentGrade, String courseId) {
        System.out.println("🧮 === CALCULATING FINAL GRADE FROM RECORD ===");
        System.out.println("Student: " + studentGrade.getStudentId());
        System.out.println("Course: " + courseId);

        try {
            // Get active grade columns for the course
            List<GradeColumn> columns = getGradeColumnsByCourse(courseId);
            System.out.println("📊 Active grade columns: " + columns.size());

            if (columns.isEmpty()) {
                System.out.println("❌ No grade columns found, returning 0");
                return 0.0;
            }

            // Use the grades from the provided record (already updated in memory)
            Map<String, Double> grades = studentGrade.getGrades();
            if (grades == null || grades.isEmpty()) {
                System.out.println("❌ No grades found for student, returning 0");
                return 0.0;
            }

            System.out.println("🔍 Student grades (from updated record): " + grades);

            // FIXED: Calculate total percentage and normalize if > 100%
            double totalPercentageOfAllColumns = columns.stream()
                    .mapToDouble(col -> col.getPercentage().doubleValue())
                    .sum();

            System.out.println("📐 Total percentage of all columns: " + totalPercentageOfAllColumns + "%");

            double totalWeightedScore = 0.0;
            double totalPercentageOfGradedItems = 0.0;
            int gradedItemsCount = 0;

            System.out.println("🔍 === GRADE BREAKDOWN ===");

            // Calculate weighted score for each grade column
            for (GradeColumn column : columns) {
                String columnId = column.getId();
                Double grade = grades.get(columnId);
                double columnPercentage = column.getPercentage().doubleValue();

                System.out.println("📋 " + column.getName() + " (" + columnPercentage + "%):");

                if (grade != null && grade >= 0) {
                    // FIXED: Normalize weight if total > 100%
                    double normalizedWeight = totalPercentageOfAllColumns > 100.0
                            ? (columnPercentage / totalPercentageOfAllColumns) * 100.0
                            : columnPercentage;

                    double weightContribution = (grade * normalizedWeight) / 100.0;

                    totalWeightedScore += weightContribution;
                    totalPercentageOfGradedItems += normalizedWeight;
                    gradedItemsCount++;

                    System.out.println("   ✅ Grade: " + grade +
                            " → Normalized Weight: " + String.format("%.2f", normalizedWeight) + "%" +
                            " → Contribution: " + String.format("%.2f", weightContribution) + " points");
                } else {
                    System.out.println("   ⚪ No grade entered (skipping)");
                }
            }

            System.out.println("📊 === CALCULATION SUMMARY ===");
            System.out.println("Total weighted score: " + String.format("%.2f", totalWeightedScore));
            System.out.println("Total percentage of graded items: " + String.format("%.2f", totalPercentageOfGradedItems) + "%");
            System.out.println("Number of graded items: " + gradedItemsCount);

            // Calculate final grade
            double finalGrade;

            if (totalPercentageOfGradedItems == 0) {
                finalGrade = 0.0;
                System.out.println("❌ No grades entered → Final: 0%");
            } else {
                // FIXED: Always calculate as percentage of total possible
                finalGrade = totalWeightedScore;
                System.out.println("✅ Final grade calculation: " + String.format("%.2f", finalGrade) + "%");

                if (totalPercentageOfAllColumns > 100.0) {
                    System.out.println("⚖️ Note: Grades were normalized due to total percentage > 100%");
                }
            }

            // Ensure grade is within valid bounds and round to 2 decimal places
            finalGrade = Math.max(0.0, Math.min(100.0, finalGrade));
            finalGrade = Math.round(finalGrade * 100.0) / 100.0;

            System.out.println("🎯 FINAL CALCULATED GRADE: " + finalGrade + "%");
            System.out.println("✅ === CALCULATION COMPLETE ===");

            return finalGrade;

        } catch (Exception e) {
            System.err.println("❌ Error calculating final grade from record: " + e.getMessage());
            e.printStackTrace();
            return 0.0;
        }
    }

    /**
     * FIXED: Properly handle duplicate records and always return a single record
     */
    private StudentGrade findOrCreateStudentGradeRecord(String studentId, String courseId) {
        System.out.println("🔍 Finding or creating grade record for student: " + studentId + ", course: " + courseId);

        // First, check for duplicates and clean them up
        List<StudentGrade> existingRecords = studentGradeRepository.findAllByStudentIdAndCourseId(studentId, courseId);

        if (existingRecords.isEmpty()) {
            // No record exists, create new one
            System.out.println("➕ Creating new grade record");
            StudentGrade newRecord = new StudentGrade();
            newRecord.setStudentId(studentId);
            newRecord.setCourseId(courseId);
            newRecord.setGrades(new HashMap<>());
            return newRecord;
        } else if (existingRecords.size() == 1) {
            // Single record exists, return it
            System.out.println("🔍 Found existing grade record");
            return existingRecords.get(0);
        } else {
            // Multiple records exist - merge and clean up duplicates
            System.out.println("⚠️ Found " + existingRecords.size() + " duplicate records, merging...");
            return mergeDuplicateRecords(existingRecords);
        }
    }

    /**
     * Merge duplicate student grade records and keep the most complete one
     */
    private StudentGrade mergeDuplicateRecords(List<StudentGrade> duplicates) {
        if (duplicates.isEmpty()) {
            throw new RuntimeException("No records to merge");
        }

        // Sort by last modified date (newest first)
        duplicates.sort((a, b) -> {
            if (a.getUpdatedAt() == null && b.getUpdatedAt() == null) return 0;
            if (a.getUpdatedAt() == null) return 1;
            if (b.getUpdatedAt() == null) return -1;
            return b.getUpdatedAt().compareTo(a.getUpdatedAt());
        });

        // Take the most recent record as base
        StudentGrade primaryRecord = duplicates.get(0);
        System.out.println("📋 Using primary record ID: " + primaryRecord.getId());

        // Merge grades from all records (prefer non-null values)
        Map<String, Double> mergedGrades = new HashMap<>();

        for (StudentGrade record : duplicates) {
            if (record.getGrades() != null) {
                for (Map.Entry<String, Double> entry : record.getGrades().entrySet()) {
                    if (entry.getValue() != null) {
                        // If we already have a grade for this column, keep the most recent one
                        if (!mergedGrades.containsKey(entry.getKey()) ||
                                record.getId().equals(primaryRecord.getId())) {
                            mergedGrades.put(entry.getKey(), entry.getValue());
                        }
                    }
                }
            }
        }

        primaryRecord.setGrades(mergedGrades);

        // Delete duplicate records (keep only the primary)
        for (int i = 1; i < duplicates.size(); i++) {
            StudentGrade duplicate = duplicates.get(i);
            System.out.println("🗑️ Deleting duplicate record ID: " + duplicate.getId());
            studentGradeRepository.deleteById(duplicate.getId());
        }

        System.out.println("✅ Merged grades: " + mergedGrades);
        return primaryRecord;
    }

    @Override
    public void deleteStudentGrades(String studentId, String courseId) {
        System.out.println("🗑️ Deleting all grades for student: " + studentId + " in course: " + courseId);
        studentGradeRepository.deleteByStudentIdAndCourseId(studentId, courseId);
        System.out.println("✅ Deleted student grades successfully");
    }

    // Keep the original calculateFinalGrade method for other uses (like bulk recalculation)
    @Override
    public Double calculateFinalGrade(String studentId, String courseId) {
        System.out.println("🧮 === CALCULATING FINAL GRADE ===");
        System.out.println("Student: " + studentId);
        System.out.println("Course: " + courseId);

        try {
            // Get active grade columns for the course
            List<GradeColumn> columns = getGradeColumnsByCourse(courseId);
            System.out.println("📊 Active grade columns: " + columns.size());

            if (columns.isEmpty()) {
                System.out.println("❌ No grade columns found, returning 0");
                return 0.0;
            }

            // Get student's grade record - FIXED to handle duplicates
            StudentGrade studentGrade = findOrCreateStudentGradeRecord(studentId, courseId);

            // Use the new method to calculate from the record
            return calculateFinalGradeFromRecord(studentGrade, courseId);

        } catch (Exception e) {
            System.err.println("❌ Error calculating final grade: " + e.getMessage());
            e.printStackTrace();
            return 0.0;
        }
    }

    @Override
    public String calculateLetterGrade(Double percentage) {
        if (percentage == null || percentage < 0) return "F";

        if (percentage >= 97) return "A+";
        else if (percentage >= 93) return "A";
        else if (percentage >= 90) return "A-";
        else if (percentage >= 87) return "B+";
        else if (percentage >= 83) return "B";
        else if (percentage >= 80) return "B-";
        else if (percentage >= 77) return "C+";
        else if (percentage >= 73) return "C";
        else if (percentage >= 70) return "C-";
        else if (percentage >= 67) return "D+";
        else if (percentage >= 63) return "D";
        else if (percentage >= 60) return "D-";
        else return "F";
    }

    @Override
    public boolean validateGradeColumnPercentage(String courseId, Integer percentage, String excludeColumnId) {
        if (percentage == null || percentage < 0 || percentage > 100) {
            return false;
        }

        List<GradeColumn> existingColumns = getGradeColumnsByCourse(courseId);
        int totalPercentage = existingColumns.stream()
                .filter(col -> excludeColumnId == null || !col.getId().equals(excludeColumnId))
                .mapToInt(GradeColumn::getPercentage)
                .sum();

        boolean isValid = (totalPercentage + percentage) <= 100;
        System.out.println("📊 Percentage validation: " + totalPercentage + " + " + percentage + " = " +
                (totalPercentage + percentage) + "% (valid: " + isValid + ")");

        return isValid;
    }

    /**
     * Helper method to recalculate all final grades for a course
     * This ensures consistency when grade columns are modified
     */
    public void recalculateAllGradesForCourse(String courseId) {
        System.out.println("🔄 === RECALCULATING ALL GRADES FOR COURSE ===");
        System.out.println("Course: " + courseId);

        try {
            List<StudentGrade> studentGrades = studentGradeRepository.findByCourseId(courseId);
            System.out.println("👥 Found " + studentGrades.size() + " student records");

            int updatedCount = 0;

            // Get unique student IDs to handle duplicates
            Set<String> processedStudents = new HashSet<>();

            for (StudentGrade studentGrade : studentGrades) {
                String studentId = studentGrade.getStudentId();

                // Skip if we've already processed this student (handles duplicates)
                if (processedStudents.contains(studentId)) {
                    continue;
                }
                processedStudents.add(studentId);

                try {
                    Double newFinalGrade = calculateFinalGrade(studentId, courseId);
                    String newLetterGrade = calculateLetterGrade(newFinalGrade);

                    // Find the most recent record for this student
                    StudentGrade currentRecord = findOrCreateStudentGradeRecord(studentId, courseId);

                    // Only update if values changed
                    if (!Objects.equals(currentRecord.getFinalGrade(), newFinalGrade) ||
                            !Objects.equals(currentRecord.getFinalLetterGrade(), newLetterGrade)) {

                        currentRecord.setFinalGrade(newFinalGrade);
                        currentRecord.setFinalLetterGrade(newLetterGrade);

                        studentGradeRepository.save(currentRecord);
                        updatedCount++;

                        System.out.println("✅ Updated student " + studentId +
                                ": " + newFinalGrade + "% (" + newLetterGrade + ")");
                    }
                } catch (Exception e) {
                    System.err.println("❌ Error updating grades for student " + studentId + ": " + e.getMessage());
                }
            }

            System.out.println("🎉 Recalculation complete: " + updatedCount + "/" +
                    processedStudents.size() + " records updated");

        } catch (Exception e) {
            System.err.println("❌ Error during bulk recalculation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Admin method to fix all existing incorrect grades and clean up duplicates
     */
    public void fixAllIncorrectGrades() {
        System.out.println("🔧 === FIXING ALL INCORRECT GRADES ===");

        try {
            // Get all unique course IDs
            List<String> allCourseIds = studentGradeRepository.findAll()
                    .stream()
                    .map(StudentGrade::getCourseId)
                    .distinct()
                    .collect(Collectors.toList());

            System.out.println("📚 Found " + allCourseIds.size() + " courses with grades");

            for (String courseId : allCourseIds) {
                System.out.println("🔄 Processing course: " + courseId);
                cleanupDuplicatesForCourse(courseId);
                recalculateAllGradesForCourse(courseId);
            }

            System.out.println("🎉 COMPLETED: Fixed all grades for " + allCourseIds.size() + " courses");

        } catch (Exception e) {
            System.err.println("❌ Error fixing all grades: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to fix all grades: " + e.getMessage());
        }
    }

    /**
     * Clean up duplicate records for a specific course
     */
    private void cleanupDuplicatesForCourse(String courseId) {
        System.out.println("🧹 Cleaning up duplicates for course: " + courseId);

        List<StudentGrade> allGrades = studentGradeRepository.findByCourseId(courseId);
        Map<String, List<StudentGrade>> groupedByStudent = allGrades.stream()
                .collect(Collectors.groupingBy(StudentGrade::getStudentId));

        int duplicatesFixed = 0;
        for (Map.Entry<String, List<StudentGrade>> entry : groupedByStudent.entrySet()) {
            if (entry.getValue().size() > 1) {
                System.out.println("🔧 Merging " + entry.getValue().size() + " records for student: " + entry.getKey());
                mergeDuplicateRecords(entry.getValue());
                duplicatesFixed++;
            }
        }

        System.out.println("✅ Fixed " + duplicatesFixed + " duplicate student records");
    }

    /**
     * Utility methods for debugging and validation
     */
    public boolean columnExists(String columnId) {
        return gradeColumnRepository.existsById(columnId);
    }

    public void cleanupOrphanedGrades(String courseId) {
        System.out.println("🧹 Cleaning up orphaned grades for course: " + courseId);

        try {
            // Get valid column IDs
            List<GradeColumn> validColumns = getGradeColumnsByCourse(courseId);
            Set<String> validColumnIds = validColumns.stream()
                    .map(GradeColumn::getId)
                    .collect(Collectors.toSet());

            System.out.println("📊 Valid columns: " + validColumnIds);

            List<StudentGrade> studentGrades = studentGradeRepository.findByCourseId(courseId);
            int cleanedCount = 0;

            for (StudentGrade sg : studentGrades) {
                Map<String, Double> originalGrades = new HashMap<>(sg.getGrades());
                Map<String, Double> cleanedGrades = new HashMap<>();

                // Keep only grades for valid columns
                for (Map.Entry<String, Double> entry : originalGrades.entrySet()) {
                    if (validColumnIds.contains(entry.getKey())) {
                        cleanedGrades.put(entry.getKey(), entry.getValue());
                    } else {
                        System.out.println("🗑️ Removing orphaned grade: " + entry.getKey() +
                                " for student " + sg.getStudentId());
                    }
                }

                // Update if grades were cleaned
                if (!originalGrades.equals(cleanedGrades)) {
                    sg.setGrades(cleanedGrades);

                    // Recalculate final grade with cleaned data
                    Double finalGrade = calculateFinalGrade(sg.getStudentId(), courseId);
                    sg.setFinalGrade(finalGrade);
                    sg.setFinalLetterGrade(calculateLetterGrade(finalGrade));

                    studentGradeRepository.save(sg);
                    cleanedCount++;
                }
            }

            System.out.println("✅ Cleanup complete: " + cleanedCount + " records cleaned");

        } catch (Exception e) {
            System.err.println("❌ Error during cleanup: " + e.getMessage());
            e.printStackTrace();
        }
    }
}