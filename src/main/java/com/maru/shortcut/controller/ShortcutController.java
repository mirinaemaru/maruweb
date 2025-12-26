package com.maru.shortcut.controller;

import com.maru.shortcut.entity.Shortcut;
import com.maru.shortcut.entity.ShortcutCategory;
import com.maru.shortcut.service.ShortcutService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.util.List;

@Controller
@RequestMapping("/shortcuts")
@RequiredArgsConstructor
public class ShortcutController {

    private final ShortcutService shortcutService;

    /**
     * Display shortcuts list page with all categories and their shortcuts
     */
    @GetMapping
    public String list(Model model) {
        List<ShortcutCategory> categories = shortcutService.getAllCategoriesWithShortcuts();
        model.addAttribute("categories", categories);
        model.addAttribute("newCategory", new ShortcutCategory());
        model.addAttribute("newShortcut", new Shortcut());
        return "shortcut/list";
    }

    // ========== Category CRUD ==========

    @PostMapping("/categories")
    public String createCategory(@Valid @ModelAttribute("newCategory") ShortcutCategory category,
                                  BindingResult result,
                                  RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Category name is required");
            return "redirect:/shortcuts";
        }
        shortcutService.createCategory(category);
        redirectAttributes.addFlashAttribute("success", "Category created successfully");
        return "redirect:/shortcuts";
    }

    @PostMapping("/categories/{id}")
    public String updateCategory(@PathVariable Long id,
                                  @ModelAttribute ShortcutCategory category,
                                  RedirectAttributes redirectAttributes) {
        try {
            shortcutService.updateCategory(id, category);
            redirectAttributes.addFlashAttribute("success", "Category updated successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to update category: " + e.getMessage());
        }
        return "redirect:/shortcuts?tab=" + id;
    }

    @PostMapping("/categories/{id}/delete")
    public String deleteCategory(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            shortcutService.deleteCategory(id);
            redirectAttributes.addFlashAttribute("success", "Category deleted successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to delete category: " + e.getMessage());
        }
        return "redirect:/shortcuts";
    }

    // ========== Shortcut CRUD ==========

    @PostMapping("/items")
    public String createShortcut(@RequestParam Long categoryId,
                                  @Valid @ModelAttribute("newShortcut") Shortcut shortcut,
                                  BindingResult result,
                                  RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Name and URL are required");
            return "redirect:/shortcuts";
        }

        ShortcutCategory category = shortcutService.getCategoryById(categoryId)
                .orElse(null);
        if (category == null) {
            redirectAttributes.addFlashAttribute("error", "Category not found");
            return "redirect:/shortcuts";
        }

        shortcut.setCategory(category);
        shortcutService.createShortcut(shortcut);
        redirectAttributes.addFlashAttribute("success", "Shortcut created successfully");
        return "redirect:/shortcuts?tab=" + categoryId;
    }

    @PostMapping("/items/{id}")
    public String updateShortcut(@PathVariable Long id,
                                  @RequestParam(required = false) Long categoryId,
                                  @ModelAttribute Shortcut shortcut,
                                  RedirectAttributes redirectAttributes) {
        try {
            if (categoryId != null) {
                ShortcutCategory category = shortcutService.getCategoryById(categoryId)
                        .orElseThrow(() -> new RuntimeException("Category not found"));
                shortcut.setCategory(category);
            }
            shortcutService.updateShortcut(id, shortcut);
            redirectAttributes.addFlashAttribute("success", "Shortcut updated successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to update shortcut: " + e.getMessage());
        }
        return "redirect:/shortcuts?tab=" + categoryId;
    }

    @PostMapping("/items/{id}/delete")
    public String deleteShortcut(@PathVariable Long id,
                                  @RequestParam(required = false) Long categoryId,
                                  RedirectAttributes redirectAttributes) {
        try {
            shortcutService.deleteShortcut(id);
            redirectAttributes.addFlashAttribute("success", "Shortcut deleted successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to delete shortcut: " + e.getMessage());
        }
        return "redirect:/shortcuts" + (categoryId != null ? "?tab=" + categoryId : "");
    }
}
