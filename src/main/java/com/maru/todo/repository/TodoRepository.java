package com.maru.todo.repository;

import com.maru.todo.entity.Todo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TodoRepository extends JpaRepository<Todo, Long> {

    List<Todo> findByCompletedAndDeletedOrderByCreatedAtDesc(String completed, String deleted);

    List<Todo> findByDeletedOrderByCreatedAtDesc(String deleted);

    List<Todo> findByCompletedAndDeletedAndCreatedAtBetweenOrderByCreatedAtDesc(
            String completed, String deleted, LocalDateTime from, LocalDateTime to);

    List<Todo> findByDeletedAndCreatedAtBetweenOrderByCreatedAtDesc(
            String deleted, LocalDateTime from, LocalDateTime to);
}
