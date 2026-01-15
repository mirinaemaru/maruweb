package com.maru.kanban.service;

import com.maru.kanban.entity.KanbanProject;
import com.maru.kanban.entity.KanbanTask;
import com.maru.kanban.repository.KanbanProjectRepository;
import com.maru.kanban.repository.KanbanTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing Kanban tasks
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class KanbanTaskService {

    private final KanbanTaskRepository taskRepository;
    private final KanbanProjectRepository projectRepository;
    private final FileStorageService fileStorageService;

    /**
     * Find all tasks for a project ordered by display order
     */
    public List<KanbanTask> findAllTasksByProject(Long projectId) {
        return taskRepository.findByProjectIdAndDeletedOrderByUpdatedAtDesc(projectId, "N");
    }

    /**
     * Find tasks by project and status (ordered by most recently updated first)
     */
    public List<KanbanTask> findTasksByProjectAndStatus(Long projectId, String status) {
        return taskRepository.findByProjectIdAndStatusAndDeletedOrderByUpdatedAtDesc(projectId, status, "N");
    }

    /**
     * Find task by ID
     */
    public Optional<KanbanTask> findTaskById(Long id) {
        return taskRepository.findByIdAndDeleted(id, "N");
    }

    /**
     * Find tasks with file attachments for a project
     */
    public List<KanbanTask> findTasksWithFiles(Long projectId) {
        return taskRepository.findTasksWithFilesByProject(projectId, "N");
    }

    /**
     * Count tasks by status
     */
    public long countTasksByStatus(Long projectId, String status) {
        return taskRepository.countByProjectIdAndStatusAndDeleted(projectId, status, "N");
    }

    /**
     * Create new task
     *
     * @param task Task to create
     * @param file Optional file attachment
     * @return Created task
     */
    @Transactional
    public KanbanTask createTask(KanbanTask task, MultipartFile file) {
        // Validate project exists
        KanbanProject project = projectRepository.findByIdAndDeleted(task.getProject().getId(), "N")
                .orElseThrow(() -> new RuntimeException("Project not found: " + task.getProject().getId()));

        task.setProject(project);

        // Set default status if not specified
        if (task.getStatus() == null || task.getStatus().isEmpty()) {
            task.setStatus("REGISTERED");
        }

        // Set display order if not specified
        if (task.getDisplayOrder() == null) {
            Integer maxOrder = taskRepository.getMaxDisplayOrderByProjectAndStatus(
                    task.getProject().getId(), task.getStatus());
            task.setDisplayOrder(maxOrder != null ? maxOrder + 1 : 0);
        }

        // Set project-specific task number
        Integer maxTaskNumber = taskRepository.getMaxTaskNumberByProject(task.getProject().getId());
        task.setTaskNumber(maxTaskNumber + 1);

        // Save task first to get ID for file storage
        KanbanTask saved = taskRepository.save(task);

        // Handle file upload if provided
        if (file != null && !file.isEmpty()) {
            handleFileUpload(saved, file);
            saved = taskRepository.save(saved);
        }

        log.info("Created task: id={}, projectId={}, title={}, status={}",
                saved.getId(), saved.getProject().getId(), saved.getTitle(), saved.getStatus());
        return saved;
    }

    /**
     * Update existing task
     *
     * @param id Task ID
     * @param updatedTask Updated task data
     * @param file Optional new file attachment
     * @return Updated task
     */
    @Transactional
    public KanbanTask updateTask(Long id, KanbanTask updatedTask, MultipartFile file) {
        KanbanTask task = taskRepository.findByIdAndDeleted(id, "N")
                .orElseThrow(() -> new RuntimeException("Task not found: " + id));

        // Update fields
        task.setTitle(updatedTask.getTitle());
        task.setDescription(updatedTask.getDescription());
        task.setPriority(updatedTask.getPriority());
        task.setAutomationNotes(updatedTask.getAutomationNotes());

        // Handle new file upload
        if (file != null && !file.isEmpty()) {
            // Delete old file if exists
            if (task.hasFile()) {
                deleteTaskFileInternal(task);
            }
            handleFileUpload(task, file);
        }

        KanbanTask saved = taskRepository.save(task);
        log.info("Updated task: id={}, title={}", saved.getId(), saved.getTitle());
        return saved;
    }

    /**
     * Update task status (for moving between columns)
     *
     * @param id Task ID
     * @param newStatus New status
     * @return Updated task
     */
    @Transactional
    public KanbanTask updateTaskStatus(Long id, String newStatus) {
        KanbanTask task = taskRepository.findByIdAndDeleted(id, "N")
                .orElseThrow(() -> new RuntimeException("Task not found: " + id));

        String oldStatus = task.getStatus();
        task.setStatus(newStatus);

        // Update display order to be last in new status column
        Integer maxOrder = taskRepository.getMaxDisplayOrderByProjectAndStatus(
                task.getProject().getId(), newStatus);
        task.setDisplayOrder(maxOrder != null ? maxOrder + 1 : 0);

        KanbanTask saved = taskRepository.save(task);
        log.info("Updated task status: id={}, oldStatus={}, newStatus={}", id, oldStatus, newStatus);
        return saved;
    }

    /**
     * Move task to new status and position
     *
     * @param taskId Task ID
     * @param newStatus New status
     * @param newDisplayOrder New display order
     * @return Updated task
     */
    @Transactional
    public KanbanTask moveTask(Long taskId, String newStatus, Integer newDisplayOrder) {
        KanbanTask task = taskRepository.findByIdAndDeleted(taskId, "N")
                .orElseThrow(() -> new RuntimeException("Task not found: " + taskId));

        task.setStatus(newStatus);
        task.setDisplayOrder(newDisplayOrder);

        KanbanTask saved = taskRepository.save(task);
        log.info("Moved task: id={}, newStatus={}, newOrder={}", taskId, newStatus, newDisplayOrder);
        return saved;
    }

    /**
     * Soft delete task
     *
     * @param id Task ID
     */
    @Transactional
    public void deleteTask(Long id) {
        KanbanTask task = taskRepository.findByIdAndDeleted(id, "N")
                .orElseThrow(() -> new RuntimeException("Task not found: " + id));

        // Delete file if attached
        if (task.hasFile()) {
            deleteTaskFileInternal(task);
        }

        task.setDeleted("Y");
        taskRepository.save(task);
        log.info("Deleted task: id={}, title={}", task.getId(), task.getTitle());
    }

    /**
     * Delete task file attachment
     *
     * @param id Task ID
     */
    @Transactional
    public void deleteTaskFile(Long id) {
        KanbanTask task = taskRepository.findByIdAndDeleted(id, "N")
                .orElseThrow(() -> new RuntimeException("Task not found: " + id));

        if (!task.hasFile()) {
            log.warn("No file to delete for task: id={}", id);
            return;
        }

        deleteTaskFileInternal(task);
        taskRepository.save(task);
        log.info("Deleted file for task: id={}", id);
    }

    /**
     * Handle file upload for task
     */
    private void handleFileUpload(KanbanTask task, MultipartFile file) {
        try {
            // Validate file
            fileStorageService.validateFile(file);

            // Store file
            String storedFilename = fileStorageService.storeFile(task.getId(), file);
            String filePath = fileStorageService.getFilePath(task.getId(), storedFilename);

            // Update task file metadata
            task.setFileOriginalName(file.getOriginalFilename());
            task.setFileStoredName(storedFilename);
            task.setFilePath(filePath);
            task.setFileSize(file.getSize());
            task.setFileContentType(file.getContentType());

            log.info("File uploaded for task: taskId={}, filename={}", task.getId(), storedFilename);
        } catch (Exception e) {
            log.error("Failed to upload file for task: taskId={}", task.getId(), e);
            throw new RuntimeException("Failed to upload file: " + e.getMessage(), e);
        }
    }

    /**
     * Delete task file and clear metadata (internal method)
     */
    private void deleteTaskFileInternal(KanbanTask task) {
        try {
            fileStorageService.deleteFile(task.getId(), task.getFileStoredName());
            task.setFileOriginalName(null);
            task.setFileStoredName(null);
            task.setFilePath(null);
            task.setFileSize(null);
            task.setFileContentType(null);
        } catch (Exception e) {
            log.error("Failed to delete file for task: taskId={}", task.getId(), e);
            // Don't throw exception to allow task deletion to continue
        }
    }

    /**
     * Update display order for task
     *
     * @param taskId Task ID
     * @param newOrder New display order
     */
    @Transactional
    public void updateDisplayOrder(Long taskId, Integer newOrder) {
        KanbanTask task = taskRepository.findByIdAndDeleted(taskId, "N")
                .orElseThrow(() -> new RuntimeException("Task not found: " + taskId));

        task.setDisplayOrder(newOrder);
        taskRepository.save(task);
        log.info("Updated task display order: id={}, newOrder={}", taskId, newOrder);
    }

    /**
     * Update task title and description (inline edit)
     *
     * @param taskId Task ID
     * @param title New title (optional, null to keep unchanged)
     * @param description New description (optional, null to keep unchanged)
     * @return Updated task
     */
    @Transactional
    public KanbanTask updateTaskContent(Long taskId, String title, String description) {
        KanbanTask task = taskRepository.findByIdAndDeleted(taskId, "N")
                .orElseThrow(() -> new RuntimeException("Task not found: " + taskId));

        if (title != null && !title.trim().isEmpty()) {
            task.setTitle(title.trim());
        }
        if (description != null) {
            task.setDescription(description.trim().isEmpty() ? null : description.trim());
        }

        KanbanTask saved = taskRepository.save(task);
        log.info("Updated task content: id={}, title={}", taskId, saved.getTitle());
        return saved;
    }
}
