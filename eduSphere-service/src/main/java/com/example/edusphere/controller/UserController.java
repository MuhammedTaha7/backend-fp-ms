package com.example.edusphere.controller;

import com.example.common.dto.request.AdminCreateUserRequest;
import com.example.common.entity.UserEntity;
import com.example.common.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "false")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/role/{role}")
    public ResponseEntity<List<UserEntity>> getUsersByRole(@PathVariable String role) {
        List<UserEntity> users = userService.findUsersByRole(role);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserEntity> getUserById(@PathVariable String id) {
        UserEntity user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    @PostMapping("/by-ids")
    public ResponseEntity<List<UserEntity>> getUsersByIds(@RequestBody List<String> userIds) {
        List<UserEntity> users = userService.findUsersByIds(userIds);
        return ResponseEntity.ok(users);
    }

    @PostMapping("/admin-create")
    public ResponseEntity<UserEntity> createAdminUser(@RequestBody AdminCreateUserRequest request) {
        UserEntity createdUser = userService.createAdminUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<UserEntity> updateUser(@PathVariable String id, @RequestBody AdminCreateUserRequest request) {
        UserEntity updatedUser = userService.updateUser(id, request);
        return ResponseEntity.ok(updatedUser);
    }
}
