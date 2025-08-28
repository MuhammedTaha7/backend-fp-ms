package com.example.edusphere.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Data
@Document(collection = "student_requests")
public class StudentRequest {

    @Id
    private String id;

    @Field("sender_id")
    private String senderId;

    @Field("receiver_id")
    private String receiverId;

    private String subject;
    private String message;
    private String type;
    private String priority; // 'high', 'medium', 'low'
    private String status; // 'pending', 'approved', 'rejected', 'responded'
    private String response;

    @CreatedDate
    private LocalDateTime date;

    @Field("response_date")
    private LocalDateTime responseDate;
}