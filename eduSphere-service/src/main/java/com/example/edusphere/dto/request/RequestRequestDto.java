package com.example.edusphere.dto.request;

import lombok.Data;

@Data
public class RequestRequestDto {
    private String senderId; // The student's ID
    private String receiverId; // The lecturer's ID
    private String subject;
    private String message;
    private String type; // e.g., 'academic', 'financial', 'technical'
    private String priority; // 'high', 'medium', 'low'
}