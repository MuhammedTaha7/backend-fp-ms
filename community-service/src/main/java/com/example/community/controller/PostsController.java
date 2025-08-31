package com.example.community.controller;

import com.example.community.service.PostsService;
import com.example.community.service.FriendsService;
import com.example.common.service.UserService; // Add this import
import com.example.community.dto.PostDto;
import com.example.community.dto.CommentDto;
import com.example.community.dto.UserDto;
import com.example.community.dto.request.CreatePostRequest;
import com.example.community.dto.request.CreateCommentRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/community/posts")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "false")
public class PostsController {

    @Autowired
    private PostsService postsService;

    @Autowired
    private FriendsService friendsService;

    @Autowired
    private UserService userService; // Add this

    @GetMapping("/feed")
    public ResponseEntity<List<PostDto>> getFeed(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).build();
        }

        String username = authentication.getName();
        String currentUserId = userService.getUserByUsername(username).getId();

        // Get friend IDs automatically
        List<String> friendIds = new ArrayList<>();
        try {
            List<UserDto> friends = friendsService.getFriends(currentUserId);
            friendIds = friends.stream()
                    .map(UserDto::getId)
                    .collect(Collectors.toList());
        } catch (Exception ignored) {}

        List<PostDto> posts = postsService.getFeed(currentUserId, friendIds);
        return ResponseEntity.ok(posts);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PostDto>> getUserPosts(@PathVariable String userId) {
        List<PostDto> posts = postsService.getUserPosts(userId);
        return ResponseEntity.ok(posts);
    }

    @PostMapping
    public ResponseEntity<PostDto> createPost(@RequestBody CreatePostRequest request, Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).build();
        }

        String username = authentication.getName();
        String userId = userService.getUserByUsername(username).getId();
        PostDto post = postsService.createPost(request, userId);
        return ResponseEntity.ok(post);
    }

    @PutMapping("/{postId}/like")
    public ResponseEntity<PostDto> toggleLike(
            @PathVariable String postId,
            @RequestParam(required = false) String userId,
            Authentication authentication) {

        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).build();
        }

        String username = authentication.getName();
        String currentUserId = userService.getUserByUsername(username).getId();
        PostDto post = postsService.toggleLike(postId, currentUserId);
        return ResponseEntity.ok(post);
    }

    @PostMapping("/{postId}/save")
    public ResponseEntity<Void> savePost(@PathVariable String postId, Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).build();
        }

        String username = authentication.getName();
        String userId = userService.getUserByUsername(username).getId();
        postsService.savePost(postId, userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{postId}/save")
    public ResponseEntity<Void> unsavePost(@PathVariable String postId, Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).build();
        }

        String username = authentication.getName();
        String userId = userService.getUserByUsername(username).getId();
        postsService.unsavePost(postId, userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/saved")
    public ResponseEntity<List<PostDto>> getSavedPosts(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).build();
        }

        String username = authentication.getName();
        String userId = userService.getUserByUsername(username).getId();
        List<PostDto> posts = postsService.getSavedPosts(userId);
        return ResponseEntity.ok(posts);
    }

    @GetMapping("/{postId}/comments")
    public ResponseEntity<List<CommentDto>> getComments(@PathVariable String postId) {
        List<CommentDto> comments = postsService.getComments(postId);
        return ResponseEntity.ok(comments);
    }

    @PostMapping("/{postId}/comments")
    public ResponseEntity<CommentDto> createComment(
            @PathVariable String postId,
            @RequestBody CreateCommentRequest request,
            Authentication authentication) {

        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).build();
        }

        String username = authentication.getName();
        String userId = userService.getUserByUsername(username).getId();
        CommentDto comment = postsService.createComment(postId, request, userId);
        return ResponseEntity.ok(comment);
    }
}