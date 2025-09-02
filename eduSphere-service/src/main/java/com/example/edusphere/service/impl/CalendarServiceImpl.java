package com.example.edusphere.service.impl;

import com.example.edusphere.dto.request.CalendarFilterDto;
import com.example.edusphere.dto.response.CalendarEventDto;
import com.example.edusphere.entity.Assignment;
import com.example.edusphere.entity.Course;
import com.example.edusphere.entity.Event;
import com.example.common.entity.UserEntity;
import com.example.edusphere.repository.AssignmentRepository;
import com.example.edusphere.repository.CourseRepository;
import com.example.edusphere.repository.EventRepository;
import com.example.edusphere.service.CalendarService;
import com.example.common.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class CalendarServiceImpl implements CalendarService {

    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private AssignmentRepository assignmentRepository;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private UserService userService;

    @Override
    public List<CalendarEventDto> getCalendarEventsForUser(LocalDate requestedDate, CalendarFilterDto filters) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        //  Correct way: cast to your UserEntity
        UserEntity user = (UserEntity) authentication.getPrincipal();
        String userId = user.getId();

        // You can now use `user` or `userId`
        LocalDate weekStartDate = requestedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
        LocalDate weekEndDate = weekStartDate.plusDays(6);

        List<Event> eventRules = new ArrayList<>();
        List<Assignment> assignments = new ArrayList<>();

        switch (user.getRole()) {
            case "1300": // Student Role
                String learningGroupId = "Nursing_Year1_2025";
                eventRules.addAll(eventRepository.findByLearningGroupId(learningGroupId));

                List<Course> enrolledCourses = courseRepository.findByEnrollments_StudentIds(userId);
                List<String> courseIds = enrolledCourses.stream().map(Course::getId).collect(Collectors.toList());
                if (!courseIds.isEmpty()) {
                    assignments.addAll(assignmentRepository.findByCourseIn(courseIds));
                }
                break;

            case "1200": // Lecturer Role
                eventRules.addAll(eventRepository.findByInstructorId(userId));
                assignments.addAll(assignmentRepository.findByInstructorId(userId));
                break;

            case "1100": // Admin Role
            default:
                Query eventQuery = new Query();
                if (filters.getCourseId() != null && !filters.getCourseId().isEmpty()) {
                    eventQuery.addCriteria(Criteria.where("course_id").is(filters.getCourseId()));
                }
                if (filters.getInstructorId() != null && !filters.getInstructorId().isEmpty()) {
                    eventQuery.addCriteria(Criteria.where("instructor_id").is(filters.getInstructorId()));
                }
                if (filters.getGroupId() != null && !filters.getGroupId().isEmpty()) {
                    eventQuery.addCriteria(Criteria.where("learning_group_id").is(filters.getGroupId()));
                }
                eventRules.addAll(mongoTemplate.find(eventQuery, Event.class));
                assignments.addAll(assignmentRepository.findAll());
                break;
        }

        Stream<CalendarEventDto> recurringEvents = eventRules.stream()
                .flatMap(rule -> generateEventsForWeek(rule, weekStartDate, weekEndDate));

        Stream<CalendarEventDto> weeklyAssignments = assignments.stream()
                .filter(a -> a.getDueDate() != null && !a.getDueDate().isBefore(weekStartDate) && !a.getDueDate().isAfter(weekEndDate))
                .map(this::mapAssignmentToDto);

        return Stream.concat(recurringEvents, weeklyAssignments)
                .sorted(Comparator.comparing(CalendarEventDto::getDate))
                .collect(Collectors.toList());
    }

    @Override
    public Event createEvent(Event event) {
        // Here you can add validation logic in the future
        return eventRepository.save(event);
    }

    @Override
    public void deleteEvent(String eventId) {
        // This will delete the entire recurring event rule
        eventRepository.deleteById(eventId);
    }

    @Override
    public Event getEventById(String eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found with ID: " + eventId));
    }

    @Override
    public Event updateEvent(String eventId, Event eventDetails) {
        // 1. Fetch the existing event rule from the database
        Event existingEvent = getEventById(eventId);

        // 2. Copy all the updatable fields from the submitted data (eventDetails)
        //    onto the existing event object.
        existingEvent.setTitle(eventDetails.getTitle());
        existingEvent.setDescription(eventDetails.getDescription());
        existingEvent.setType(eventDetails.getType());
        existingEvent.setStartDate(eventDetails.getStartDate());
        existingEvent.setEndDate(eventDetails.getEndDate());
        existingEvent.setDayOfWeek(eventDetails.getDayOfWeek());
        existingEvent.setStartTime(eventDetails.getStartTime());
        existingEvent.setEndTime(eventDetails.getEndTime());
        existingEvent.setLocation(eventDetails.getLocation());
        existingEvent.setCourseId(eventDetails.getCourseId());
        existingEvent.setInstructorId(eventDetails.getInstructorId());
        existingEvent.setLearningGroupId(eventDetails.getLearningGroupId());

        // 3. Save the updated event back to the database
        return eventRepository.save(existingEvent);
    }

    private Stream<CalendarEventDto> generateEventsForWeek(Event rule, LocalDate weekStart, LocalDate weekEnd) {
        List<CalendarEventDto> instances = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate currentDay = weekStart.plusDays(i);
            if (currentDay.getDayOfWeek() == rule.getDayOfWeek()) {
                if (!currentDay.isBefore(rule.getStartDate()) && !currentDay.isAfter(rule.getEndDate())) {
                    instances.add(mapEventToDto(rule, currentDay));
                    break;
                }
            }
        }
        return instances.stream();
    }

    //  THIS METHOD IS NOW FIXED
    private CalendarEventDto mapEventToDto(Event rule, LocalDate specificDate) {
        String instanceId = rule.getId() + "_" + specificDate.toString();

        // --- Logic to fetch instructor details ---
        String instructorName = "N/A";
        String instructorImage = null; // Default to null
        if (rule.getInstructorId() != null && !rule.getInstructorId().isEmpty()) {
            try {
                UserEntity instructor = userService.getUserById(rule.getInstructorId());
                if (instructor != null) {
                    instructorName = instructor.getName();
                    instructorImage = instructor.getProfilePic(); // Get the profile picture URL
                }
            } catch (Exception e) {
                System.err.println("Instructor not found for ID: " + rule.getInstructorId());
            }
        }
        // --- End of new logic ---

        return CalendarEventDto.builder()
                .id(instanceId)
                .title(rule.getTitle())
                .description(rule.getDescription())
                .type(rule.getType())
                .date(specificDate)
                .startTime(rule.getStartTime())
                .endTime(rule.getEndTime())
                .location(rule.getLocation())
                .instructorName(instructorName) // Add name to DTO
                .instructorImage(instructorImage) // Add image to DTO
                .build();
    }

    private CalendarEventDto mapAssignmentToDto(Assignment assignment) {
        final String[] courseName = {"Unknown Course"};
        if (assignment.getCourse() != null) {
            Optional<Course> courseOpt = courseRepository.findById(assignment.getCourse());
            courseOpt.ifPresent(c -> courseName[0] = c.getName());
        }
        return CalendarEventDto.builder()
                .id(assignment.getId())
                .title(assignment.getTitle())
                .course(courseName[0])
                .description(assignment.getDescription())
                .type(assignment.getType())
                .date(assignment.getDueDate())
                .dueTime(assignment.getDueTime())
                .progress(assignment.getProgress())
                .build();
    }
}