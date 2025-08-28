package com.example.edusphere.entity;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Data
@Document(collection = "submissions")
public class Submission {

    @Id
    private String id;

    @Field("course_id")
    private String courseId;

    @Field("assignment_id")
    private String assignmentId; // To link to a specific assignment

    @Field("student_id")
    private String studentId;

    private Integer grade; // The grade or score for the submission
    private String feedback; // Feedback from the lecturer
    private String fileUrl; // A URL to the submitted file

    @CreatedDate
    private LocalDateTime submittedAt;
}
