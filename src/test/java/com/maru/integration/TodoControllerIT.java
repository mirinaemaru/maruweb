package com.maru.integration;

import com.maru.todo.TodoApplication;
import com.maru.todo.entity.Todo;
import com.maru.todo.repository.TodoRepository;
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
@DisplayName("TodoController 통합테스트")
class TodoControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TodoRepository todoRepository;

    @BeforeEach
    void setUp() {
        todoRepository.deleteAll();
    }

    @Test
    @DisplayName("Todo 목록 조회 - 성공")
    void listTodos_Success() throws Exception {
        // Given
        Todo todo = new Todo();
        todo.setTitle("테스트 할 일");
        todo.setCompleted("N");
        todo.setDeleted("N");
        todoRepository.save(todo);

        // When & Then
        mockMvc.perform(get("/todos"))
                .andExpect(status().isOk())
                .andExpect(view().name("todo/list"))
                .andExpect(model().attributeExists("todos"));
    }

    @Test
    @DisplayName("Todo 생성 - 성공")
    void createTodo_Success() throws Exception {
        // When
        mockMvc.perform(post("/todos")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("title", "새로운 할 일"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/todos"));

        // Then
        assertThat(todoRepository.count()).isEqualTo(1);
        Todo saved = todoRepository.findAll().get(0);
        assertThat(saved.getTitle()).isEqualTo("새로운 할 일");
        assertThat(saved.getCompleted()).isEqualTo("N");
    }

    @Test
    @DisplayName("Todo 생성 - 제목 없음 실패")
    void createTodo_NoTitle_Fails() throws Exception {
        // When
        mockMvc.perform(post("/todos")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("title", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/todos"))
                .andExpect(flash().attributeExists("error"));

        // Then
        assertThat(todoRepository.count()).isZero();
    }

    @Test
    @DisplayName("Todo 수정 폼 - 성공")
    void editTodoForm_Success() throws Exception {
        // Given
        Todo todo = new Todo();
        todo.setTitle("기존 할 일");
        todo.setCompleted("N");
        todo.setDeleted("N");
        Todo saved = todoRepository.save(todo);

        // When & Then
        mockMvc.perform(get("/todos/{id}/edit", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("todo/edit"))
                .andExpect(model().attributeExists("todo"));
    }

    @Test
    @DisplayName("Todo 수정 - 성공")
    void updateTodo_Success() throws Exception {
        // Given
        Todo todo = new Todo();
        todo.setTitle("기존 할 일");
        todo.setCompleted("N");
        todo.setDeleted("N");
        Todo saved = todoRepository.save(todo);

        // When
        mockMvc.perform(post("/todos/{id}", saved.getId())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("title", "수정된 할 일"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/todos"));

        // Then
        Optional<Todo> updated = todoRepository.findById(saved.getId());
        assertThat(updated).isPresent();
        assertThat(updated.get().getTitle()).isEqualTo("수정된 할 일");
    }

    @Test
    @DisplayName("Todo 완료 토글 - 성공")
    void toggleComplete_Success() throws Exception {
        // Given
        Todo todo = new Todo();
        todo.setTitle("완료할 할 일");
        todo.setCompleted("N");
        todo.setDeleted("N");
        Todo saved = todoRepository.save(todo);

        // When
        mockMvc.perform(post("/todos/{id}/toggle", saved.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/todos"));

        // Then
        Optional<Todo> toggled = todoRepository.findById(saved.getId());
        assertThat(toggled).isPresent();
        assertThat(toggled.get().getCompleted()).isEqualTo("Y");
    }

    @Test
    @DisplayName("Todo 삭제 - 성공 (소프트 삭제)")
    void deleteTodo_Success() throws Exception {
        // Given
        Todo todo = new Todo();
        todo.setTitle("삭제할 할 일");
        todo.setCompleted("N");
        todo.setDeleted("N");
        Todo saved = todoRepository.save(todo);

        // When
        mockMvc.perform(post("/todos/{id}/delete", saved.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/todos"))
                .andExpect(flash().attributeExists("success"));

        // Then - 소프트 삭제이므로 레코드는 존재
        Optional<Todo> deleted = todoRepository.findById(saved.getId());
        assertThat(deleted).isPresent();
        assertThat(deleted.get().getDeleted()).isEqualTo("Y");
    }

    @Test
    @DisplayName("Todo 설명 업데이트 - 성공")
    void updateDescription_Success() throws Exception {
        // Given
        Todo todo = new Todo();
        todo.setTitle("설명 추가할 할 일");
        todo.setCompleted("N");
        todo.setDeleted("N");
        Todo saved = todoRepository.save(todo);

        // When
        mockMvc.perform(post("/todos/{id}/description", saved.getId())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("description", "상세한 설명입니다."))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/todos"));

        // Then
        Optional<Todo> updated = todoRepository.findById(saved.getId());
        assertThat(updated).isPresent();
        assertThat(updated.get().getDescription()).isEqualTo("상세한 설명입니다.");
    }

    @Test
    @DisplayName("Todo 목록 필터링 - 완료된 항목")
    void listTodos_FilterCompleted() throws Exception {
        // Given
        Todo activeTodo = new Todo();
        activeTodo.setTitle("미완료 할 일");
        activeTodo.setCompleted("N");
        activeTodo.setDeleted("N");
        todoRepository.save(activeTodo);

        Todo completedTodo = new Todo();
        completedTodo.setTitle("완료된 할 일");
        completedTodo.setCompleted("Y");
        completedTodo.setDeleted("N");
        todoRepository.save(completedTodo);

        // When & Then
        mockMvc.perform(get("/todos")
                        .param("filter", "completed"))
                .andExpect(status().isOk())
                .andExpect(view().name("todo/list"))
                .andExpect(model().attribute("filter", "completed"));
    }
}
