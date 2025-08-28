package com.example.common.service;

import com.example.common.dto.request.AdminCreateUserRequest;
import com.example.common.dto.request.LoginRequest;
import com.example.common.dto.request.RegisterRequest;
import com.example.common.dto.response.LoginResponse;
import com.example.common.entity.UserEntity;
import lombok.NonNull;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface UserService {
    LoginResponse authenticateUser(@NonNull LoginRequest loginRequest);
    void registerUser(@NonNull RegisterRequest registerRequest);

    String getUserIdByEmail(@NonNull String email);

    UserEntity getUserByEmail(String email);

    UserEntity getUserById(String userId);

    List<UserEntity> findUsersByRole(String role);

    List<UserEntity> findUsersByIds(List<String> userIds);

    UserEntity createAdminUser(AdminCreateUserRequest request);

    UserEntity updateUser(String userId, AdminCreateUserRequest request);

    UserEntity getUserByUsername(String username);

    UserEntity findByUsername(String authName);
}