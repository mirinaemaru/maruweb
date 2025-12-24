package com.maru.calendar.repository;

import com.maru.calendar.entity.CalendarEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CalendarEventRepository extends JpaRepository<CalendarEvent, Long> {

    // Find events within date range (for monthly view)
    List<CalendarEvent> findByDeletedAndStartDateTimeBetweenOrderByStartDateTimeAsc(
            String deleted, LocalDateTime start, LocalDateTime end);

    // Find all non-deleted events
    List<CalendarEvent> findByDeletedOrderByStartDateTimeAsc(String deleted);

    // Find events for a specific day
    List<CalendarEvent> findByDeletedAndStartDateTimeBetween(
            String deleted, LocalDateTime dayStart, LocalDateTime dayEnd);

    // Find events pending sync
    List<CalendarEvent> findBySyncStatus(String syncStatus);

    // Find event by Google Event ID
    CalendarEvent findByGoogleEventId(String googleEventId);

    // Find events that need to be synced (PENDING status)
    List<CalendarEvent> findBySyncStatusIn(List<String> syncStatuses);
}
