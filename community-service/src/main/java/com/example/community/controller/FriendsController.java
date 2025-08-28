package com.example.community.controller;

import com.example.community.dto.*;
import com.example.community.service.FriendsService;
import com.example.common.service.UserService; // Add this import
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/friends")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class FriendsController {

    @Autowired
    private FriendsService friendsService;

    @Autowired
    private UserService userService; // Add this

    @GetMapping
    public ResponseEntity<List<UserDto>> getFriends(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).build();
        }
        String username = authentication.getName();
        String userId = userService.getUserByUsername(username).getId();
        return ResponseEntity.ok(friendsService.getFriends(userId));
    }

    @GetMapping("/suggestions")
    public ResponseEntity<List<UserDto>> getFriendSuggestions(Authentication authentication) {
        String username = authentication.getName();
        String userId = userService.getUserByUsername(username).getId();
        return ResponseEntity.ok(friendsService.getFriendSuggestions(userId));
    }

    @GetMapping("/requests")
    public ResponseEntity<List<UserDto>> getFriendRequests(Authentication authentication) {
        String username = authentication.getName();
        String userId = userService.getUserByUsername(username).getId();
        return ResponseEntity.ok(friendsService.getFriendRequests(userId));
    }

    @PostMapping("/request/{userId}")
    public ResponseEntity<Void> sendFriendRequest(@PathVariable String userId, Authentication authentication) {
        String username = authentication.getName();
        String currentUserId = userService.getUserByUsername(username).getId();
        friendsService.sendFriendRequest(currentUserId, userId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/accept/{userId}")
    public ResponseEntity<Void> acceptFriendRequest(@PathVariable String userId, Authentication authentication) {
        String username = authentication.getName();
        String currentUserId = userService.getUserByUsername(username).getId();
        friendsService.acceptFriendRequest(userId, currentUserId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/reject/{userId}")
    public ResponseEntity<Void> rejectFriendRequest(@PathVariable String userId, Authentication authentication) {
        String username = authentication.getName();
        String currentUserId = userService.getUserByUsername(username).getId();
        friendsService.rejectFriendRequest(userId, currentUserId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/remove/{userId}")
    public ResponseEntity<Void> removeFriend(@PathVariable String userId, Authentication authentication) {
        String username = authentication.getName();
        String currentUserId = userService.getUserByUsername(username).getId();
        friendsService.removeFriend(currentUserId, userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/status/{userId}")
    public ResponseEntity<FriendshipStatusDto> getFriendshipStatus(@PathVariable String userId, Authentication authentication) {
        String username = authentication.getName();
        String currentUserId = userService.getUserByUsername(username).getId();
        return ResponseEntity.ok(friendsService.getFriendshipStatus(currentUserId, userId));
    }

    @GetMapping("/activities")
    public ResponseEntity<List<ActivityDto>> getFriendsActivities(Authentication authentication) {
        String username = authentication.getName();
        String userId = userService.getUserByUsername(username).getId();
        return ResponseEntity.ok(friendsService.getFriendsActivities(userId));
    }

    @PostMapping("/dismiss-suggestion/{userId}")
    public ResponseEntity<Void> dismissSuggestion(@PathVariable String userId, Authentication authentication) {
        String username = authentication.getName();
        String currentUserId = userService.getUserByUsername(username).getId();
        friendsService.dismissSuggestion(currentUserId, userId);
        return ResponseEntity.ok().build();
    }
}