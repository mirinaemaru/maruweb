package com.maru.calendar.controller;

import com.maru.calendar.dto.CalendarGridData;
import com.maru.calendar.entity.CalendarEvent;
import com.maru.calendar.service.CalendarEventService;
import com.maru.calendar.service.GoogleOAuthService;
import com.maru.calendar.service.GoogleCalendarSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Controller
@RequestMapping("/calendar")
@RequiredArgsConstructor
public class CalendarController {

    private final CalendarEventService eventService;
    private final GoogleOAuthService oauthService;
    private final GoogleCalendarSyncService syncService;

    // Main calendar view (monthly)
    @GetMapping
    public String viewCalendar(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            Model model) {

        // Default to current month
        LocalDate now = LocalDate.now();
        int displayYear = year != null ? year : now.getYear();
        int displayMonth = month != null ? month : now.getMonthValue();

        // Get events for the month
        List<CalendarEvent> events = eventService.getEventsForMonth(displayYear, displayMonth);

        // Build calendar grid data
        CalendarGridData gridData = new CalendarGridData(displayYear, displayMonth, events);

        model.addAttribute("year", displayYear);
        model.addAttribute("month", displayMonth);
        model.addAttribute("gridData", gridData);
        model.addAttribute("events", events);
        model.addAttribute("calendarEvent", new CalendarEvent());
        model.addAttribute("isGoogleConnected", oauthService.isAuthenticated());

        return "calendar/view";
    }

    // Create new event
    @PostMapping("/events")
    public String createEvent(
            @Valid @ModelAttribute CalendarEvent event,
            BindingResult result,
            RedirectAttributes redirectAttributes) {

        log.info("Creating event: title={}, start={}, end={}, allDay={}",
                event.getTitle(), event.getStartDateTime(), event.getEndDateTime(), event.getAllDay());

        if (result.hasErrors()) {
            log.error("Validation errors: {}", result.getAllErrors());
            redirectAttributes.addFlashAttribute("error", "Invalid event data: " + result.getAllErrors().toString());
            return "redirect:/calendar";
        }

        // Handle all-day events
        if ("Y".equals(event.getAllDay())) {
            // Set time to start/end of day for all-day events
            event.setStartDateTime(event.getStartDateTime().toLocalDate().atStartOfDay());
            event.setEndDateTime(event.getEndDateTime().toLocalDate().atTime(23, 59, 59));
        }

        eventService.createEvent(event);
        redirectAttributes.addFlashAttribute("success", "Event created successfully");

        return "redirect:/calendar";
    }

    // View/Edit event
    @GetMapping("/events/{id}")
    public String viewEvent(@PathVariable Long id, Model model) {
        return eventService.getEventById(id)
                .map(event -> {
                    model.addAttribute("event", event);
                    return "calendar/event-detail";
                })
                .orElse("redirect:/calendar");
    }

    // Update event
    @PostMapping("/events/{id}")
    public String updateEvent(
            @PathVariable Long id,
            @Valid @ModelAttribute CalendarEvent event,
            BindingResult result,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Invalid event data");
            return "redirect:/calendar/events/" + id;
        }

        // Handle all-day events
        if ("Y".equals(event.getAllDay())) {
            event.setStartDateTime(event.getStartDateTime().toLocalDate().atStartOfDay());
            event.setEndDateTime(event.getEndDateTime().toLocalDate().atTime(23, 59, 59));
        }

        eventService.updateEvent(id, event);
        redirectAttributes.addFlashAttribute("success", "Event updated successfully");

        return "redirect:/calendar";
    }

    // Delete event
    @PostMapping("/events/{id}/delete")
    public String deleteEvent(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        eventService.deleteEvent(id);
        redirectAttributes.addFlashAttribute("success", "Event deleted successfully");
        return "redirect:/calendar";
    }
}
