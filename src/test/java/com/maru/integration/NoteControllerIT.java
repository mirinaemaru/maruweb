package com.maru.integration;

import com.maru.todo.TodoApplication;
import com.maru.note.entity.Note;
import com.maru.note.repository.NoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = {TodoApplication.class, TestConfig.class})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("NoteController 통합테스트")
class NoteControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NoteRepository noteRepository;

    @BeforeEach
    void setUp() {
        noteRepository.deleteAll();
    }

    @Test
    @DisplayName("Note 목록 조회 - 성공")
    void listNotes_Success() throws Exception {
        // Given
        Note note = new Note();
        note.setTitle("테스트 노트");
        note.setContent("테스트 내용");
        note.setDeleted("N");
        noteRepository.save(note);

        // When & Then
        mockMvc.perform(get("/notes"))
                .andExpect(status().isOk())
                .andExpect(view().name("note/list"))
                .andExpect(model().attributeExists("notes"));
    }

    @Test
    @DisplayName("Note 생성 폼 - 성공")
    void newNoteForm_Success() throws Exception {
        // When & Then
        mockMvc.perform(get("/notes/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("note/edit"))
                .andExpect(model().attributeExists("note"));
    }

    @Test
    @DisplayName("Note 생성 - 성공")
    void createNote_Success() throws Exception {
        // When
        mockMvc.perform(post("/notes")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("title", "새 노트")
                        .param("content", "노트 내용입니다.")
                        .param("category", "일반"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/notes"));

        // Then
        assertThat(noteRepository.count()).isEqualTo(1);
        Note saved = noteRepository.findAll().get(0);
        assertThat(saved.getTitle()).isEqualTo("새 노트");
        assertThat(saved.getContent()).isEqualTo("노트 내용입니다.");
        assertThat(saved.getCategory()).isEqualTo("일반");
    }

    @Test
    @DisplayName("Note 생성 - 제목 없음 실패")
    void createNote_NoTitle_Fails() throws Exception {
        // When
        mockMvc.perform(post("/notes")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("title", "")
                        .param("content", "내용만 있음"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/notes"))
                .andExpect(flash().attributeExists("error"));

        // Then
        assertThat(noteRepository.count()).isZero();
    }

    @Test
    @DisplayName("Note 상세 조회 - 성공")
    void viewNote_Success() throws Exception {
        // Given
        Note note = new Note();
        note.setTitle("상세 조회 노트");
        note.setContent("상세 내용");
        note.setDeleted("N");
        Note saved = noteRepository.save(note);

        // When & Then
        mockMvc.perform(get("/notes/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("note/view"))
                .andExpect(model().attributeExists("note"));
    }

    @Test
    @DisplayName("Note 수정 폼 - 성공")
    void editNoteForm_Success() throws Exception {
        // Given
        Note note = new Note();
        note.setTitle("수정할 노트");
        note.setContent("기존 내용");
        note.setDeleted("N");
        Note saved = noteRepository.save(note);

        // When & Then
        mockMvc.perform(get("/notes/{id}/edit", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("note/edit"))
                .andExpect(model().attributeExists("note"));
    }

    @Test
    @DisplayName("Note 수정 - 성공")
    void updateNote_Success() throws Exception {
        // Given
        Note note = new Note();
        note.setTitle("기존 노트");
        note.setContent("기존 내용");
        note.setDeleted("N");
        Note saved = noteRepository.save(note);

        // When
        mockMvc.perform(post("/notes/{id}", saved.getId())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("title", "수정된 노트")
                        .param("content", "수정된 내용"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/notes/" + saved.getId()));

        // Then
        Optional<Note> updated = noteRepository.findById(saved.getId());
        assertThat(updated).isPresent();
        assertThat(updated.get().getTitle()).isEqualTo("수정된 노트");
        assertThat(updated.get().getContent()).isEqualTo("수정된 내용");
    }

    @Test
    @DisplayName("Note 고정 토글 - 성공")
    void togglePin_Success() throws Exception {
        // Given
        Note note = new Note();
        note.setTitle("고정할 노트");
        note.setPinned("N");
        note.setDeleted("N");
        Note saved = noteRepository.save(note);

        // When
        mockMvc.perform(post("/notes/{id}/pin", saved.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/notes"));

        // Then
        Optional<Note> pinned = noteRepository.findById(saved.getId());
        assertThat(pinned).isPresent();
        assertThat(pinned.get().getPinned()).isEqualTo("Y");
    }

    @Test
    @DisplayName("Note 삭제 - 성공 (소프트 삭제)")
    void deleteNote_Success() throws Exception {
        // Given
        Note note = new Note();
        note.setTitle("삭제할 노트");
        note.setDeleted("N");
        Note saved = noteRepository.save(note);

        // When
        mockMvc.perform(post("/notes/{id}/delete", saved.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/notes"))
                .andExpect(flash().attributeExists("success"));

        // Then - 소프트 삭제이므로 레코드는 존재
        Optional<Note> deleted = noteRepository.findById(saved.getId());
        assertThat(deleted).isPresent();
        assertThat(deleted.get().getDeleted()).isEqualTo("Y");
    }

    @Test
    @DisplayName("Note 카테고리 필터링 - 성공")
    void listNotes_FilterByCategory() throws Exception {
        // Given
        Note note1 = new Note();
        note1.setTitle("업무 노트");
        note1.setCategory("업무");
        note1.setDeleted("N");
        noteRepository.save(note1);

        Note note2 = new Note();
        note2.setTitle("개인 노트");
        note2.setCategory("개인");
        note2.setDeleted("N");
        noteRepository.save(note2);

        // When & Then
        mockMvc.perform(get("/notes")
                        .param("category", "업무"))
                .andExpect(status().isOk())
                .andExpect(view().name("note/list"))
                .andExpect(model().attribute("selectedCategory", "업무"));
    }

    @Test
    @DisplayName("Note 검색 - 성공")
    void listNotes_Search() throws Exception {
        // Given
        Note note = new Note();
        note.setTitle("검색될 노트");
        note.setContent("특정 키워드가 있는 내용");
        note.setDeleted("N");
        noteRepository.save(note);

        // When & Then
        mockMvc.perform(get("/notes")
                        .param("keyword", "키워드"))
                .andExpect(status().isOk())
                .andExpect(view().name("note/list"))
                .andExpect(model().attribute("keyword", "키워드"));
    }
}
