package com.example.community.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateGroupRequest {
    private String name;
    private String description;
    private String type;
    private String img;
}