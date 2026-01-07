package com.maru.kanban.repository;

import com.maru.kanban.entity.KanbanTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KanbanTaskRepository extends JpaRepository<KanbanTask, Long> {

    // Find all tasks by project (ordered by most recently updated first)
    List<KanbanTask> findByProjectIdAndDeletedOrderByUpdatedAtDesc(Long projectId, String deleted);

    // Find tasks by project and status (ordered by most recently updated first)
    List<KanbanTask> findByProjectIdAndStatusAndDeletedOrderByUpdatedAtDesc(
            Long projectId, String status, String deleted);

    // Find task by ID with active check
    Optional<KanbanTask> findByIdAndDeleted(Long id, String deleted);

    // Find tasks with file attachments
    @Query("SELECT t FROM KanbanTask t WHERE t.project.id = :projectId " +
           "AND t.deleted = :deleted AND t.fileStoredName IS NOT NULL " +
           "ORDER BY t.displayOrder ASC")
    List<KanbanTask> findTasksWithFilesByProject(
            @Param("projectId") Long projectId,
            @Param("deleted") String deleted);

    // Count tasks by status
    long countByProjectIdAndStatusAndDeleted(Long projectId, String status, String deleted);

    // Get max display order for new task insertion
    @Query("SELECT COALESCE(MAX(t.displayOrder), 0) FROM KanbanTask t " +
           "WHERE t.project.id = :projectId AND t.status = :status AND t.deleted = 'N'")
    Integer getMaxDisplayOrderByProjectAndStatus(
            @Param("projectId") Long projectId,
            @Param("status") String status);
}
