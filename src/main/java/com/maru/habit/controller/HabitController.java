package com.maru.habit.controller;

import com.maru.habit.entity.Habit;
import com.maru.habit.service.HabitService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/habits")
@RequiredArgsConstructor
public class HabitController {

    private final HabitService habitService;

    @GetMapping
    public String list(@RequestParam(required = false) Integer year,
                      @RequestParam(required = false) Integer month,
                      Model model) {
        LocalDate now = LocalDate.now();
        int displayYear = year != null ? year : now.getYear();
        int displayMonth = month != null ? month : now.getMonthValue();

        YearMonth yearMonth = YearMonth.of(displayYear, displayMonth);
        int daysInMonth = yearMonth.lengthOfMonth();

        // Generate list of days for the month
        List<LocalDate> days = new ArrayList<>();
        for (int i = 1; i <= daysInMonth; i++) {
            days.add(LocalDate.of(displayYear, displayMonth, i));
        }

        model.addAttribute("habits", habitService.getAllHabits());
        model.addAttribute("records", habitService.getRecordsForMonth(displayYear, displayMonth));
        model.addAttribute("numericRecords", habitService.getNumericRecordsForMonth(displayYear, displayMonth));
        model.addAttribute("counts", habitService.getMonthlyCounts(displayYear, displayMonth));
        model.addAttribute("days", days);
        model.addAttribute("year", displayYear);
        model.addAttribute("month", displayMonth);
        model.addAttribute("today", now);
        model.addAttribute("newHabit", new Habit());

        // Calculate prev/next month
        YearMonth prevMonth = yearMonth.minusMonths(1);
        YearMonth nextMonth = yearMonth.plusMonths(1);
        model.addAttribute("prevYear", prevMonth.getYear());
        model.addAttribute("prevMonth", prevMonth.getMonthValue());
        model.addAttribute("nextYear", nextMonth.getYear());
        model.addAttribute("nextMonth", nextMonth.getMonthValue());

        return "habit/list";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("newHabit") Habit habit,
                        BindingResult result,
                        RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Habit name is required");
            return "redirect:/habits";
        }
        habitService.createHabit(habit);
        redirectAttributes.addFlashAttribute("success", "Habit created successfully");
        return "redirect:/habits";
    }

    @PostMapping("/{id}/toggle")
    public String toggleRecord(@PathVariable Long id,
                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                              @RequestParam(required = false) Integer year,
                              @RequestParam(required = false) Integer month) {
        habitService.toggleRecord(id, date);
        String redirect = "redirect:/habits";
        if (year != null && month != null) {
            redirect += "?year=" + year + "&month=" + month;
        }
        return redirect;
    }

    @PostMapping("/{id}/numeric")
    public String saveNumericRecord(@PathVariable Long id,
                                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                   @RequestParam(required = false) Double value,
                                   @RequestParam(required = false) Integer year,
                                   @RequestParam(required = false) Integer month) {
        habitService.saveNumericRecord(id, date, value);
        String redirect = "redirect:/habits";
        if (year != null && month != null) {
            redirect += "?year=" + year + "&month=" + month;
        }
        return redirect;
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        return habitService.getHabitById(id)
                .map(habit -> {
                    model.addAttribute("habit", habit);
                    return "habit/edit";
                })
                .orElse("redirect:/habits");
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                        @Valid @ModelAttribute Habit habit,
                        BindingResult result,
                        RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Habit name is required");
            return "redirect:/habits/" + id + "/edit";
        }
        habitService.updateHabit(id, habit);
        redirectAttributes.addFlashAttribute("success", "Habit updated successfully");
        return "redirect:/habits";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        habitService.deleteHabit(id);
        redirectAttributes.addFlashAttribute("success", "Habit deleted successfully");
        return "redirect:/habits";
    }
}
