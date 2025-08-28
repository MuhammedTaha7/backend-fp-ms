package com.example.edusphere.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Document(collection = "events")
public class Event {

    @Id
    private String id;
    private String year;
    private String title;
    private String description;
    private String type; // "LECTURE", "LAB", etc.

    private LocalDate startDate;    // The date the recurrence begins
    private LocalDate endDate;      // The date the recurrence ends
    private DayOfWeek dayOfWeek;    // The day of the week this event occurs on (e.g., MONDAY)

    private LocalTime startTime;
    private LocalTime endTime;
    private String location;

    @Field("course_id")
    private String courseId;

    @Field("instructor_id")
    private String instructorId;

    @Field("learning_group_id")
    private String learningGroupId;
}