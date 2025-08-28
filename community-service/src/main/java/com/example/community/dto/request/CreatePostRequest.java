package com.example.community.dto.request;

import lombok.Getter;
import lombok.Setter;
import java.util.Map;

@Getter
@Setter
public class CreatePostRequest {
    private String desc;
    private String img;
    private Map<String, String> file; // {"name": "filename", "url": "file_url"}
    private String groupId; // If posting to a group
    private String groupName; // If posting to a group
}