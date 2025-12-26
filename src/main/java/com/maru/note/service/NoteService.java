package com.maru.note.service;

import com.maru.note.entity.Note;
import com.maru.note.repository.NoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoteService {

    private final NoteRepository noteRepository;

    public List<Note> getAllNotes() {
        return noteRepository.findByDeletedOrderByPinnedDescUpdatedAtDesc("N");
    }

    public List<Note> getNotesByCategory(String category) {
        return noteRepository.findByDeletedAndCategoryOrderByPinnedDescUpdatedAtDesc("N", category);
    }

    public List<Note> searchNotes(String keyword) {
        return noteRepository.searchByKeyword(keyword);
    }

    public List<String> getAllCategories() {
        return noteRepository.findAllCategories();
    }

    public Optional<Note> getNoteById(Long id) {
        return noteRepository.findById(id)
                .filter(note -> "N".equals(note.getDeleted()));
    }

    @Transactional
    public Note createNote(Note note) {
        return noteRepository.save(note);
    }

    @Transactional
    public Note updateNote(Long id, Note updatedNote) {
        return noteRepository.findById(id)
                .filter(note -> "N".equals(note.getDeleted()))
                .map(note -> {
                    note.setTitle(updatedNote.getTitle());
                    note.setContent(updatedNote.getContent());
                    note.setCategory(updatedNote.getCategory());
                    return noteRepository.save(note);
                })
                .orElseThrow(() -> new IllegalArgumentException("Note not found with id: " + id));
    }

    @Transactional
    public void togglePin(Long id) {
        noteRepository.findById(id)
                .filter(note -> "N".equals(note.getDeleted()))
                .ifPresent(note -> {
                    note.setPinned("Y".equals(note.getPinned()) ? "N" : "Y");
                    noteRepository.save(note);
                });
    }

    @Transactional
    public void deleteNote(Long id) {
        noteRepository.findById(id)
                .filter(note -> "N".equals(note.getDeleted()))
                .ifPresent(note -> {
                    note.setDeleted("Y");
                    noteRepository.save(note);
                });
    }
}
