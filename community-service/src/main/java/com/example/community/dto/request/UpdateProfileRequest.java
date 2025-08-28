package com.example.community.dto.request;

import lombok.Getter;
import lombok.Setter;
import java.util.Map;

@Getter
@Setter
public class UpdateProfileRequest {
    private String name;
    private String title;
    private String bio;
    private String location;
    private String website;
    private String coverPic;
    private Map<String, String> socialLinks;
}