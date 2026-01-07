package com.maru.kanban.repository;

import com.maru.kanban.entity.KanbanTaskComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for KanbanTaskComment entity
 */
@Repository
public interface KanbanTaskCommentRepository extends JpaRepository<KanbanTaskComment, Long> {

    /**
     * Find all comments for a task (excluding soft-deleted)
     */
    List<KanbanTaskComment> findByTaskIdAndDeletedOrderByCreatedAtDesc(Long taskId, String deleted);
}
