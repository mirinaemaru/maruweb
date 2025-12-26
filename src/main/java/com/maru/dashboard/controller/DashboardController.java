package com.maru.dashboard.controller;

import com.maru.calendar.entity.CalendarEvent;
import com.maru.calendar.service.CalendarEventService;
import com.maru.dday.entity.DDay;
import com.maru.dday.service.DDayService;
import com.maru.shortcut.entity.ShortcutCategory;
import com.maru.shortcut.service.ShortcutService;
import com.maru.todo.entity.Todo;
import com.maru.todo.service.TodoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final TodoService todoService;
    private final CalendarEventService calendarEventService;
    private final ShortcutService shortcutService;
    private final DDayService ddayService;

    @GetMapping("/")
    public String dashboard(Model model) {
        // Active Todos (not completed, limit to 5)
        List<Todo> activeTodos = todoService.getTodosByStatus("N")
                .stream()
                .limit(5)
                .collect(Collectors.toList());
        long activeTodoCount = todoService.getTodosByStatus("N").size();

        // Today's events
        List<CalendarEvent> todayEvents = calendarEventService.getEventsForDay(LocalDate.now());

        // Upcoming events (next 7 days, limit to 5)
        List<CalendarEvent> upcomingEvents = calendarEventService.getEventsForMonth(
                        LocalDate.now().getYear(),
                        LocalDate.now().getMonthValue())
                .stream()
                .filter(e -> e.getStartDateTime().isAfter(LocalDateTime.now()))
                .limit(5)
                .collect(Collectors.toList());

        // Shortcut categories with shortcuts (limit to 3 categories)
        List<ShortcutCategory> shortcutCategories = shortcutService.getAllCategoriesWithShortcuts()
                .stream()
                .limit(3)
                .collect(Collectors.toList());

        // Upcoming D-Days (limit to 5)
        List<DDay> upcomingDDays = ddayService.getUpcomingDDays()
                .stream()
                .limit(5)
                .collect(Collectors.toList());

        model.addAttribute("activeTodos", activeTodos);
        model.addAttribute("activeTodoCount", activeTodoCount);
        model.addAttribute("todayEvents", todayEvents);
        model.addAttribute("upcomingEvents", upcomingEvents);
        model.addAttribute("shortcutCategories", shortcutCategories);
        model.addAttribute("upcomingDDays", upcomingDDays);
        model.addAttribute("today", LocalDate.now());

        return "dashboard/index";
    }
}
