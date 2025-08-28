package com.example.community.dto.response;

import com.example.community.dto.StoryDto;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class UserStoriesGroup {
    private String userId;
    private String name;
    private String profilePic;
    private List<StoryDto> stories;
}