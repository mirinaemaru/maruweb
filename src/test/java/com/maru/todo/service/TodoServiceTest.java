package com.maru.todo.service;

import com.maru.todo.entity.Todo;
import com.maru.todo.repository.TodoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TodoService 단위 테스트")
class TodoServiceTest {

    @Mock
    private TodoRepository todoRepository;

    private TodoService todoService;

    @BeforeEach
    void setUp() {
        todoService = new TodoService(todoRepository);
    }

    private Todo createTodo(Long id, String title, String completed) {
        Todo todo = new Todo();
        todo.setId(id);
        todo.setTitle(title);
        todo.setCompleted(completed);
        todo.setDeleted("N");
        todo.setCreatedAt(LocalDateTime.now());
        return todo;
    }

    // ==================== getAllTodos Tests ====================

    @Nested
    @DisplayName("getAllTodos")
    class GetAllTodosTests {

        @Test
        @DisplayName("모든 할일 조회 - 성공")
        void getAllTodos_Success() {
            // given
            Todo todo1 = createTodo(1L, "할일 1", "N");
            Todo todo2 = createTodo(2L, "할일 2", "Y");
            when(todoRepository.findByDeletedOrderByCreatedAtDesc("N"))
                    .thenReturn(Arrays.asList(todo1, todo2));

            // when
            List<Todo> result = todoService.getAllTodos();

            // then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getTitle()).isEqualTo("할일 1");
        }

        @Test
        @DisplayName("모든 할일 조회 - 빈 목록")
        void getAllTodos_Empty() {
            // given
            when(todoRepository.findByDeletedOrderByCreatedAtDesc("N"))
                    .thenReturn(Collections.emptyList());

            // when
            List<Todo> result = todoService.getAllTodos();

            // then
            assertThat(result).isEmpty();
        }
    }

    // ==================== getTodosByStatus Tests ====================

    @Nested
    @DisplayName("getTodosByStatus")
    class GetTodosByStatusTests {

        @Test
        @DisplayName("완료된 할일 조회")
        void getTodosByStatus_Completed() {
            // given
            Todo todo1 = createTodo(1L, "완료된 할일", "Y");
            when(todoRepository.findByCompletedAndDeletedOrderByCreatedAtDesc("Y", "N"))
                    .thenReturn(Arrays.asList(todo1));

            // when
            List<Todo> result = todoService.getTodosByStatus("Y");

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getCompleted()).isEqualTo("Y");
        }

