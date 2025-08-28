package com.example.community.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReportRequest {
    private String reason;
    private String description;
}