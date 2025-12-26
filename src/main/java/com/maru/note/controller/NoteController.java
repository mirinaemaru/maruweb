package com.maru.note.controller;

import com.maru.note.entity.Note;
import com.maru.note.service.NoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.util.List;

@Controller
@RequestMapping("/notes")
@RequiredArgsConstructor
public class NoteController {

    private final NoteService noteService;

    @GetMapping
    public String list(@RequestParam(required = false) String category,
                      @RequestParam(required = false) String keyword,
                      Model model) {
        List<Note> notes;

        if (keyword != null && !keyword.trim().isEmpty()) {
            notes = noteService.searchNotes(keyword.trim());
        } else if (category != null && !category.isEmpty()) {
            notes = noteService.getNotesByCategory(category);
        } else {
            notes = noteService.getAllNotes();
        }

        model.addAttribute("notes", notes);
        model.addAttribute("categories", noteService.getAllCategories());
        model.addAttribute("selectedCategory", category);
        model.addAttribute("keyword", keyword);
        model.addAttribute("newNote", new Note());
        return "note/list";
    }

    @GetMapping("/new")
    public String newNoteForm(Model model) {
        model.addAttribute("note", new Note());
        model.addAttribute("categories", noteService.getAllCategories());
        return "note/edit";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("newNote") Note note,
                        BindingResult result,
                        RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Title is required");
            return "redirect:/notes";
        }
        noteService.createNote(note);
        redirectAttributes.addFlashAttribute("success", "Note created successfully");
        return "redirect:/notes";
    }

    @GetMapping("/{id}")
    public String view(@PathVariable Long id, Model model) {
        return noteService.getNoteById(id)
                .map(note -> {
                    model.addAttribute("note", note);
                    return "note/view";
                })
                .orElse("redirect:/notes");
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        return noteService.getNoteById(id)
                .map(note -> {
                    model.addAttribute("note", note);
                    model.addAttribute("categories", noteService.getAllCategories());
                    return "note/edit";
                })
                .orElse("redirect:/notes");
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                        @Valid @ModelAttribute Note note,
                        BindingResult result,
                        RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Title is required");
            return "redirect:/notes/" + id + "/edit";
        }
        noteService.updateNote(id, note);
        redirectAttributes.addFlashAttribute("success", "Note updated successfully");
        return "redirect:/notes/" + id;
    }

    @PostMapping("/{id}/pin")
    public String togglePin(@PathVariable Long id) {
        noteService.togglePin(id);
        return "redirect:/notes";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        noteService.deleteNote(id);
        redirectAttributes.addFlashAttribute("success", "Note deleted successfully");
        return "redirect:/notes";
    }
}
