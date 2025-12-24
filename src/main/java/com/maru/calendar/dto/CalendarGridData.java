package com.maru.calendar.dto;

import com.maru.calendar.entity.CalendarEvent;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class CalendarGridData {
    private int year;
    private int month;
    private String monthYearDisplay;
    private LocalDate previousMonth;
    private LocalDate nextMonth;
    private List<List<CalendarDayData>> weeks;

    public CalendarGridData(int year, int month, List<CalendarEvent> events) {
        this.year = year;
        this.month = month;

        LocalDate date = LocalDate.of(year, month, 1);
        this.monthYearDisplay = date.format(DateTimeFormatter.ofPattern("MMMM yyyy"));
        this.previousMonth = date.minusMonths(1);
        this.nextMonth = date.plusMonths(1);

        this.weeks = buildWeeks(date, events);
    }

    private List<List<CalendarDayData>> buildWeeks(LocalDate monthStart, List<CalendarEvent> events) {
        List<List<CalendarDayData>> weeks = new ArrayList<>();

        // Find the first day to display (might be from previous month)
        LocalDate current = monthStart.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
        LocalDate monthEnd = monthStart.with(TemporalAdjusters.lastDayOfMonth());

        // Build 6 weeks (42 days) to ensure consistent grid
        for (int week = 0; week < 6; week++) {
            List<CalendarDayData> weekDays = new ArrayList<>();

            for (int day = 0; day < 7; day++) {
                CalendarDayData dayData = new CalendarDayData(
                        current,
                        current.getMonth() == monthStart.getMonth(),
                        current.equals(LocalDate.now()),
                        getEventsForDay(current, events)
                );
                weekDays.add(dayData);
                current = current.plusDays(1);
            }

            weeks.add(weekDays);

            // Stop if we've passed the month end and filled at least 5 weeks
            if (week >= 4 && current.isAfter(monthEnd)) {
                break;
            }
        }

        return weeks;
    }

    private List<CalendarEvent> getEventsForDay(LocalDate date, List<CalendarEvent> allEvents) {
        return allEvents.stream()
                .filter(event -> {
                    LocalDate eventDate = event.getStartDateTime().toLocalDate();
                    return eventDate.equals(date);
                })
                .collect(Collectors.toList());
    }
}

@Data
@AllArgsConstructor
class CalendarDayData {
    private LocalDate date;
    private boolean isCurrentMonth;
    private boolean isToday;
    private List<CalendarEvent> events;

    public int getDayOfMonth() {
        return date.getDayOfMonth();
    }
}
