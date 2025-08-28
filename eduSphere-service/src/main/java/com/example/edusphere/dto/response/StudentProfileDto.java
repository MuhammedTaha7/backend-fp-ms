package com.example.edusphere.dto.response;

import lombok.Data;

@Data
public class StudentProfileDto {
    private String id;
    private String name;
    private String email;
    private String phoneNumber;
    private String profilePic;
    private String dateOfBirth;
    private String academicYear;
    private String department;
    private String status;
    private Double gpa;
}