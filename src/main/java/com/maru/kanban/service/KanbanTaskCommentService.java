package com.maru.kanban.service;

import com.maru.kanban.entity.KanbanTask;
import com.maru.kanban.entity.KanbanTaskComment;
import com.maru.kanban.repository.KanbanTaskCommentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for managing Kanban task comments
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class KanbanTaskCommentService {

    private final KanbanTaskCommentRepository commentRepository;

    /**
     * Get all comments for a task
     */
    public List<KanbanTaskComment> getCommentsByTaskId(Long taskId) {
        return commentRepository.findByTaskIdAndDeletedOrderByCreatedAtDesc(taskId, "N");
    }

    /**
     * Create a new comment
     */
    @Transactional
    public KanbanTaskComment createComment(Long taskId, String commentText) {
        KanbanTaskComment comment = new KanbanTaskComment();

        KanbanTask task = new KanbanTask();
        task.setId(taskId);
        comment.setTask(task);

        comment.setCommentText(commentText);

        return commentRepository.save(comment);
    }

    /**
     * Delete a comment (soft delete)
     */
    @Transactional
    public void deleteComment(Long commentId) {
        commentRepository.findById(commentId).ifPresent(comment -> {
            comment.setDeleted("Y");
            commentRepository.save(comment);
        });
    }
}
