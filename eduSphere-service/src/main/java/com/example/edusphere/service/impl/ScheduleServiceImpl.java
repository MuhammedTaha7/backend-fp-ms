package com.example.edusphere.service.impl;

import com.example.edusphere.dto.request.ScheduleRequestDto;
import com.example.edusphere.dto.response.ScheduleResponseDto;
import com.example.edusphere.entity.LecturerSchedule;
import com.example.edusphere.repository.LecturerScheduleRepository;
import com.example.edusphere.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class ScheduleServiceImpl implements ScheduleService {

    private final LecturerScheduleRepository scheduleRepository;

    @Override
    public List<ScheduleResponseDto> getLecturerSchedule(String lecturerId) {
        List<LecturerSchedule> schedules = scheduleRepository.findByLecturerId(lecturerId);
        return schedules.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public ScheduleResponseDto addScheduleEntry(ScheduleRequestDto scheduleDto) {
        LecturerSchedule schedule = new LecturerSchedule();
        schedule.setLecturerId(scheduleDto.getLecturerId());
        schedule.setDay(scheduleDto.getDay());
        schedule.setStartTime(scheduleDto.getStartTime());
        schedule.setEndTime(scheduleDto.getEndTime());
        schedule.setAvailability(scheduleDto.getAvailability());
        schedule.setNotes(scheduleDto.getNotes());

        LecturerSchedule savedSchedule = scheduleRepository.save(schedule);
        return mapToDto(savedSchedule);
    }

    @Override
    public ScheduleResponseDto updateScheduleEntry(String id, ScheduleRequestDto scheduleDto) {
        LecturerSchedule existingSchedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Schedule entry not found with ID: " + id));

        existingSchedule.setDay(scheduleDto.getDay());
        existingSchedule.setStartTime(scheduleDto.getStartTime());
        existingSchedule.setEndTime(scheduleDto.getEndTime());
        existingSchedule.setAvailability(scheduleDto.getAvailability());
        existingSchedule.setNotes(scheduleDto.getNotes());

        LecturerSchedule updatedSchedule = scheduleRepository.save(existingSchedule);
        return mapToDto(updatedSchedule);
    }

    @Override
    public void deleteScheduleEntry(String id) {
        scheduleRepository.deleteById(id);
    }

    private ScheduleResponseDto mapToDto(LecturerSchedule schedule) {
        ScheduleResponseDto dto = new ScheduleResponseDto();
        dto.setId(schedule.getId());
        dto.setLecturerId(schedule.getLecturerId());
        dto.setDay(schedule.getDay());
        dto.setStartTime(schedule.getStartTime());
        dto.setEndTime(schedule.getEndTime());
        dto.setAvailability(schedule.getAvailability());
        dto.setNotes(schedule.getNotes());
        return dto;
    }
}