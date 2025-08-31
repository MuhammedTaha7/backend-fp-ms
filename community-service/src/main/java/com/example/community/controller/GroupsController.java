package com.example.community.controller;

import com.example.community.service.GroupsService;
import com.example.common.service.UserService; // Add this import
import com.example.community.dto.*;
import com.example.community.dto.request.*;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequestMapping("/api/groups")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "false")
public class GroupsController {

    @Autowired
    private GroupsService groupsService;

    @Autowired
    private UserService userService; // Add this

    @GetMapping
    public ResponseEntity<List<GroupDto>> getAllGroups(
            @RequestParam(required = false) Boolean joined,
            Authentication authentication) {
        String username = authentication.getName();
        String userId = userService.getUserByUsername(username).getId();
        List<GroupDto> groups = groupsService.getAllGroups(userId, joined);
        return ResponseEntity.ok(groups);
    }

    @GetMapping("/my-groups")
    public ResponseEntity<List<GroupDto>> getMyGroups(Authentication authentication) {
        String username = authentication.getName();
        String userId = userService.getUserByUsername(username).getId();
        List<GroupDto> groups = groupsService.getUserGroups(userId);
        return ResponseEntity.ok(groups);
    }

    @GetMapping("/{groupId}")
    public ResponseEntity<GroupDto> getGroupDetails(@PathVariable String groupId) {
        GroupDto group = groupsService.getGroupDetails(groupId);
        return ResponseEntity.ok(group);
    }

    @PostMapping
    public ResponseEntity<GroupDto> createGroup(
            @RequestPart("name") String name,
            @RequestPart("description") String description,
            @RequestPart("type") String type,
            @RequestPart(value = "img", required = false) MultipartFile img,
            Authentication authentication) {
        String username = authentication.getName();
        String userId = userService.getUserByUsername(username).getId();

        CreateGroupRequest request = new CreateGroupRequest();
        request.setName(name);
        request.setDescription(description);
        request.setType(type);

        GroupDto group = groupsService.createGroup(request, img, userId);
        return ResponseEntity.ok(group);
    }

    @PutMapping("/{groupId}")
    public ResponseEntity<GroupDto> updateGroup(
            @PathVariable String groupId,
            @RequestBody UpdateGroupRequest request,
            Authentication authentication) {
        String username = authentication.getName();
        String userId = userService.getUserByUsername(username).getId();
        GroupDto group = groupsService.updateGroup(groupId, request, userId);
        return ResponseEntity.ok(group);
    }

