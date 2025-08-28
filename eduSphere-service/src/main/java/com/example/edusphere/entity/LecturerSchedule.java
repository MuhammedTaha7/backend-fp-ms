package com.example.edusphere.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@Document(collection = "lecturer_schedules")
public class LecturerSchedule {

    @Id
    private String id;

    @Field("lecturer_id")
    private String lecturerId;

    private String day;
    private String startTime;
    private String endTime;
    private String availability; // 'available', 'busy', 'preferred'
    private String notes;
}