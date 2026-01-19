package com.maru.note.service;

import com.maru.note.entity.Note;
import com.maru.note.repository.NoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NoteService 단위 테스트")
class NoteServiceTest {

    @Mock
    private NoteRepository noteRepository;

    private NoteService noteService;

    @BeforeEach
    void setUp() {
        noteService = new NoteService(noteRepository);
    }

    private Note createNote(Long id, String title, String category, String pinned) {
        Note note = new Note();
        note.setId(id);
        note.setTitle(title);
        note.setContent("노트 내용");
        note.setCategory(category);
        note.setPinned(pinned);
        note.setDeleted("N");
        note.setUpdatedAt(LocalDateTime.now());
        return note;
    }

    // ==================== getAllNotes Tests ====================

    @Nested
    @DisplayName("getAllNotes")
    class GetAllNotesTests {

        @Test
        @DisplayName("모든 노트 조회 - 성공")
        void getAllNotes_Success() {
            // given
            Note note1 = createNote(1L, "노트 1", "업무", "Y");
            Note note2 = createNote(2L, "노트 2", "개인", "N");
            when(noteRepository.findByDeletedOrderByPinnedDescUpdatedAtDesc("N"))
                    .thenReturn(Arrays.asList(note1, note2));

            // when
            List<Note> result = noteService.getAllNotes();

            // then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getPinned()).isEqualTo("Y"); // 고정된 노트가 먼저
        }

        @Test
        @DisplayName("모든 노트 조회 - 빈 목록")
        void getAllNotes_Empty() {
            // given
            when(noteRepository.findByDeletedOrderByPinnedDescUpdatedAtDesc("N"))
                    .thenReturn(Collections.emptyList());

            // when
            List<Note> result = noteService.getAllNotes();

            // then
            assertThat(result).isEmpty();
        }
    }

    // ==================== getNotesByCategory Tests ====================

    @Nested
    @DisplayName("getNotesByCategory")
    class GetNotesByCategoryTests {

        @Test
        @DisplayName("카테고리별 노트 조회 - 성공")
        void getNotesByCategory_Success() {
            // given
            Note note1 = createNote(1L, "업무 노트 1", "업무", "N");
            Note note2 = createNote(2L, "업무 노트 2", "업무", "N");
            when(noteRepository.findByDeletedAndCategoryOrderByPinnedDescUpdatedAtDesc("N", "업무"))
                    .thenReturn(Arrays.asList(note1, note2));

            // when
            List<Note> result = noteService.getNotesByCategory("업무");

            // then
            assertThat(result).hasSize(2);
            assertThat(result).allMatch(note -> "업무".equals(note.getCategory()));
        }

        @Test
        @DisplayName("카테고리별 노트 조회 - 빈 결과")
        void getNotesByCategory_Empty() {
            // given
            when(noteRepository.findByDeletedAndCategoryOrderByPinnedDescUpdatedAtDesc("N", "없는카테고리"))
                    .thenReturn(Collections.emptyList());

            // when
            List<Note> result = noteService.getNotesByCategory("없는카테고리");

            // then
            assertThat(result).isEmpty();
        }
    }

    // ==================== searchNotes Tests ====================

    @Nested
    @DisplayName("searchNotes")
    class SearchNotesTests {

        @Test
        @DisplayName("키워드로 노트 검색 - 성공")
        void searchNotes_Success() {
            // given
            Note note1 = createNote(1L, "프로젝트 회의록", "업무", "N");
            when(noteRepository.searchByKeyword("프로젝트"))
                    .thenReturn(Arrays.asList(note1));

            // when
            List<Note> result = noteService.searchNotes("프로젝트");

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTitle()).contains("프로젝트");
        }

