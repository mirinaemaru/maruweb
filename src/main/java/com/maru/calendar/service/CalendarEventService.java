package com.maru.calendar.service;

import com.maru.calendar.entity.CalendarEvent;
import com.maru.calendar.repository.CalendarEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CalendarEventService {

    private final CalendarEventRepository calendarEventRepository;
    private final GoogleCalendarSyncService syncService;

    // Get events for a specific month
    public List<CalendarEvent> getEventsForMonth(int year, int month) {
        LocalDateTime monthStart = LocalDateTime.of(year, month, 1, 0, 0);
        LocalDateTime monthEnd = monthStart.plusMonths(1).minusSeconds(1);
        return calendarEventRepository.findByDeletedAndStartDateTimeBetweenOrderByStartDateTimeAsc(
                "N", monthStart, monthEnd);
    }

    // Get events for a specific day
    public List<CalendarEvent> getEventsForDay(LocalDate date) {
        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = date.atTime(LocalTime.MAX);
        return calendarEventRepository.findByDeletedAndStartDateTimeBetween("N", dayStart, dayEnd);
    }

    // Get all events
    public List<CalendarEvent> getAllEvents() {
        return calendarEventRepository.findByDeletedOrderByStartDateTimeAsc("N");
    }

    // Get event by ID
    public Optional<CalendarEvent> getEventById(Long id) {
        return calendarEventRepository.findById(id)
                .filter(event -> "N".equals(event.getDeleted()));
    }

    // Create event
    @Transactional
    public CalendarEvent createEvent(CalendarEvent event) {
        event.setSyncStatus("PENDING"); // Mark for sync to Google
        CalendarEvent saved = calendarEventRepository.save(event);
        syncService.markEventForSync(saved);
        return saved;
    }

    // Update event
    @Transactional
    public CalendarEvent updateEvent(Long id, CalendarEvent updatedEvent) {
        return calendarEventRepository.findById(id)
                .filter(event -> "N".equals(event.getDeleted()))
                .map(event -> {
                    event.setTitle(updatedEvent.getTitle());
                    event.setDescription(updatedEvent.getDescription());
                    event.setStartDateTime(updatedEvent.getStartDateTime());
                    event.setEndDateTime(updatedEvent.getEndDateTime());
                    event.setAllDay(updatedEvent.getAllDay());
                    event.setLocation(updatedEvent.getLocation());
                    syncService.markEventForSync(event); // Mark for sync
                    return calendarEventRepository.save(event);
                })
                .orElseThrow(() -> new IllegalArgumentException("Event not found with id: " + id));
    }

    // Soft delete
    @Transactional
    public void deleteEvent(Long id) {
        calendarEventRepository.findById(id)
                .filter(event -> "N".equals(event.getDeleted()))
                .ifPresent(event -> {
                    event.setDeleted("Y");
                    syncService.markEventForSync(event); // Mark for sync (will delete from Google)
                    calendarEventRepository.save(event);
                });
    }
}
