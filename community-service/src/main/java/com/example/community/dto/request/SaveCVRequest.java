package com.example.community.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SaveCVRequest {
    private String name;
    private String title;
    private String summary;
    private String education;
    private String experience;
    private String skills;
    private String links;
}