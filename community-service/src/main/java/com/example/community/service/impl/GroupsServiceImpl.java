package com.example.community.service.impl;

import com.example.community.mapper.PostMapper;
import com.example.common.entity.UserEntity;
import com.example.common.repository.UserRepository;
import com.example.community.entity.Group;
import com.example.community.entity.GroupMembership;
import com.example.community.entity.GroupInvitation;
import com.example.community.entity.Post;
import com.example.community.repository.GroupRepository;
import com.example.community.repository.GroupMembershipRepository;
import com.example.community.repository.GroupInvitationRepository;
import com.example.community.repository.PostRepository;
import com.example.community.service.GroupsService;
import com.example.community.service.FriendsService;
import com.example.community.service.FileStorageService;
import com.example.community.dto.*;
import com.example.community.dto.request.*;
import com.example.community.mapper.GroupMapper;
import com.example.community.mapper.GroupMemberMapper;
import com.example.community.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.ArrayList;

@Service
public class GroupsServiceImpl implements GroupsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private GroupMembershipRepository groupMembershipRepository;

    @Autowired
    private GroupInvitationRepository groupInvitationRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private FriendsService friendsService;

    @Autowired
    private GroupMapper groupMapper;

    @Autowired
    private GroupMemberMapper groupMemberMapper;

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private UserMapper userMapper;

    // EXISTING METHODS
    @Override
    public List<GroupDto> getAllGroups(String userId, Boolean joined) {
        List<Group> groups;

        if (joined != null) {
            if (joined) {
                List<GroupMembership> memberships = groupMembershipRepository.findByUserOrderByJoinDateDesc(getUserById(userId));
                groups = memberships.stream()
                        .map(GroupMembership::getGroup)
                        .collect(Collectors.toList());
            } else {
                List<String> memberGroupIds = groupMembershipRepository.findByUserOrderByJoinDateDesc(getUserById(userId))
                        .stream()
                        .map(membership -> membership.getGroup().getId())
                        .collect(Collectors.toList());
                groups = groupRepository.findPublicGroupsUserNotMemberOf(memberGroupIds);
            }
        } else {
            groups = groupRepository.findAll();
        }

        return groups.stream()
                .map(group -> {
                    GroupDto dto = groupMapper.toDto(group);
                    dto.setMemberCount(Math.toIntExact(groupMembershipRepository.countByGroup(group)));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<GroupDto> getUserGroups(String userId) {
        UserEntity user = getUserById(userId);
        List<GroupMembership> memberships = groupMembershipRepository.findByUserOrderByJoinDateDesc(user);

        return memberships.stream()
                .map(membership -> {
                    GroupDto dto = groupMapper.toDto(membership.getGroup());
                    dto.setUserRole(membership.getRole());
                    dto.setMemberCount(Math.toIntExact(groupMembershipRepository.countByGroup(membership.getGroup())));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public GroupDto getGroupDetails(String groupId) {
        Group group = getGroupById(groupId);
        GroupDto dto = groupMapper.toDto(group);
        dto.setMemberCount(Math.toIntExact(groupMembershipRepository.countByGroup(group)));
        return dto;
    }

    @Override
    public GroupDto createGroup(CreateGroupRequest request, MultipartFile img, String userId) {
        UserEntity user = getUserById(userId);

        Group group = new Group();
        group.setName(request.getName());
        group.setDescription(request.getDescription());
        group.setType(request.getType());
        group.setFounder(user);
        group.setMemberCount(1);
        group.setCreatedAt(LocalDateTime.now());

        if (img != null && !img.isEmpty()) {
            String imageUrl = fileStorageService.storeFile(img, "groups");
            group.setImg(imageUrl);
        }

        Group savedGroup = groupRepository.save(group);

        GroupMembership membership = new GroupMembership();
        membership.setGroup(savedGroup);
        membership.setUser(user);
        membership.setRole("Founder");
        membership.setJoinDate(LocalDateTime.now());
        groupMembershipRepository.save(membership);

        return groupMapper.toDto(savedGroup);
    }

    @Override
    public GroupDto updateGroup(String groupId, UpdateGroupRequest request, String userId) {
        Group group = getGroupById(groupId);

        if (!hasManagePermission(group, userId)) {
            throw new RuntimeException("You don't have permission to update this group");
        }

        group.setName(request.getName());
        group.setDescription(request.getDescription());
        group.setType(request.getType());
        if (request.getImg() != null) {
            group.setImg(request.getImg());
        }
        group.setUpdatedAt(LocalDateTime.now());

        Group savedGroup = groupRepository.save(group);
        return groupMapper.toDto(savedGroup);
    }

    @Override
    public void deleteGroup(String groupId, String userId) {
        Group group = getGroupById(groupId);

        if (!group.getFounder().getId().equals(userId)) {
            throw new RuntimeException("Only the founder can delete this group");
        }

        List<GroupMembership> memberships = groupMembershipRepository.findByGroupOrderByJoinDateAsc(group);
        groupMembershipRepository.deleteAll(memberships);

        groupRepository.delete(group);
    }

    @Override
    public void joinGroup(String groupId, String userId) {
        Group group = getGroupById(groupId);
        UserEntity user = getUserById(userId);

        if (groupMembershipRepository.findByGroupAndUser(group, user).isPresent()) {
            throw new RuntimeException("User is already a member of this group");
        }

        GroupMembership membership = new GroupMembership();
        membership.setGroup(group);
        membership.setUser(user);
        membership.setRole("Member");
        membership.setJoinDate(LocalDateTime.now());
        groupMembershipRepository.save(membership);

        group.setMemberCount(group.getMemberCount() + 1);
        groupRepository.save(group);
    }

    @Override
    public void leaveGroup(String groupId, String userId) {
        Group group = getGroupById(groupId);
        UserEntity user = getUserById(userId);

        if (group.getFounder().getId().equals(userId)) {
            throw new RuntimeException("Founder cannot leave the group. Delete the group instead.");
        }

        groupMembershipRepository.deleteByGroupAndUser(group, user);

        group.setMemberCount(group.getMemberCount() - 1);
        groupRepository.save(group);
    }

    @Override
    public List<GroupMemberDto> getGroupMembers(String groupId) {
        Group group = getGroupById(groupId);
        List<GroupMembership> memberships = groupMembershipRepository.findByGroupOrderByJoinDateAsc(group);

        return memberships.stream()
                .map(groupMemberMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public void promoteMember(String groupId, String memberId, String role, String currentUserId) {
        Group group = getGroupById(groupId);

        if (!group.getFounder().getId().equals(currentUserId)) {
            throw new RuntimeException("Only the founder can promote members");
        }

        UserEntity member = getUserById(memberId);
        GroupMembership membership = groupMembershipRepository.findByGroupAndUser(group, member)
                .orElseThrow(() -> new RuntimeException("User is not a member of this group"));

        membership.setRole(role);
        groupMembershipRepository.save(membership);
    }

    @Override
    public void removeMember(String groupId, String memberId, String currentUserId) {
        Group group = getGroupById(groupId);

        if (!hasManagePermission(group, currentUserId)) {
            throw new RuntimeException("You don't have permission to remove members");
        }

        if (group.getFounder().getId().equals(memberId)) {
            throw new RuntimeException("Cannot remove the founder");
        }

        UserEntity member = getUserById(memberId);
        groupMembershipRepository.deleteByGroupAndUser(group, member);

        group.setMemberCount(group.getMemberCount() - 1);
        groupRepository.save(group);
    }

    @Override
    public List<PostDto> getGroupPosts(String groupId) {
        List<Post> posts = postRepository.findByGroupIdOrderByCreatedAtDesc(groupId);
        return posts.stream()
                .map(postMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<PostDto> getGroupFeed(String userId) {
        UserEntity user = getUserById(userId);
        List<GroupMembership> memberships = groupMembershipRepository.findByUserOrderByJoinDateDesc(user);

        List<String> groupIds = memberships.stream()
                .map(membership -> membership.getGroup().getId())
                .collect(Collectors.toList());

        List<Post> posts = postRepository.findByGroupIdInOrderByCreatedAtDesc(groupIds);
        return posts.stream()
                .map(postMapper::toDto)
                .collect(Collectors.toList());
    }

    // NEW ENHANCED METHODS
    @Override
    public List<GroupDto> searchGroups(String userId, String searchTerm, String type, String sortBy) {
        List<Group> groups;

        try {
            if (searchTerm != null && !searchTerm.trim().isEmpty()) {
                groups = groupRepository.findByNameContainingIgnoreCase(searchTerm.trim());
            } else {
                groups = groupRepository.findAll();
            }

            if (type != null && !type.isEmpty() && !"all".equalsIgnoreCase(type)) {
                groups = groups.stream()
                        .filter(group -> type.equalsIgnoreCase(group.getType()))
                        .collect(Collectors.toList());
            }

            List<String> joinedGroupIds = groupMembershipRepository.findByUserOrderByJoinDateDesc(getUserById(userId))
                    .stream()
                    .map(membership -> membership.getGroup().getId())
                    .collect(Collectors.toList());

            groups = groups.stream()
                    .filter(group -> !joinedGroupIds.contains(group.getId()))
                    .collect(Collectors.toList());

            if ("members".equalsIgnoreCase(sortBy)) {
                groups.sort((a, b) -> Integer.compare(b.getMemberCount(), a.getMemberCount()));
            } else if ("newest".equalsIgnoreCase(sortBy)) {
                groups.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
            } else {
                groups.sort((a, b) -> {
                    LocalDateTime aUpdated = a.getUpdatedAt() != null ? a.getUpdatedAt() : a.getCreatedAt();
                    LocalDateTime bUpdated = b.getUpdatedAt() != null ? b.getUpdatedAt() : b.getCreatedAt();
                    return bUpdated.compareTo(aUpdated);
                });
            }

            return groups.stream()
                    .map(group -> {
                        GroupDto dto = groupMapper.toDto(group);
                        dto.setMemberCount(Math.toIntExact(groupMembershipRepository.countByGroup(group)));
                        return dto;
                    })
                    .collect(Collectors.toList());

        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    @Override
    public List<GroupDto> getRecommendedGroups(String userId) {
        try {
            UserEntity user = getUserById(userId);
            List<Group> recommendedGroups = new ArrayList<>();

            List<String> joinedGroupIds = groupMembershipRepository.findByUserOrderByJoinDateDesc(user)
                    .stream()
                    .map(membership -> membership.getGroup().getId())
                    .collect(Collectors.toList());

            List<Group> popularGroups = groupRepository.findTop10ByTypeOrderByMemberCountDesc("Public");
            recommendedGroups.addAll(popularGroups.stream()
                    .filter(group -> !joinedGroupIds.contains(group.getId()))
                    .limit(10)
                    .collect(Collectors.toList()));

            return recommendedGroups.stream()
                    .distinct()
                    .map(group -> {
                        GroupDto dto = groupMapper.toDto(group);
                        dto.setMemberCount(Math.toIntExact(groupMembershipRepository.countByGroup(group)));
                        return dto;
                    })
                    .collect(Collectors.toList());

        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    @Override
    @Transactional
    public BulkInviteResultDto inviteFriendsToGroup(String groupId, InviteFriendsRequest request, String inviterId) {
        Group group = getGroupById(groupId);
        UserEntity inviter = getUserById(inviterId);
        BulkInviteResultDto result = new BulkInviteResultDto();

        if (!hasInvitePermission(group, inviterId)) {
            throw new RuntimeException("You don't have permission to invite members to this group");
        }

        for (String friendId : request.getFriendIds()) {
            try {
                UserEntity friend = getUserById(friendId);

                if (groupMembershipRepository.findByGroupAndUser(group, friend).isPresent()) {
                    result.getAlreadyMembers().add(friend.getName());
                    continue;
                }

                if (groupInvitationRepository.findByGroupAndInviterAndInviteeAndStatus(
                        group, inviter, friend, "PENDING").isPresent()) {
                    result.getAlreadyInvited().add(friend.getName());
                    continue;
                }

                GroupInvitation invitation = new GroupInvitation();
                invitation.setGroup(group);
                invitation.setInviter(inviter);
                invitation.setInvitee(friend);
                invitation.setStatus("PENDING");
                invitation.setMessage(request.getMessage());
                invitation.setCreatedAt(LocalDateTime.now());
                invitation.setExpiresAt(LocalDateTime.now().plusDays(30));

                groupInvitationRepository.save(invitation);
                result.getSuccessfulInvites().add(friend.getName());

            } catch (Exception e) {
                result.getFailedInvites().add("Failed to invite friend: " + e.getMessage());
            }
        }

        result.setTotalAttempted(request.getFriendIds().size());
        result.setTotalSuccessful(result.getSuccessfulInvites().size());
        result.setTotalFailed(result.getFailedInvites().size());

        return result;
    }

    @Override
    public List<GroupInvitationDto> getGroupInvitations(String userId) {
        UserEntity user = getUserById(userId);
        List<GroupInvitation> invitations = groupInvitationRepository
                .findByInviteeAndStatusOrderByCreatedAtDesc(user, "PENDING");

        return invitations.stream()
                .map(this::mapInvitationToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void respondToGroupInvitation(String invitationId, String response, String userId) {
        GroupInvitation invitation = groupInvitationRepository.findById(invitationId)
                .orElseThrow(() -> new RuntimeException("Invitation not found"));

        UserEntity user = getUserById(userId);

        if (!invitation.getInvitee().getId().equals(userId)) {
            throw new RuntimeException("You are not authorized to respond to this invitation");
        }

        if (!"PENDING".equals(invitation.getStatus())) {
            throw new RuntimeException("This invitation has already been responded to");
        }

        if (invitation.getExpiresAt().isBefore(LocalDateTime.now())) {
            invitation.setStatus("EXPIRED");
            groupInvitationRepository.save(invitation);
            throw new RuntimeException("This invitation has expired");
        }

        invitation.setStatus(response.toUpperCase());
        invitation.setRespondedAt(LocalDateTime.now());
        groupInvitationRepository.save(invitation);

        if ("ACCEPTED".equalsIgnoreCase(response)) {
            if (groupMembershipRepository.findByGroupAndUser(invitation.getGroup(), user).isEmpty()) {
                GroupMembership membership = new GroupMembership();
                membership.setGroup(invitation.getGroup());
                membership.setUser(user);
                membership.setRole("Member");
                membership.setJoinDate(LocalDateTime.now());
                groupMembershipRepository.save(membership);

                Group group = invitation.getGroup();
                group.setMemberCount(group.getMemberCount() + 1);
                groupRepository.save(group);
            }
        }
    }

    @Override
    public GroupStatsDto getGroupStats(String userId) {
        GroupStatsDto stats = new GroupStatsDto();
        UserEntity user = getUserById(userId);

        stats.setTotalGroups(groupRepository.count());
        stats.setPublicGroups(groupRepository.countByType("Public"));
        stats.setPrivateGroups(groupRepository.countByType("Private"));
        stats.setUserJoinedGroups((long) groupMembershipRepository.findByUserOrderByJoinDateDesc(user).size());
        stats.setPendingInvitations((long) groupInvitationRepository.findByInviteeAndStatus(user, "PENDING").size());

        return stats;
    }

    @Override
    public List<String> getGroupCategories() {
        return List.of("Study", "Sports", "Technology", "Arts", "Music", "Gaming", "Social", "Academic", "Professional");
    }

    @Override
    public List<GroupActivityDto> getGroupActivity(String groupId, int page, int size) {
        return new ArrayList<>();
    }

    @Override
    public List<GroupDto> getTrendingGroups(String userId, int limit) {
        List<Group> trendingGroups = groupRepository.findTop10ByTypeOrderByMemberCountDesc("Public");
        return trendingGroups.stream()
                .limit(limit)
                .map(group -> {
                    GroupDto dto = groupMapper.toDto(group);
                    dto.setMemberCount(Math.toIntExact(groupMembershipRepository.countByGroup(group)));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public void pinPost(String groupId, String postId, String userId) {
        // Implementation for pinning posts
    }

    @Override
    public void unpinPost(String groupId, String postId, String userId) {
        // Implementation for unpinning posts
    }

    @Override
    public List<PostDto> getPinnedPosts(String groupId) {
        return new ArrayList<>();
    }

    @Override
    public void updateGroupSettings(String groupId, GroupSettingsRequest request, String userId) {
        // Implementation for updating group settings
    }

    @Override
    public void reportGroup(String groupId, GroupReportRequest request, String userId) {
        // Implementation for reporting groups
    }

    // HELPER METHODS
    private UserEntity getUserById(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private Group getGroupById(String groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
    }

    private boolean hasManagePermission(Group group, String userId) {
        if (group.getFounder().getId().equals(userId)) {
            return true;
        }

        UserEntity user = getUserById(userId);
        return groupMembershipRepository.findByGroupAndUser(group, user)
                .map(membership -> "Co-founder".equals(membership.getRole()))
                .orElse(false);
    }

    private boolean hasInvitePermission(Group group, String userId) {
        if (hasManagePermission(group, userId)) {
            return true;
        }

        UserEntity user = getUserById(userId);
        return groupMembershipRepository.findByGroupAndUser(group, user).isPresent();
    }

    private GroupInvitationDto mapInvitationToDto(GroupInvitation invitation) {
        GroupInvitationDto dto = new GroupInvitationDto();
        dto.setId(invitation.getId());
        dto.setGroup(groupMapper.toDto(invitation.getGroup()));
        dto.setInviter(userMapper.toDto(invitation.getInviter()));
        dto.setMessage(invitation.getMessage());
        dto.setCreatedAt(invitation.getCreatedAt());
        dto.setExpiresAt(invitation.getExpiresAt());
        return dto;
    }
}