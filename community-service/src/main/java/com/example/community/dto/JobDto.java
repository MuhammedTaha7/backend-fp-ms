package com.example.community.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class JobDto {
    private String id;
    private String title;
    private String company;
    private String location;
    private String description;
    private String type;
    private List<String> tags;
    private String salary;
    private String remote;
    private String experience;
    private String deadline;
    private List<String> benefits;
    private String requirements;
    private String status;
    private String postedDate;
    private String updatedAt;

    // Poster information
    private String postedById;
    private String postedByName;
}