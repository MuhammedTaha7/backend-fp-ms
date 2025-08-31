package com.example.edusphere.controller;

import com.example.edusphere.dto.request.ScheduleRequestDto;
import com.example.edusphere.dto.response.ScheduleResponseDto;
import com.example.edusphere.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "false")
public class ScheduleController {

    private final ScheduleService scheduleService;

    @GetMapping("/by-lecturer/{lecturerId}")
    @PreAuthorize("hasRole('LECTURER') or hasRole('ADMIN')")
    public ResponseEntity<List<ScheduleResponseDto>> getLecturerSchedule(@PathVariable String lecturerId) {
        List<ScheduleResponseDto> schedules = scheduleService.getLecturerSchedule(lecturerId);
        return ResponseEntity.ok(schedules);
    }

    @PostMapping
    @PreAuthorize("hasRole('LECTURER') or hasRole('ADMIN')")
    public ResponseEntity<ScheduleResponseDto> addScheduleEntry(@RequestBody ScheduleRequestDto scheduleDto) {
        ScheduleResponseDto newEntry = scheduleService.addScheduleEntry(scheduleDto);
        return new ResponseEntity<>(newEntry, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('LECTURER') or hasRole('ADMIN')")
    public ResponseEntity<ScheduleResponseDto> updateScheduleEntry(@PathVariable String id, @RequestBody ScheduleRequestDto scheduleDto) {
        ScheduleResponseDto updatedEntry = scheduleService.updateScheduleEntry(id, scheduleDto);
        return ResponseEntity.ok(updatedEntry);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('LECTURER') or hasRole('ADMIN')")
    public ResponseEntity<Void> deleteScheduleEntry(@PathVariable String id) {
        scheduleService.deleteScheduleEntry(id);
        return ResponseEntity.noContent().build();
    }
}