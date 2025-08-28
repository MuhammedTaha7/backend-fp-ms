package com.example.edusphere.service;

import com.example.edusphere.dto.request.ScheduleRequestDto;
import com.example.edusphere.dto.response.ScheduleResponseDto;

import java.util.List;

public interface ScheduleService {
    List<ScheduleResponseDto> getLecturerSchedule(String lecturerId);
    ScheduleResponseDto addScheduleEntry(ScheduleRequestDto scheduleDto);
    ScheduleResponseDto updateScheduleEntry(String id, ScheduleRequestDto scheduleDto);
    void deleteScheduleEntry(String id);
}