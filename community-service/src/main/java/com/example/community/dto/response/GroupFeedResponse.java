package com.example.community.dto.response;

import com.example.community.dto.PostDto;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class GroupFeedResponse {
    private List<PostDto> posts;
    private int totalGroups;
    private List<String> groupNames;

    public GroupFeedResponse() {}

    public GroupFeedResponse(List<PostDto> posts, int totalGroups, List<String> groupNames) {
        this.posts = posts;
        this.totalGroups = totalGroups;
        this.groupNames = groupNames;
    }
}