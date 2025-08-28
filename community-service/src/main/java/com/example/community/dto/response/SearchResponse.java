package com.example.community.dto.response;

import com.example.community.dto.UserDto;
import com.example.community.dto.GroupDto;
import com.example.community.dto.PostDto;
import com.example.community.dto.JobDto;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class SearchResponse {
    private List<UserDto> users;
    private List<GroupDto> groups;
    private List<PostDto> posts;
    private List<JobDto> jobs;
    private String query;
    private int totalResults;

    public SearchResponse() {}

    public SearchResponse(String query) {
        this.query = query;
        this.totalResults = 0;
    }
}