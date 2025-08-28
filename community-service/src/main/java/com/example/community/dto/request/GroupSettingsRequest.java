package com.example.community.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GroupSettingsRequest {
    private Boolean allowMemberInvites; // Allow regular members to invite friends
    private Boolean requireApprovalToJoin; // Require admin approval for join requests
    private Boolean allowMemberPosts; // Allow members to post (vs admin-only)
    private Boolean showMemberList; // Show member list to non-members
    private String joinApprovalMessage; // Custom message for join requests
    private String[] blockedWords; // Words that trigger post moderation
    private Integer maxMembersLimit; // Maximum number of members (null = unlimited)
    private Boolean enablePostModeration; // Require admin approval for posts
    private Boolean allowFileSharing; // Allow file attachments in posts
    private Boolean allowExternalLinks; // Allow external links in posts
}