package com.maru.todo.controller;

import com.maru.todo.entity.Todo;
import com.maru.todo.service.TodoService;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequestMapping("/todos")
@RequiredArgsConstructor
public class TodoController {

    private final TodoService todoService;

    @GetMapping
    public String listTodos(@RequestParam(required = false) String filter,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                           Model model) {

        // 기본 날짜 설정 (최근 30일)
        if (from == null) {
            from = LocalDate.now().minusDays(30);
        }
        if (to == null) {
            to = LocalDate.now();
        }

        if ("completed".equals(filter)) {
            model.addAttribute("todos", todoService.getTodosByStatusWithDateRange("Y", from, to));
            model.addAttribute("filter", "completed");
        } else if ("all".equals(filter)) {
            model.addAttribute("todos", todoService.getAllTodosWithDateRange(from, to));
            model.addAttribute("filter", "all");
        } else {
            // 기본값은 active (날짜 필터 없음)
            model.addAttribute("todos", todoService.getTodosByStatus("N"));
            model.addAttribute("filter", "active");
        }

        model.addAttribute("todo", new Todo());
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        return "todo/list";
    }

    @PostMapping
    public String createTodo(@Valid @ModelAttribute Todo todo,
                           BindingResult result,
                           RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Title is required");
            return "redirect:/todos";
        }
        todoService.createTodo(todo);
        redirectAttributes.addFlashAttribute("success", "Todo created successfully");
        return "redirect:/todos";
    }

    @GetMapping("/{id}/edit")
    public String editTodoForm(@PathVariable Long id, Model model) {
        return todoService.getTodoById(id)
                .map(todo -> {
                    model.addAttribute("todo", todo);
                    return "todo/edit";
                })
                .orElse("redirect:/todos");
    }

    @PostMapping("/{id}")
    public String updateTodo(@PathVariable Long id,
                           @Valid @ModelAttribute Todo todo,
                           BindingResult result,
                           RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Title is required");
            return "redirect:/todos/" + id + "/edit";
        }
        todoService.updateTodo(id, todo);
        redirectAttributes.addFlashAttribute("success", "Todo updated successfully");
        return "redirect:/todos";
    }

    @PostMapping("/{id}/toggle")
    public String toggleComplete(@PathVariable Long id) {
        todoService.toggleComplete(id);
        return "redirect:/todos";
    }

    @PostMapping("/{id}/delete")
    public String deleteTodo(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        todoService.deleteTodo(id);
        redirectAttributes.addFlashAttribute("success", "Todo deleted successfully");
        return "redirect:/todos";
    }

    @PostMapping("/{id}/description")
    public String updateDescription(@PathVariable Long id,
                                   @RequestParam String description,
                                   @RequestParam(required = false) String filter) {
        todoService.updateDescription(id, description);
        if (filter != null) {
            return "redirect:/todos?filter=" + filter;
        }
        return "redirect:/todos";
    }
}
