package com.maru.kanban.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "kanban_projects")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KanbanProject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Project name is required")
    @Column(nullable = false)
    private String name;

    @NotBlank(message = "Directory path is required")
    @Column(name = "directory_path", nullable = false, length = 500)
    private String directoryPath;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 7)
    private String color = "#667eea";

    @Column(name = "display_order")
    private Integer displayOrder = 0;

    @Column(nullable = false, length = 1)
    private String deleted = "N";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<KanbanTask> tasks = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Helper method to get active tasks count
    @Transient
    public long getActiveTasksCount() {
        return tasks.stream()
                .filter(t -> "N".equals(t.getDeleted()))
                .filter(t -> !"COMPLETED".equals(t.getStatus()))
                .count();
    }
}
