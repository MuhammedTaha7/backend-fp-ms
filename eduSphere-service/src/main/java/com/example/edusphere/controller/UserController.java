package com.example.edusphere.controller;

import com.example.common.dto.request.AdminCreateUserRequest;
import com.example.common.entity.UserEntity;
import com.example.common.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing Users.
 * This class exposes API endpoints for user-related operations.
 */
@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "false")
public class UserController {

    private final UserService userService;

    // Constructor-based dependency injection
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * GET /api/users/role/{role} : Get all users with a specific role.
     * We use this to get all lecturers by calling /api/users/role/1200.
     *
     * @param role The role to filter by (e.g., "1100", "1200", "1300").
     * @return A list of users with the specified role.
     */
    @GetMapping("/role/{role}")
    public ResponseEntity<List<UserEntity>> getUsersByRole(@PathVariable String role) {
        List<UserEntity> users = userService.findUsersByRole(role);
        return ResponseEntity.ok(users);
    }

    /**
     * GET /api/users/{id} : Get a single user by their ID.
     *
     * @param id The ID of the user to retrieve.
     * @return The user entity.
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserEntity> getUserById(@PathVariable String id) {
        UserEntity user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    /**
     * POST /api/users/by-ids : Get a list of users by their IDs.
     * We use a POST request because sending a long list of IDs is cleaner in a request body
     * than in a URL.
     *
     * @param userIds A JSON array of user IDs in the request body.
     * @return A list of user details.
     */
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
