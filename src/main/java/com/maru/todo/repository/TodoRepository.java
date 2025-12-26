package com.maru.todo.repository;

import com.maru.todo.entity.Todo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Query("SELECT t FROM Todo t WHERE t.deleted = :deleted " +
           "AND t.createdAt BETWEEN :from AND :to " +
           "AND (LOWER(t.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(t.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY t.createdAt DESC")
    List<Todo> searchByKeywordAndDateRange(
            @Param("keyword") String keyword,
            @Param("deleted") String deleted,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query("SELECT t FROM Todo t WHERE t.completed = :completed AND t.deleted = :deleted " +
           "AND t.createdAt BETWEEN :from AND :to " +
           "AND (LOWER(t.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(t.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY t.createdAt DESC")
    List<Todo> searchByKeywordAndStatusAndDateRange(
            @Param("keyword") String keyword,
            @Param("completed") String completed,
            @Param("deleted") String deleted,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);
}
