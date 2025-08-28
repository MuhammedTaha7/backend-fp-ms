package com.example.community.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GroupSearchRequest {
    private String searchTerm;
    private String type; // "Public", "Private", "all"
    private String sortBy; // "activity", "members", "newest"
    private String category; // Future: group categories
    private Integer page = 0;
    private Integer size = 20;
}