package com.example.edusphere.service;

import com.example.edusphere.dto.response.LecturerProfileDto;
import com.example.edusphere.dto.response.StudentProfileDto;
import org.springframework.stereotype.Service;

@Service
public interface ProfileService {
    StudentProfileDto getStudentProfile(String id);
    LecturerProfileDto getLecturerProfile(String id);
}