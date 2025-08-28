package com.example.community.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateGroupRequest {
    private String name;
    private String description;
    private String type; // "Public" or "Private"
    private String img; // Image URL (if provided as URL instead of file)
}