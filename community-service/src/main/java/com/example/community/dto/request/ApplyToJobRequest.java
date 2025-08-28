package com.example.community.dto.request;

import lombok.Getter;
import lombok.Setter;
import java.util.Map;

@Getter
@Setter
public class ApplyToJobRequest {
    private String applicationLink;
    private String message;
    private Map<String, String> cvData; // CV form data if provided
}