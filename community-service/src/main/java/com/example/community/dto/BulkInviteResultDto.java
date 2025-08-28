package com.example.community.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.List;
import java.util.ArrayList;

@Getter
@Setter
public class BulkInviteResultDto {
    private List<String> successfulInvites;
    private List<String> failedInvites;
    private List<String> alreadyMembers;
    private List<String> alreadyInvited;
    private int totalAttempted;
    private int totalSuccessful;
    private int totalFailed;

    public BulkInviteResultDto() {
        this.successfulInvites = new ArrayList<>();
        this.failedInvites = new ArrayList<>();
        this.alreadyMembers = new ArrayList<>();
        this.alreadyInvited = new ArrayList<>();
    }
}