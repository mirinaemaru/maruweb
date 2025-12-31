package com.maru.strategy.repository;

import com.maru.strategy.entity.Strategy;
import com.maru.strategy.entity.StrategyCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface StrategyRepository extends JpaRepository<Strategy, Long> {

    // Basic queries
    List<Strategy> findByDeletedOrderByCreatedAtDesc(String deleted);

    Optional<Strategy> findByIdAndDeleted(Long id, String deleted);

    // Filter by status
    List<Strategy> findByStatusAndDeletedOrderByCreatedAtDesc(String status, String deleted);

    // Filter by priority
    List<Strategy> findByPriorityAndDeletedOrderByCreatedAtDesc(String priority, String deleted);

    // Filter by category
    List<Strategy> findByCategoryAndDeletedOrderByCreatedAtDesc(StrategyCategory category, String deleted);

    // Filter by status AND category
    List<Strategy> findByStatusAndCategoryAndDeletedOrderByCreatedAtDesc(
            String status, StrategyCategory category, String deleted);

    // Filter by priority AND category
    List<Strategy> findByPriorityAndCategoryAndDeletedOrderByCreatedAtDesc(
            String priority, StrategyCategory category, String deleted);

    // Complex search with keyword (title + description)
    @Query("SELECT s FROM Strategy s WHERE s.deleted = :deleted " +
            "AND (:status IS NULL OR s.status = :status) " +
            "AND (:priority IS NULL OR s.priority = :priority) " +
            "AND (:categoryId IS NULL OR s.category.id = :categoryId) " +
            "AND (:keyword IS NULL OR :keyword = '' OR LOWER(s.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "     OR LOWER(s.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "ORDER BY s.createdAt DESC")
    List<Strategy> searchStrategies(
            @Param("status") String status,
            @Param("priority") String priority,
            @Param("categoryId") Long categoryId,
            @Param("keyword") String keyword,
            @Param("deleted") String deleted);

    // Date range queries for startDate
    @Query("SELECT s FROM Strategy s WHERE s.deleted = :deleted " +
            "AND s.startDate BETWEEN :from AND :to " +
            "ORDER BY s.startDate DESC")
    List<Strategy> findByStartDateRange(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("deleted") String deleted);

    // Date range queries for targetDate
    @Query("SELECT s FROM Strategy s WHERE s.deleted = :deleted " +
            "AND s.targetDate BETWEEN :from AND :to " +
            "ORDER BY s.targetDate DESC")
    List<Strategy> findByTargetDateRange(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("deleted") String deleted);
}