        @Test
        @DisplayName("미완료 할일 조회")
        void getTodosByStatus_NotCompleted() {
            // given
            Todo todo1 = createTodo(1L, "미완료 할일", "N");
            when(todoRepository.findByCompletedAndDeletedOrderByCreatedAtDesc("N", "N"))
                    .thenReturn(Arrays.asList(todo1));

            // when
            List<Todo> result = todoService.getTodosByStatus("N");

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getCompleted()).isEqualTo("N");
        }
    }

    // ==================== getAllTodosWithDateRange Tests ====================

    @Nested
    @DisplayName("getAllTodosWithDateRange")
    class GetAllTodosWithDateRangeTests {

        @Test
        @DisplayName("날짜 범위로 할일 조회")
        void getAllTodosWithDateRange_Success() {
            // given
            LocalDate from = LocalDate.of(2024, 1, 1);
            LocalDate to = LocalDate.of(2024, 1, 31);
            LocalDateTime fromDateTime = from.atStartOfDay();
            LocalDateTime toDateTime = to.atTime(LocalTime.MAX);

            Todo todo1 = createTodo(1L, "1월 할일", "N");
            when(todoRepository.findByDeletedAndCreatedAtBetweenOrderByCreatedAtDesc(
                    eq("N"), eq(fromDateTime), eq(toDateTime)))
                    .thenReturn(Arrays.asList(todo1));

            // when
            List<Todo> result = todoService.getAllTodosWithDateRange(from, to);

            // then
            assertThat(result).hasSize(1);
            verify(todoRepository).findByDeletedAndCreatedAtBetweenOrderByCreatedAtDesc(
                    eq("N"), eq(fromDateTime), eq(toDateTime));
        }
    }

    // ==================== getTodosByStatusWithDateRange Tests ====================

    @Nested
    @DisplayName("getTodosByStatusWithDateRange")
    class GetTodosByStatusWithDateRangeTests {

        @Test
        @DisplayName("상태와 날짜 범위로 할일 조회")
        void getTodosByStatusWithDateRange_Success() {
            // given
            LocalDate from = LocalDate.of(2024, 1, 1);
            LocalDate to = LocalDate.of(2024, 1, 31);
            LocalDateTime fromDateTime = from.atStartOfDay();
            LocalDateTime toDateTime = to.atTime(LocalTime.MAX);

            Todo todo1 = createTodo(1L, "완료된 1월 할일", "Y");
            when(todoRepository.findByCompletedAndDeletedAndCreatedAtBetweenOrderByCreatedAtDesc(
                    eq("Y"), eq("N"), eq(fromDateTime), eq(toDateTime)))
                    .thenReturn(Arrays.asList(todo1));

            // when
            List<Todo> result = todoService.getTodosByStatusWithDateRange("Y", from, to);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getCompleted()).isEqualTo("Y");
        }
    }

    // ==================== searchTodos Tests ====================

    @Nested
    @DisplayName("searchTodos")
    class SearchTodosTests {

        @Test
        @DisplayName("키워드로 할일 검색")
        void searchTodos_WithKeyword() {
            // given
            LocalDate from = LocalDate.of(2024, 1, 1);
            LocalDate to = LocalDate.of(2024, 12, 31);
            LocalDateTime fromDateTime = from.atStartOfDay();
            LocalDateTime toDateTime = to.atTime(LocalTime.MAX);

            Todo todo1 = createTodo(1L, "프로젝트 회의", "N");
            when(todoRepository.searchByKeywordAndStatusAndDateRange(
                    eq("프로젝트"), eq("N"), eq("N"), eq(fromDateTime), eq(toDateTime)))
                    .thenReturn(Arrays.asList(todo1));

            // when
            List<Todo> result = todoService.searchTodos("N", "프로젝트", from, to);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTitle()).contains("프로젝트");
        }

        @Test
        @DisplayName("빈 키워드로 검색 시 전체 조회")
        void searchTodos_EmptyKeyword() {
            // given
            LocalDate from = LocalDate.of(2024, 1, 1);
            LocalDate to = LocalDate.of(2024, 12, 31);
            LocalDateTime fromDateTime = from.atStartOfDay();
            LocalDateTime toDateTime = to.atTime(LocalTime.MAX);

            Todo todo1 = createTodo(1L, "할일 1", "N");
            when(todoRepository.findByCompletedAndDeletedAndCreatedAtBetweenOrderByCreatedAtDesc(
                    eq("N"), eq("N"), eq(fromDateTime), eq(toDateTime)))
                    .thenReturn(Arrays.asList(todo1));

            // when
            List<Todo> result = todoService.searchTodos("N", "", from, to);

            // then
            assertThat(result).hasSize(1);
            verify(todoRepository, never()).searchByKeywordAndStatusAndDateRange(any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("null 키워드로 검색 시 전체 조회")
        void searchTodos_NullKeyword() {
            // given
            LocalDate from = LocalDate.of(2024, 1, 1);
            LocalDate to = LocalDate.of(2024, 12, 31);
            LocalDateTime fromDateTime = from.atStartOfDay();
            LocalDateTime toDateTime = to.atTime(LocalTime.MAX);

            Todo todo1 = createTodo(1L, "할일 1", "N");
            when(todoRepository.findByCompletedAndDeletedAndCreatedAtBetweenOrderByCreatedAtDesc(
                    eq("N"), eq("N"), eq(fromDateTime), eq(toDateTime)))
                    .thenReturn(Arrays.asList(todo1));

            // when
            List<Todo> result = todoService.searchTodos("N", null, from, to);

            // then
            assertThat(result).hasSize(1);
        }
    }

    // ==================== searchAllTodos Tests ====================

    @Nested
    @DisplayName("searchAllTodos")
    class SearchAllTodosTests {

        @Test
        @DisplayName("전체 상태에서 키워드 검색")
        void searchAllTodos_WithKeyword() {
            // given
            LocalDate from = LocalDate.of(2024, 1, 1);
            LocalDate to = LocalDate.of(2024, 12, 31);
            LocalDateTime fromDateTime = from.atStartOfDay();
            LocalDateTime toDateTime = to.atTime(LocalTime.MAX);

            Todo todo1 = createTodo(1L, "회의 준비", "N");
            when(todoRepository.searchByKeywordAndDateRange(
                    eq("회의"), eq("N"), eq(fromDateTime), eq(toDateTime)))
                    .thenReturn(Arrays.asList(todo1));

            // when
            List<Todo> result = todoService.searchAllTodos("회의", from, to);

            // then
            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("빈 키워드로 전체 검색")
        void searchAllTodos_EmptyKeyword() {
            // given
            LocalDate from = LocalDate.of(2024, 1, 1);
            LocalDate to = LocalDate.of(2024, 12, 31);
            LocalDateTime fromDateTime = from.atStartOfDay();
            LocalDateTime toDateTime = to.atTime(LocalTime.MAX);

            Todo todo1 = createTodo(1L, "할일 1", "N");
            when(todoRepository.findByDeletedAndCreatedAtBetweenOrderByCreatedAtDesc(
                    eq("N"), eq(fromDateTime), eq(toDateTime)))
                    .thenReturn(Arrays.asList(todo1));

            // when
            List<Todo> result = todoService.searchAllTodos("", from, to);

            // then
            assertThat(result).hasSize(1);
            verify(todoRepository, never()).searchByKeywordAndDateRange(any(), any(), any(), any());
        }
    }

    // ==================== getTodoById Tests ====================

    @Nested
    @DisplayName("getTodoById")
    class GetTodoByIdTests {

        @Test
        @DisplayName("ID로 할일 조회 - 존재하는 경우")
        void getTodoById_Found() {
            // given
            Todo todo = createTodo(1L, "할일 1", "N");
            when(todoRepository.findById(1L)).thenReturn(Optional.of(todo));

            // when
            Optional<Todo> result = todoService.getTodoById(1L);

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getTitle()).isEqualTo("할일 1");
        }

        @Test
        @DisplayName("ID로 할일 조회 - 존재하지 않는 경우")
        void getTodoById_NotFound() {
            // given
            when(todoRepository.findById(999L)).thenReturn(Optional.empty());

            // when
            Optional<Todo> result = todoService.getTodoById(999L);

            // then
            assertThat(result).isEmpty();
        }
    }

    // ==================== createTodo Tests ====================

    @Nested
    @DisplayName("createTodo")
    class CreateTodoTests {

        @Test
        @DisplayName("할일 생성 - 성공")
        void createTodo_Success() {
            // given
            Todo newTodo = new Todo();
            newTodo.setTitle("새 할일");
            newTodo.setDescription("설명");

            when(todoRepository.save(any(Todo.class))).thenAnswer(inv -> {
                Todo saved = inv.getArgument(0);
                saved.setId(1L);
                return saved;
            });

            // when
            Todo result = todoService.createTodo(newTodo);

            // then
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getTitle()).isEqualTo("새 할일");
            verify(todoRepository).save(newTodo);
        }
    }

    // ==================== updateTodo Tests ====================

    @Nested
    @DisplayName("updateTodo")
    class UpdateTodoTests {

        @Test
        @DisplayName("할일 수정 - 성공")
        void updateTodo_Success() {
            // given
            Todo existingTodo = createTodo(1L, "기존 할일", "N");
            existingTodo.setDescription("기존 설명");

            Todo updatedData = new Todo();
            updatedData.setTitle("수정된 할일");
            updatedData.setDescription("수정된 설명");
            updatedData.setCompleted("Y");

            when(todoRepository.findById(1L)).thenReturn(Optional.of(existingTodo));
            when(todoRepository.save(any(Todo.class))).thenAnswer(inv -> inv.getArgument(0));

            // when
            Todo result = todoService.updateTodo(1L, updatedData);

            // then
            assertThat(result.getTitle()).isEqualTo("수정된 할일");
            assertThat(result.getDescription()).isEqualTo("수정된 설명");
            assertThat(result.getCompleted()).isEqualTo("Y");
        }

        @Test
        @DisplayName("할일 수정 - 존재하지 않는 ID")
        void updateTodo_NotFound() {
            // given
            Todo updatedData = new Todo();
            updatedData.setTitle("수정된 할일");

            when(todoRepository.findById(999L)).thenReturn(Optional.empty());

            // when/then
            assertThatThrownBy(() -> todoService.updateTodo(999L, updatedData))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Todo not found with id: 999");
        }
    }

    // ==================== toggleComplete Tests ====================

    @Nested
    @DisplayName("toggleComplete")
    class ToggleCompleteTests {

        @Test
        @DisplayName("미완료 → 완료로 토글")
        void toggleComplete_ToCompleted() {
            // given
            Todo todo = createTodo(1L, "할일", "N");
            when(todoRepository.findById(1L)).thenReturn(Optional.of(todo));
            when(todoRepository.save(any(Todo.class))).thenAnswer(inv -> inv.getArgument(0));

            // when
            todoService.toggleComplete(1L);

            // then
            ArgumentCaptor<Todo> todoCaptor = ArgumentCaptor.forClass(Todo.class);
            verify(todoRepository).save(todoCaptor.capture());
            assertThat(todoCaptor.getValue().getCompleted()).isEqualTo("Y");
        }

        @Test
        @DisplayName("완료 → 미완료로 토글")
        void toggleComplete_ToNotCompleted() {
            // given
            Todo todo = createTodo(1L, "할일", "Y");
            when(todoRepository.findById(1L)).thenReturn(Optional.of(todo));
            when(todoRepository.save(any(Todo.class))).thenAnswer(inv -> inv.getArgument(0));

            // when
            todoService.toggleComplete(1L);

            // then
            ArgumentCaptor<Todo> todoCaptor = ArgumentCaptor.forClass(Todo.class);
            verify(todoRepository).save(todoCaptor.capture());
            assertThat(todoCaptor.getValue().getCompleted()).isEqualTo("N");
        }

        @Test
        @DisplayName("토글 - 존재하지 않는 ID")
        void toggleComplete_NotFound() {
            // given
            when(todoRepository.findById(999L)).thenReturn(Optional.empty());

            // when
            todoService.toggleComplete(999L);

            // then
            verify(todoRepository, never()).save(any());
        }
    }

    // ==================== updateDescription Tests ====================

    @Nested
    @DisplayName("updateDescription")
    class UpdateDescriptionTests {

        @Test
        @DisplayName("설명 수정 - 성공")
        void updateDescription_Success() {
            // given
            Todo todo = createTodo(1L, "할일", "N");
            todo.setDescription("기존 설명");
            when(todoRepository.findById(1L)).thenReturn(Optional.of(todo));
            when(todoRepository.save(any(Todo.class))).thenAnswer(inv -> inv.getArgument(0));

            // when
            todoService.updateDescription(1L, "새로운 설명");

            // then
            ArgumentCaptor<Todo> todoCaptor = ArgumentCaptor.forClass(Todo.class);
            verify(todoRepository).save(todoCaptor.capture());
            assertThat(todoCaptor.getValue().getDescription()).isEqualTo("새로운 설명");
        }

        @Test
        @DisplayName("설명 수정 - 존재하지 않는 ID")
        void updateDescription_NotFound() {
            // given
            when(todoRepository.findById(999L)).thenReturn(Optional.empty());

            // when
            todoService.updateDescription(999L, "새로운 설명");

            // then
            verify(todoRepository, never()).save(any());
        }
    }

    // ==================== deleteTodo Tests ====================

    @Nested
    @DisplayName("deleteTodo")
    class DeleteTodoTests {

        @Test
        @DisplayName("할일 삭제 (소프트 삭제) - 성공")
        void deleteTodo_Success() {
            // given
            Todo todo = createTodo(1L, "할일", "N");
            when(todoRepository.findById(1L)).thenReturn(Optional.of(todo));
            when(todoRepository.save(any(Todo.class))).thenAnswer(inv -> inv.getArgument(0));

            // when
            todoService.deleteTodo(1L);

            // then
            ArgumentCaptor<Todo> todoCaptor = ArgumentCaptor.forClass(Todo.class);
            verify(todoRepository).save(todoCaptor.capture());
            assertThat(todoCaptor.getValue().getDeleted()).isEqualTo("Y");
        }

        @Test
        @DisplayName("할일 삭제 - 존재하지 않는 ID")
        void deleteTodo_NotFound() {
            // given
            when(todoRepository.findById(999L)).thenReturn(Optional.empty());

            // when
            todoService.deleteTodo(999L);

            // then
            verify(todoRepository, never()).save(any());
        }
    }
}
