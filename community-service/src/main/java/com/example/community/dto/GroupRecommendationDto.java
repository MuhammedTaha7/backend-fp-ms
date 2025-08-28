package com.example.community.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class GroupRecommendationDto {
    private GroupDto group;
    private String recommendationReason; // "Friends are members", "Same university", "Popular", etc.
    private List<UserDto> mutualFriends; // Friends who are members
    private Double relevanceScore; // 0.0 to 1.0
    private String category;
}