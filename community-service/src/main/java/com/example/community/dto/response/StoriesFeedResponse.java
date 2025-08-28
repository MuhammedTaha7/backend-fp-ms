package com.example.community.dto.response;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class StoriesFeedResponse {
    private List<UserStoriesGroup> stories;
}