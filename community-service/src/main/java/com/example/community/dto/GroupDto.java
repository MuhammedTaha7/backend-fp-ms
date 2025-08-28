package com.example.community.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class GroupDto {
    private String id;
    private String name;
    private String description;
    private String img;
    private String type;
    private UserDto founder;
    private Integer memberCount;
    private String userRole; // Role of current user in this group
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // NEW FIELDS for enhanced functionality
    private boolean isUserMember; // Quick check for membership
    private boolean canInviteFriends; // Permission check
    private boolean canManageGroup; // Admin permission check
    private List<UserDto> recentMembers; // Show some recent members
    private String activityLevel; // "High", "Medium", "Low" based on recent posts
    private Integer recentPostsCount; // Posts in last 7 days
    private boolean hasUnreadPosts; // If user has unread posts
    private String category; // Future: group categories like "Study", "Sports", etc.
    private List<String> tags; // Future: searchable tags
}