    @DeleteMapping("/{groupId}")
    public ResponseEntity<Void> deleteGroup(@PathVariable String groupId, Authentication authentication) {
        String username = authentication.getName();
        String userId = userService.getUserByUsername(username).getId();
        groupsService.deleteGroup(groupId, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{groupId}/join")
    public ResponseEntity<Void> joinGroup(@PathVariable String groupId, Authentication authentication) {
        String username = authentication.getName();
        String userId = userService.getUserByUsername(username).getId();
        groupsService.joinGroup(groupId, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{groupId}/leave")
    public ResponseEntity<Void> leaveGroup(@PathVariable String groupId, Authentication authentication) {
        String username = authentication.getName();
        String userId = userService.getUserByUsername(username).getId();
        groupsService.leaveGroup(groupId, userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{groupId}/members")
    public ResponseEntity<List<GroupMemberDto>> getGroupMembers(@PathVariable String groupId) {
        List<GroupMemberDto> members = groupsService.getGroupMembers(groupId);
        return ResponseEntity.ok(members);
    }

    @PostMapping("/{groupId}/promote")
    public ResponseEntity<Void> promoteMember(
            @PathVariable String groupId,
            @RequestBody PromoteMemberRequest request,
            Authentication authentication) {
        String username = authentication.getName();
        String currentUserId = userService.getUserByUsername(username).getId();
        groupsService.promoteMember(groupId, request.getUserId(), request.getRole(), currentUserId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{groupId}/remove")
    public ResponseEntity<Void> removeMember(
            @PathVariable String groupId,
            @RequestBody RemoveMemberRequest request,
            Authentication authentication) {
        String username = authentication.getName();
        String currentUserId = userService.getUserByUsername(username).getId();
        groupsService.removeMember(groupId, request.getUserId(), currentUserId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{groupId}/posts")
    public ResponseEntity<List<PostDto>> getGroupPosts(@PathVariable String groupId) {
        List<PostDto> posts = groupsService.getGroupPosts(groupId);
        return ResponseEntity.ok(posts);
    }

    @GetMapping("/feed")
    public ResponseEntity<List<PostDto>> getGroupFeed(Authentication authentication) {
        String username = authentication.getName();
        String userId = userService.getUserByUsername(username).getId();
        List<PostDto> posts = groupsService.getGroupFeed(userId);
        return ResponseEntity.ok(posts);
    }

    @GetMapping("/search")
    public ResponseEntity<List<GroupDto>> searchGroups(
            @RequestParam(required = false) String q,
            @RequestParam(required = false, defaultValue = "all") String type,
            @RequestParam(required = false, defaultValue = "activity") String sortBy,
            Authentication authentication) {
        String username = authentication.getName();
        String userId = userService.getUserByUsername(username).getId();
        List<GroupDto> groups = groupsService.searchGroups(userId, q, type, sortBy);
        return ResponseEntity.ok(groups);
    }

    @GetMapping("/recommendations")
    public ResponseEntity<List<GroupDto>> getRecommendedGroups(Authentication authentication) {
        String username = authentication.getName();
        String userId = userService.getUserByUsername(username).getId();
        List<GroupDto> groups = groupsService.getRecommendedGroups(userId);
        return ResponseEntity.ok(groups);
    }

    @PostMapping("/{groupId}/invite-friends")
    public ResponseEntity<BulkInviteResultDto> inviteFriendsToGroup(
            @PathVariable String groupId,
            @Valid @RequestBody InviteFriendsRequest request,
            Authentication authentication) {
        String username = authentication.getName();
        String userId = userService.getUserByUsername(username).getId();
        BulkInviteResultDto result = groupsService.inviteFriendsToGroup(groupId, request, userId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/invitations")
    public ResponseEntity<List<GroupInvitationDto>> getGroupInvitations(Authentication authentication) {
        String username = authentication.getName();
        String userId = userService.getUserByUsername(username).getId();
        List<GroupInvitationDto> invitations = groupsService.getGroupInvitations(userId);
        return ResponseEntity.ok(invitations);
    }

    @PostMapping("/invitations/{invitationId}/respond")
    public ResponseEntity<Void> respondToGroupInvitation(
            @PathVariable String invitationId,
            @Valid @RequestBody InvitationResponseRequest request,
            Authentication authentication) {
        String username = authentication.getName();
        String userId = userService.getUserByUsername(username).getId();
        groupsService.respondToGroupInvitation(invitationId, request.getResponse(), userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/stats")
    public ResponseEntity<GroupStatsDto> getGroupStats(Authentication authentication) {
        String username = authentication.getName();
        String userId = userService.getUserByUsername(username).getId();
        GroupStatsDto stats = groupsService.getGroupStats(userId);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/categories")
    public ResponseEntity<List<String>> getGroupCategories() {
        List<String> categories = groupsService.getGroupCategories();
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/{groupId}/activity")
    public ResponseEntity<List<GroupActivityDto>> getGroupActivity(
            @PathVariable String groupId,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        List<GroupActivityDto> activities = groupsService.getGroupActivity(groupId, page, size);
        return ResponseEntity.ok(activities);
    }

    @PostMapping("/{groupId}/pin-post/{postId}")
    public ResponseEntity<Void> pinPost(
            @PathVariable String groupId,
            @PathVariable String postId,
            Authentication authentication) {
        String username = authentication.getName();
        String userId = userService.getUserByUsername(username).getId();
        groupsService.pinPost(groupId, postId, userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{groupId}/pin-post/{postId}")
    public ResponseEntity<Void> unpinPost(
            @PathVariable String groupId,
            @PathVariable String postId,
            Authentication authentication) {
        String username = authentication.getName();
        String userId = userService.getUserByUsername(username).getId();
        groupsService.unpinPost(groupId, postId, userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{groupId}/pinned-posts")
    public ResponseEntity<List<PostDto>> getPinnedPosts(@PathVariable String groupId) {
        List<PostDto> posts = groupsService.getPinnedPosts(groupId);
        return ResponseEntity.ok(posts);
    }

    @PostMapping("/{groupId}/settings")
    public ResponseEntity<Void> updateGroupSettings(
            @PathVariable String groupId,
            @RequestBody GroupSettingsRequest request,
            Authentication authentication) {
        String username = authentication.getName();
        String userId = userService.getUserByUsername(username).getId();
        groupsService.updateGroupSettings(groupId, request, userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/trending")
    public ResponseEntity<List<GroupDto>> getTrendingGroups(
            @RequestParam(required = false, defaultValue = "10") int limit,
            Authentication authentication) {
        String username = authentication.getName();
        String userId = userService.getUserByUsername(username).getId();
        List<GroupDto> groups = groupsService.getTrendingGroups(userId, limit);
        return ResponseEntity.ok(groups);
    }

    @PostMapping("/{groupId}/report")
    public ResponseEntity<Void> reportGroup(
            @PathVariable String groupId,
            @RequestBody GroupReportRequest request,
            Authentication authentication) {
        String username = authentication.getName();
        String userId = userService.getUserByUsername(username).getId();
        groupsService.reportGroup(groupId, request, userId);
        return ResponseEntity.ok().build();
    }
}