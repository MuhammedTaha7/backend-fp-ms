package com.example.community.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

@Getter
@Setter
public class InviteFriendsRequest {

    @NotEmpty(message = "At least one friend must be selected")
    private List<String> friendIds;

    @Size(max = 500, message = "Message cannot exceed 500 characters")
    private String message;
}