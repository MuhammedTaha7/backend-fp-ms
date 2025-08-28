package com.example.edusphere.controller;

import com.example.edusphere.dto.response.LecturerProfileDto;
import com.example.edusphere.dto.response.StudentProfileDto;
import com.example.edusphere.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping("/students/{id}")
    public ResponseEntity<StudentProfileDto> getStudentProfile(@PathVariable String id) {
        StudentProfileDto profile = profileService.getStudentProfile(id);
        return ResponseEntity.ok(profile);
    }

    @GetMapping("/lecturers/{id}")
    public ResponseEntity<LecturerProfileDto> getLecturerProfile(@PathVariable String id) {
        LecturerProfileDto profile = profileService.getLecturerProfile(id);
        return ResponseEntity.ok(profile);
    }
}