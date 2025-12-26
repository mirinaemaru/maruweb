package com.maru.todo.service;

import com.maru.todo.entity.Todo;
import com.maru.todo.repository.TodoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TodoService {

    private final TodoRepository todoRepository;

    public List<Todo> getAllTodos() {
        return todoRepository.findByDeletedOrderByCreatedAtDesc("N");
    }

    public List<Todo> getTodosByStatus(String completed) {
        return todoRepository.findByCompletedAndDeletedOrderByCreatedAtDesc(completed, "N");
    }

    public List<Todo> getAllTodosWithDateRange(LocalDate from, LocalDate to) {
        LocalDateTime fromDateTime = from.atStartOfDay();
        LocalDateTime toDateTime = to.atTime(LocalTime.MAX);
        return todoRepository.findByDeletedAndCreatedAtBetweenOrderByCreatedAtDesc("N", fromDateTime, toDateTime);
    }

    public List<Todo> getTodosByStatusWithDateRange(String completed, LocalDate from, LocalDate to) {
        LocalDateTime fromDateTime = from.atStartOfDay();
        LocalDateTime toDateTime = to.atTime(LocalTime.MAX);
        return todoRepository.findByCompletedAndDeletedAndCreatedAtBetweenOrderByCreatedAtDesc(
                completed, "N", fromDateTime, toDateTime);
    }

    public List<Todo> searchTodos(String completed, String keyword, LocalDate from, LocalDate to) {
        LocalDateTime fromDateTime = from.atStartOfDay();
        LocalDateTime toDateTime = to.atTime(LocalTime.MAX);
        if (keyword != null && !keyword.trim().isEmpty()) {
            return todoRepository.searchByKeywordAndStatusAndDateRange(
                    keyword.trim(), completed, "N", fromDateTime, toDateTime);
        }
        return todoRepository.findByCompletedAndDeletedAndCreatedAtBetweenOrderByCreatedAtDesc(
                completed, "N", fromDateTime, toDateTime);
    }

    public List<Todo> searchAllTodos(String keyword, LocalDate from, LocalDate to) {
        LocalDateTime fromDateTime = from.atStartOfDay();
        LocalDateTime toDateTime = to.atTime(LocalTime.MAX);
        if (keyword != null && !keyword.trim().isEmpty()) {
            return todoRepository.searchByKeywordAndDateRange(keyword.trim(), "N", fromDateTime, toDateTime);
        }
        return todoRepository.findByDeletedAndCreatedAtBetweenOrderByCreatedAtDesc("N", fromDateTime, toDateTime);
    }

    public Optional<Todo> getTodoById(Long id) {
        return todoRepository.findById(id);
    }

    @Transactional
    public Todo createTodo(Todo todo) {
        return todoRepository.save(todo);
    }

    @Transactional
    public Todo updateTodo(Long id, Todo updatedTodo) {
        return todoRepository.findById(id)
                .map(todo -> {
                    todo.setTitle(updatedTodo.getTitle());
                    todo.setDescription(updatedTodo.getDescription());
                    todo.setCompleted(updatedTodo.getCompleted());
                    return todoRepository.save(todo);
                })
                .orElseThrow(() -> new IllegalArgumentException("Todo not found with id: " + id));
    }

    @Transactional
    public void toggleComplete(Long id) {
        todoRepository.findById(id)
                .ifPresent(todo -> {
                    todo.setCompleted("Y".equals(todo.getCompleted()) ? "N" : "Y");
                    todoRepository.save(todo);
                });
    }

    @Transactional
    public void updateDescription(Long id, String description) {
        todoRepository.findById(id)
                .ifPresent(todo -> {
                    todo.setDescription(description);
                    todoRepository.save(todo);
                });
    }

    @Transactional
    public void deleteTodo(Long id) {
        todoRepository.findById(id)
                .ifPresent(todo -> {
                    todo.setDeleted("Y");
                    todoRepository.save(todo);
                });
    }
}
