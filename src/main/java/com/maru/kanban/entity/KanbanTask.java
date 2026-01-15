package com.maru.kanban.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import java.time.LocalDateTime;

@Entity
@Table(name = "kanban_tasks")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KanbanTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private KanbanProject project;

    @Column(name = "task_number")
    private Integer taskNumber;

    @NotBlank(message = "Task title is required")
    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 20)
    private String status = "REGISTERED"; // REGISTERED, WAITING_RESPONSE, IN_PROGRESS, COMPLETED

    @Column(length = 10)
    private String priority = "MEDIUM"; // LOW, MEDIUM, HIGH

    @Column(name = "display_order")
    private Integer displayOrder = 0;

    // File fields
    @Column(name = "file_original_name")
    private String fileOriginalName;

    @Column(name = "file_stored_name")
    private String fileStoredName;

    @Column(name = "file_path", length = 1000)
    private String filePath;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "file_content_type", length = 100)
    private String fileContentType;

    // Automation fields
    @Column(name = "automation_notes", columnDefinition = "TEXT")
    private String automationNotes;

    @Column(name = "last_executed_at")
    private LocalDateTime lastExecutedAt;

    // Timestamps
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(nullable = false, length = 1)
    private String deleted = "N";

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        if ("COMPLETED".equals(status) && completedAt == null) {
            completedAt = LocalDateTime.now();
        }
    }

    // Helper method to check if file attached
    @Transient
    public boolean hasFile() {
        return fileStoredName != null && !fileStoredName.isEmpty();
    }

    // Helper method to check if file is an image
    @Transient
    public boolean isImageFile() {
        if (fileContentType == null) return false;
        return fileContentType.startsWith("image/");
    }
}
