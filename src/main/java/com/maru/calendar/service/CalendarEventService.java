package com.maru.calendar.service;

import com.maru.calendar.entity.CalendarEvent;
import com.maru.calendar.repository.CalendarEventRepository;
import com.maru.dday.entity.DDay;
import com.maru.dday.service.DDayService;
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
    private final DDayService ddayService;

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

        // D-Day 연동: ddayIcon이 선택되어 있으면 D-Day도 생성
        if (saved.getDdayIcon() != null && !saved.getDdayIcon().isEmpty()) {
            DDay dday = new DDay();
            dday.setTitle(saved.getTitle());
            dday.setDescription(saved.getDescription());
            dday.setTargetDate(saved.getStartDateTime().toLocalDate());
            dday.setIcon(saved.getDdayIcon());
            DDay savedDDay = ddayService.createDDay(dday);

            // CalendarEvent에 연동된 D-Day ID 저장
            saved.setDdayId(savedDDay.getId());
            calendarEventRepository.save(saved);
        }

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

                    // D-Day 연동 처리
                    String oldIcon = event.getDdayIcon();
                    String newIcon = updatedEvent.getDdayIcon();

                    if (newIcon != null && !newIcon.isEmpty()) {
                        // 새로운 아이콘이 선택됨
                        if (event.getDdayId() != null) {
                            // 기존 D-Day가 있으면 업데이트
                            DDay existingDDay = new DDay();
                            existingDDay.setTitle(event.getTitle());
                            existingDDay.setDescription(event.getDescription());
                            existingDDay.setTargetDate(event.getStartDateTime().toLocalDate());
                            existingDDay.setIcon(newIcon);
                            ddayService.updateDDay(event.getDdayId(), existingDDay);
                        } else {
                            // 기존 D-Day가 없으면 새로 생성
                            DDay newDDay = new DDay();
                            newDDay.setTitle(event.getTitle());
                            newDDay.setDescription(event.getDescription());
                            newDDay.setTargetDate(event.getStartDateTime().toLocalDate());
                            newDDay.setIcon(newIcon);
                            DDay savedDDay = ddayService.createDDay(newDDay);
                            event.setDdayId(savedDDay.getId());
                        }
                        event.setDdayIcon(newIcon);
                    } else {
                        // 아이콘이 제거됨
                        if (event.getDdayId() != null) {
                            // 연동된 D-Day 삭제
                            ddayService.deleteDDay(event.getDdayId());
                            event.setDdayId(null);
                        }
                        event.setDdayIcon(null);
                    }

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
                    // 연동된 D-Day도 삭제
                    if (event.getDdayId() != null) {
                        ddayService.deleteDDay(event.getDdayId());
                    }

                    event.setDeleted("Y");
                    syncService.markEventForSync(event); // Mark for sync (will delete from Google)
                    calendarEventRepository.save(event);
                });
    }
}
