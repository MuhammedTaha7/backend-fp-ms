package com.example.edusphere.dto.response;

import lombok.Data;

@Data
public class ScheduleResponseDto {
    private String id;
    private String lecturerId;
    private String day;
    private String startTime;
    private String endTime;
    private String availability;
    private String notes;
}