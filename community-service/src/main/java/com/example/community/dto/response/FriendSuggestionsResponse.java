package com.example.community.dto.response;

import com.example.community.dto.UserDto;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class FriendSuggestionsResponse {
    private List<UserDto> suggestions;
    private String reason; // "Same university", "Mutual friends", etc.
    private int totalCount;

    public FriendSuggestionsResponse() {}

    public FriendSuggestionsResponse(List<UserDto> suggestions, int totalCount) {
        this.suggestions = suggestions;
        this.totalCount = totalCount;
    }
}