package com.example.community.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GroupStatsDto {
    private long totalGroups;
    private long publicGroups;
    private long privateGroups;
    private long userJoinedGroups;
    private long availableGroups;
    private long pendingInvitations;
    private GroupDto mostActiveGroup;
    private GroupDto largestGroup;
    private GroupDto newestGroup;
}