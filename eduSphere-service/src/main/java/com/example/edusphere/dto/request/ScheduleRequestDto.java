package com.example.edusphere.dto.request;

import lombok.Data;

@Data
public class ScheduleRequestDto {
    private String lecturerId;
    private String day;
    private String startTime;
    private String endTime;
    private String availability;
    private String notes;
}