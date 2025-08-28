package com.example.edusphere.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.ArrayList;
import java.util.List;

@Data
@Document(collection = "courses")
public class Course {

    @Id
    private String id;

    private String name;
    private String code;
    private String description;
    private String imageUrl;
    private String academicYear;
    private String semester;
    private Integer year;
    private Boolean selectable;

    @Field("lecturer_id")
    private String lecturerId;

    private List<YearlyEnrollment> enrollments = new ArrayList<>();
    private String department;
    private int credits;

    // --- NEW FIELDS TO ADD ---

    /**
     * The primary language of the course. e.g., "English".
     */
    private String language;

    /**
     * Course completion percentage (0-100).
     */
    private Integer progress;

    /**
     * A brief description of course prerequisites. e.g., "None".
     */
    private String prerequisites;

    /**
     * Information about the final exam. e.g., "TBD" or "2025-06-15".
     */
    private String finalExam;
}