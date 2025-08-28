package com.example.community.service.impl;

import com.example.common.entity.UserEntity; // Main project
import com.example.common.repository.UserRepository; // Main project
import com.example.community.service.UsersService;
import com.example.community.service.FileStorageService;
import com.example.community.dto.UserDto;
import com.example.community.dto.request.UpdateProfileRequest;
import com.example.community.dto.request.ReportRequest;
import com.example.community.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class UsersServiceImpl implements UsersService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private UserMapper userMapper;

    @Override
    public UserDto getUserProfile(String userId) {
        UserEntity user = getUserById(userId);
        return userMapper.toDto(user);
    }

    @Override
    public UserDto updateProfile(UpdateProfileRequest request, String userId) {
        UserEntity user = getUserById(userId);

        user.setName(request.getName());
        user.setTitle(request.getTitle());
        user.setBio(request.getBio());
        user.setWebsite(request.getWebsite());
        user.setCoverPic(request.getCoverPic());

        if (request.getSocialLinks() != null) {
            user.setSocialLinks(request.getSocialLinks());
        }

        user.setUpdatedAt(LocalDateTime.now());

        UserEntity savedUser = userRepository.save(user);
        return userMapper.toDto(savedUser);
    }

    @Override
    public List<UserDto> searchUsers(String query, String currentUserId) { // 🆕 Updated method signature
        List<UserEntity> foundUsers = userRepository.findByNameContainingIgnoreCase(query);

        List<UserDto> userDtos = foundUsers.stream()
                // The currentUserId is now a parameter, so the code is correct
                .filter(user -> !user.getId().equals(currentUserId))
                .map(userMapper::toDto)
                .collect(Collectors.toList());

        return userDtos;
    }

    @Override
    public Map<String, String> uploadAvatar(MultipartFile file, String userId) {
        UserEntity user = getUserById(userId);

        // Validate file type
        if (!file.getContentType().startsWith("image/")) {
            throw new RuntimeException("Only image files are allowed");
        }

        // Store file
        String imageUrl = fileStorageService.storeFile(file, "avatars");

        // Delete old avatar if exists
        if (user.getProfilePic() != null) {
            fileStorageService.deleteFile(user.getProfilePic());
        }

        // Update user profile
        user.setProfilePic(imageUrl);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        Map<String, String> result = new HashMap<>();
        result.put("url", imageUrl);
        result.put("message", "Avatar uploaded successfully");

        return result;
    }

    @Override
    public void reportUser(String reportedUserId, ReportRequest request, String reporterId) {
        UserEntity reportedUser = getUserById(reportedUserId);
        UserEntity reporter = getUserById(reporterId);

        // Log the report (you might want to create a Report entity for this)
        System.out.println("User " + reporter.getUsername() + " reported user " +
                reportedUser.getUsername() + " for: " + request.getReason());

        // Here you could:
        // 1. Save to a reports collection
        // 2. Send notification to admins
        // 3. Implement automated moderation logic

        // For now, just log it
    }

    private UserEntity getUserById(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}