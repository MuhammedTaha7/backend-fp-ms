package com.example.community.service;

import com.example.community.dto.UserDto;
import com.example.community.dto.request.UpdateProfileRequest;
import com.example.community.dto.request.ReportRequest;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;

public interface UsersService {
    UserDto getUserProfile(String userId);
    UserDto updateProfile(UpdateProfileRequest request, String userId);
    Map<String, String> uploadAvatar(MultipartFile file, String userId);
    void reportUser(String reportedUserId, ReportRequest request, String reporterId);
    List<UserDto> searchUsers(String query, String currentUserId); // 🆕 Add currentUserId to the method signature

}