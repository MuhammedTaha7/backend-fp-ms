package com.example.edusphere.controller;

import com.example.edusphere.dto.request.CalendarFilterDto;
import com.example.edusphere.dto.response.CalendarEventDto;
import com.example.edusphere.entity.Event; // Import Event
import com.example.edusphere.service.CalendarService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/calendar")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class CalendarController {

    @Autowired
    private CalendarService calendarService;

    @GetMapping("/events")
    public ResponseEntity<List<CalendarEventDto>> getCalendarEvents(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStartDate,
            // ✅ Add optional parameters for filtering
            @RequestParam(required = false) String courseId,
            @RequestParam(required = false) String instructorId,
            @RequestParam(required = false) String groupId
    ) {
        // We will create this DTO to pass the filters cleanly
        CalendarFilterDto filters = new CalendarFilterDto(courseId, instructorId, groupId);
        List<CalendarEventDto> events = calendarService.getCalendarEventsForUser(weekStartDate, filters);
        return ResponseEntity.ok(events);
    }

    @PostMapping("/events")
    public ResponseEntity<Event> createEvent(@RequestBody Event event) {
        Event createdEvent = calendarService.createEvent(event);
        return new ResponseEntity<>(createdEvent, HttpStatus.CREATED);
    }

    @DeleteMapping("/events/{eventId}")
    public ResponseEntity<Void> deleteEvent(@PathVariable String eventId) {
        calendarService.deleteEvent(eventId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/events/{eventId}")
    public ResponseEntity<Event> getEventById(@PathVariable String eventId) {
        return ResponseEntity.ok(calendarService.getEventById(eventId));
    }

    @PutMapping("/events/{eventId}")
    public ResponseEntity<Event> updateEvent(@PathVariable String eventId, @RequestBody Event eventDetails) {
        return ResponseEntity.ok(calendarService.updateEvent(eventId, eventDetails));
    }
}