package com.example.edusphere.service;

import com.example.edusphere.dto.request.CalendarFilterDto;
import com.example.edusphere.dto.response.CalendarEventDto;
import com.example.edusphere.entity.Event;
import java.time.LocalDate;
import java.util.List;

public interface CalendarService {

    List<CalendarEventDto> getCalendarEventsForUser(LocalDate weekStartDate, CalendarFilterDto filters);

    Event createEvent(Event event);
    void deleteEvent(String eventId);
    Event getEventById(String eventId);
    Event updateEvent(String eventId, Event eventDetails);
}