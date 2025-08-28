package com.example.community.controller;

import com.example.community.service.UsersService;
import com.example.common.service.UserService; // Add this import
import com.example.community.dto.UserDto;
import com.example.community.dto.request.UpdateProfileRequest;
import com.example.community.dto.request.ReportRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.Authentication;


@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class UsersController {

    @Autowired
    private UsersService usersService;

    @Autowired
    private UserService userService; // Add this

    @GetMapping("/profile/{userId}")
    public ResponseEntity<UserDto> getUserProfile(@PathVariable String userId) {
        UserDto user = usersService.getUserProfile(userId);
        return ResponseEntity.ok(user);
    }

    @PutMapping("/profile")
    public ResponseEntity<UserDto> updateProfile(
            @RequestBody UpdateProfileRequest request,
            Authentication authentication) {
        String username = authentication.getName();
        String userId = userService.getUserByUsername(username).getId();
        UserDto user = usersService.updateProfile(request, userId);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/search")
    public ResponseEntity<List<UserDto>> searchUsers(
            @RequestParam String q,
            Authentication authentication) {

        String username = authentication.getName();
        String currentUserId = userService.getUserByUsername(username).getId();
        List<UserDto> users = usersService.searchUsers(q, currentUserId); // 🆕 Pass the currentUserId here
        return ResponseEntity.ok(users);
    }

    @PostMapping("/upload-avatar")
    public ResponseEntity<Map<String, String>> uploadAvatar(
            @RequestParam("avatar") MultipartFile file,
            Authentication authentication) {
        String username = authentication.getName();
        String userId = userService.getUserByUsername(username).getId();
        Map<String, String> result = usersService.uploadAvatar(file, userId);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/report/{userId}")
    public ResponseEntity<Void> reportUser(
            @PathVariable String userId,
            @RequestBody ReportRequest request,
            Authentication authentication) {
        String username = authentication.getName();
        String reporterId = userService.getUserByUsername(username).getId();
        usersService.reportUser(userId, request, reporterId);
        return ResponseEntity.ok().build();
    }
}