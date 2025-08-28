package com.example.edusphere.dto.response;

import lombok.Data;

@Data
public class LecturerProfileDto {
    private String id;
    private String name;
    private String email;
    private String phoneNumber;
    private String profilePic;
    private String dateOfBirth;
    private String department;
    private String specialization;
    private String employmentType;
    private String experience;
    private Double rating;
    private Integer activeCourses;
}