package com.example.community.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FriendshipStatusDto {
    private String status; // "friends", "pending", "none"

    public FriendshipStatusDto() {}

    public FriendshipStatusDto(String status) {
        this.status = status;
    }
}