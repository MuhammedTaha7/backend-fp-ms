package com.example.community.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PromoteMemberRequest {
    private String userId;
    private String role; // "Co-founder", "Member"
}