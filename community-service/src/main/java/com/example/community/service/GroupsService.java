// GroupsService.java - Enhanced Interface with New Methods
package com.example.community.service;

import com.example.community.dto.*;
import com.example.community.dto.request.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface GroupsService {

    // EXISTING METHODS
    List<GroupDto> getAllGroups(String userId, Boolean joined);
    List<GroupDto> getUserGroups(String userId);
    GroupDto getGroupDetails(String groupId);
    GroupDto createGroup(CreateGroupRequest request, MultipartFile img, String userId);
    GroupDto updateGroup(String groupId, UpdateGroupRequest request, String userId);
    void deleteGroup(String groupId, String userId);
    void joinGroup(String groupId, String userId);
    void leaveGroup(String groupId, String userId);
    List<GroupMemberDto> getGroupMembers(String groupId);
    void promoteMember(String groupId, String memberId, String role, String currentUserId);
    void removeMember(String groupId, String memberId, String currentUserId);
    List<PostDto> getGroupPosts(String groupId);
    List<PostDto> getGroupFeed(String userId);

    // NEW ENHANCED METHODS
    List<GroupDto> searchGroups(String userId, String searchTerm, String type, String sortBy);
    List<GroupDto> getRecommendedGroups(String userId);
    BulkInviteResultDto inviteFriendsToGroup(String groupId, InviteFriendsRequest request, String inviterId);
    List<GroupInvitationDto> getGroupInvitations(String userId);
    void respondToGroupInvitation(String invitationId, String response, String userId);
    GroupStatsDto getGroupStats(String userId);
    List<String> getGroupCategories();
    List<GroupActivityDto> getGroupActivity(String groupId, int page, int size);
    List<GroupDto> getTrendingGroups(String userId, int limit);
    void pinPost(String groupId, String postId, String userId);
    void unpinPost(String groupId, String postId, String userId);
    List<PostDto> getPinnedPosts(String groupId);
    void updateGroupSettings(String groupId, GroupSettingsRequest request, String userId);
    void reportGroup(String groupId, GroupReportRequest request, String userId);
}