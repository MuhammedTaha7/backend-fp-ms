package com.example.common.dto.request;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminCreateUserRequest {
private String id;
    // common
    private String name;
    private String email;
    private String role;
    private String phoneNumber;
    private String dateOfBirth;

    // for student
    private String department;
    private String academicYear;
    private String status;

    // for lecturer
    private String specialization;
    private String employmentType;
    private String experience;
    private Double rating;
}