        @Test
        @DisplayName("키워드로 노트 검색 - 결과 없음")
        void searchNotes_NoResults() {
            // given
            when(noteRepository.searchByKeyword("없는키워드"))
                    .thenReturn(Collections.emptyList());

            // when
            List<Note> result = noteService.searchNotes("없는키워드");

            // then
            assertThat(result).isEmpty();
        }
    }

    // ==================== getAllCategories Tests ====================

    @Nested
    @DisplayName("getAllCategories")
    class GetAllCategoriesTests {

        @Test
        @DisplayName("모든 카테고리 조회 - 성공")
        void getAllCategories_Success() {
            // given
            when(noteRepository.findAllCategories())
                    .thenReturn(Arrays.asList("업무", "개인", "학습"));

            // when
            List<String> result = noteService.getAllCategories();

            // then
            assertThat(result).hasSize(3);
            assertThat(result).containsExactly("업무", "개인", "학습");
        }

        @Test
        @DisplayName("모든 카테고리 조회 - 빈 목록")
        void getAllCategories_Empty() {
            // given
            when(noteRepository.findAllCategories())
                    .thenReturn(Collections.emptyList());

            // when
            List<String> result = noteService.getAllCategories();

            // then
            assertThat(result).isEmpty();
        }
    }

    // ==================== getNoteById Tests ====================

    @Nested
    @DisplayName("getNoteById")
    class GetNoteByIdTests {

        @Test
        @DisplayName("ID로 노트 조회 - 존재하는 경우")
        void getNoteById_Found() {
            // given
            Note note = createNote(1L, "노트 1", "업무", "N");
            when(noteRepository.findById(1L)).thenReturn(Optional.of(note));

            // when
            Optional<Note> result = noteService.getNoteById(1L);

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getTitle()).isEqualTo("노트 1");
        }

        @Test
        @DisplayName("ID로 노트 조회 - 존재하지 않는 경우")
        void getNoteById_NotFound() {
            // given
            when(noteRepository.findById(999L)).thenReturn(Optional.empty());

            // when
            Optional<Note> result = noteService.getNoteById(999L);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("ID로 노트 조회 - 삭제된 노트는 조회 불가")
        void getNoteById_DeletedNote() {
            // given
            Note deletedNote = createNote(1L, "삭제된 노트", "업무", "N");
            deletedNote.setDeleted("Y");
            when(noteRepository.findById(1L)).thenReturn(Optional.of(deletedNote));

            // when
            Optional<Note> result = noteService.getNoteById(1L);

            // then
            assertThat(result).isEmpty();
        }
    }

    // ==================== createNote Tests ====================

    @Nested
    @DisplayName("createNote")
    class CreateNoteTests {

        @Test
        @DisplayName("노트 생성 - 성공")
        void createNote_Success() {
            // given
            Note newNote = new Note();
            newNote.setTitle("새 노트");
            newNote.setContent("새 노트 내용");
            newNote.setCategory("업무");

            when(noteRepository.save(any(Note.class))).thenAnswer(inv -> {
                Note saved = inv.getArgument(0);
                saved.setId(1L);
                return saved;
            });

            // when
            Note result = noteService.createNote(newNote);

            // then
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getTitle()).isEqualTo("새 노트");
            verify(noteRepository).save(newNote);
        }
    }

    // ==================== updateNote Tests ====================

    @Nested
    @DisplayName("updateNote")
    class UpdateNoteTests {

        @Test
        @DisplayName("노트 수정 - 성공")
        void updateNote_Success() {
            // given
            Note existingNote = createNote(1L, "기존 노트", "업무", "N");

            Note updatedData = new Note();
            updatedData.setTitle("수정된 노트");
            updatedData.setContent("수정된 내용");
            updatedData.setCategory("개인");

            when(noteRepository.findById(1L)).thenReturn(Optional.of(existingNote));
            when(noteRepository.save(any(Note.class))).thenAnswer(inv -> inv.getArgument(0));

            // when
            Note result = noteService.updateNote(1L, updatedData);

            // then
            assertThat(result.getTitle()).isEqualTo("수정된 노트");
            assertThat(result.getContent()).isEqualTo("수정된 내용");
            assertThat(result.getCategory()).isEqualTo("개인");
        }

        @Test
        @DisplayName("노트 수정 - 존재하지 않는 ID")
        void updateNote_NotFound() {
            // given
            Note updatedData = new Note();
            updatedData.setTitle("수정된 노트");

            when(noteRepository.findById(999L)).thenReturn(Optional.empty());

            // when/then
            assertThatThrownBy(() -> noteService.updateNote(999L, updatedData))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Note not found with id: 999");
        }

        @Test
        @DisplayName("노트 수정 - 삭제된 노트는 수정 불가")
        void updateNote_DeletedNote() {
            // given
            Note deletedNote = createNote(1L, "삭제된 노트", "업무", "N");
            deletedNote.setDeleted("Y");

            Note updatedData = new Note();
            updatedData.setTitle("수정된 노트");

            when(noteRepository.findById(1L)).thenReturn(Optional.of(deletedNote));

            // when/then
            assertThatThrownBy(() -> noteService.updateNote(1L, updatedData))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Note not found with id: 1");
        }
    }

    // ==================== togglePin Tests ====================

    @Nested
    @DisplayName("togglePin")
    class TogglePinTests {

        @Test
        @DisplayName("고정 해제 → 고정으로 토글")
        void togglePin_ToPinned() {
            // given
            Note note = createNote(1L, "노트", "업무", "N");
            when(noteRepository.findById(1L)).thenReturn(Optional.of(note));
            when(noteRepository.save(any(Note.class))).thenAnswer(inv -> inv.getArgument(0));

            // when
            noteService.togglePin(1L);

            // then
            ArgumentCaptor<Note> noteCaptor = ArgumentCaptor.forClass(Note.class);
            verify(noteRepository).save(noteCaptor.capture());
            assertThat(noteCaptor.getValue().getPinned()).isEqualTo("Y");
        }

        @Test
        @DisplayName("고정 → 고정 해제로 토글")
        void togglePin_ToUnpinned() {
            // given
            Note note = createNote(1L, "노트", "업무", "Y");
            when(noteRepository.findById(1L)).thenReturn(Optional.of(note));
            when(noteRepository.save(any(Note.class))).thenAnswer(inv -> inv.getArgument(0));

            // when
            noteService.togglePin(1L);

            // then
            ArgumentCaptor<Note> noteCaptor = ArgumentCaptor.forClass(Note.class);
            verify(noteRepository).save(noteCaptor.capture());
            assertThat(noteCaptor.getValue().getPinned()).isEqualTo("N");
        }

        @Test
        @DisplayName("토글 - 존재하지 않는 ID")
        void togglePin_NotFound() {
            // given
            when(noteRepository.findById(999L)).thenReturn(Optional.empty());

            // when
            noteService.togglePin(999L);

            // then
            verify(noteRepository, never()).save(any());
        }

        @Test
        @DisplayName("토글 - 삭제된 노트는 토글 불가")
        void togglePin_DeletedNote() {
            // given
            Note deletedNote = createNote(1L, "삭제된 노트", "업무", "N");
            deletedNote.setDeleted("Y");
            when(noteRepository.findById(1L)).thenReturn(Optional.of(deletedNote));

            // when
            noteService.togglePin(1L);

            // then
            verify(noteRepository, never()).save(any());
        }
    }

    // ==================== deleteNote Tests ====================

    @Nested
    @DisplayName("deleteNote")
    class DeleteNoteTests {

        @Test
        @DisplayName("노트 삭제 (소프트 삭제) - 성공")
        void deleteNote_Success() {
            // given
            Note note = createNote(1L, "노트", "업무", "N");
            when(noteRepository.findById(1L)).thenReturn(Optional.of(note));
            when(noteRepository.save(any(Note.class))).thenAnswer(inv -> inv.getArgument(0));

            // when
            noteService.deleteNote(1L);

            // then
            ArgumentCaptor<Note> noteCaptor = ArgumentCaptor.forClass(Note.class);
            verify(noteRepository).save(noteCaptor.capture());
            assertThat(noteCaptor.getValue().getDeleted()).isEqualTo("Y");
        }

        @Test
        @DisplayName("노트 삭제 - 존재하지 않는 ID")
        void deleteNote_NotFound() {
            // given
            when(noteRepository.findById(999L)).thenReturn(Optional.empty());

            // when
            noteService.deleteNote(999L);

            // then
            verify(noteRepository, never()).save(any());
        }

        @Test
        @DisplayName("노트 삭제 - 이미 삭제된 노트")
        void deleteNote_AlreadyDeleted() {
            // given
            Note deletedNote = createNote(1L, "삭제된 노트", "업무", "N");
            deletedNote.setDeleted("Y");
            when(noteRepository.findById(1L)).thenReturn(Optional.of(deletedNote));

            // when
            noteService.deleteNote(1L);

            // then
            verify(noteRepository, never()).save(any());
        }
    }
}
