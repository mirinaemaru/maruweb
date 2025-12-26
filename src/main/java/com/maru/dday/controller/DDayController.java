package com.maru.dday.controller;

import com.maru.dday.entity.DDay;
import com.maru.dday.service.DDayService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;

@Controller
@RequestMapping("/dday")
@RequiredArgsConstructor
public class DDayController {

    private final DDayService ddayService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("upcomingDDays", ddayService.getUpcomingDDays());
        model.addAttribute("pastDDays", ddayService.getPastDDays());
        model.addAttribute("newDDay", new DDay());
        return "dday/list";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("newDDay") DDay dday,
                        BindingResult result,
                        RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Title and target date are required");
            return "redirect:/dday";
        }
        ddayService.createDDay(dday);
        redirectAttributes.addFlashAttribute("success", "D-Day created successfully");
        return "redirect:/dday";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        return ddayService.getDDayById(id)
                .map(dday -> {
                    model.addAttribute("dday", dday);
                    return "dday/edit";
                })
                .orElse("redirect:/dday");
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                        @Valid @ModelAttribute DDay dday,
                        BindingResult result,
                        RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Title and target date are required");
            return "redirect:/dday/" + id + "/edit";
        }
        ddayService.updateDDay(id, dday);
        redirectAttributes.addFlashAttribute("success", "D-Day updated successfully");
        return "redirect:/dday";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        ddayService.deleteDDay(id);
        redirectAttributes.addFlashAttribute("success", "D-Day deleted successfully");
        return "redirect:/dday";
    }
}
