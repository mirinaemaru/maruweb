package com.maru.kanban.service;

import com.maru.kanban.entity.KanbanProject;
import com.maru.kanban.repository.KanbanProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing Kanban projects
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class KanbanProjectService {

    private final KanbanProjectRepository projectRepository;

    /**
     * Find all active projects ordered by display order
     */
    public List<KanbanProject> findAllProjects() {
        return projectRepository.findByDeletedOrderByDisplayOrderAsc("N");
    }

    /**
     * Find all active projects with eager-loaded tasks
     */
    public List<KanbanProject> findAllProjectsWithTasks() {
        return projectRepository.findAllWithTasks("N");
    }

    /**
     * Find project by ID
     */
    public Optional<KanbanProject> findProjectById(Long id) {
        return projectRepository.findByIdAndDeleted(id, "N");
    }

    /**
     * Create new project
     *
     * @param project Project to create
     * @return Created project
     */
    @Transactional
    public KanbanProject createProject(KanbanProject project) {
        // Validate directory path
        validateDirectoryPath(project.getDirectoryPath());

        // Check for duplicate directory path
        Optional<KanbanProject> existing = projectRepository
                .findByDirectoryPathAndDeleted(project.getDirectoryPath(), "N");
        if (existing.isPresent()) {
            throw new RuntimeException("Project with directory path already exists: " + project.getDirectoryPath());
        }

        // Set display order if not specified
        if (project.getDisplayOrder() == null) {
            List<KanbanProject> allProjects = projectRepository.findByDeletedOrderByDisplayOrderAsc("N");
            project.setDisplayOrder(allProjects.size());
        }

        // Set default color if not specified
        if (project.getColor() == null || project.getColor().isEmpty()) {
            project.setColor("#667eea");
        }

        KanbanProject saved = projectRepository.save(project);
        log.info("Created project: id={}, name={}, path={}", saved.getId(), saved.getName(), saved.getDirectoryPath());
        return saved;
    }

    /**
     * Update existing project
     *
     * @param id Project ID
     * @param updatedProject Updated project data
     * @return Updated project
     */
    @Transactional
    public KanbanProject updateProject(Long id, KanbanProject updatedProject) {
        KanbanProject project = projectRepository.findByIdAndDeleted(id, "N")
                .orElseThrow(() -> new RuntimeException("Project not found: " + id));

        // Validate directory path if changed
        if (!project.getDirectoryPath().equals(updatedProject.getDirectoryPath())) {
            validateDirectoryPath(updatedProject.getDirectoryPath());

            // Check for duplicate directory path
            Optional<KanbanProject> existing = projectRepository
                    .findByDirectoryPathAndDeleted(updatedProject.getDirectoryPath(), "N");
            if (existing.isPresent() && !existing.get().getId().equals(id)) {
                throw new RuntimeException("Project with directory path already exists: " + updatedProject.getDirectoryPath());
            }
        }

        // Update fields
        project.setName(updatedProject.getName());
        project.setDirectoryPath(updatedProject.getDirectoryPath());
        project.setDescription(updatedProject.getDescription());
        project.setColor(updatedProject.getColor());
        project.setDisplayOrder(updatedProject.getDisplayOrder());

        KanbanProject saved = projectRepository.save(project);
        log.info("Updated project: id={}, name={}", saved.getId(), saved.getName());
        return saved;
    }

    /**
     * Soft delete project
     *
     * @param id Project ID
     */
    @Transactional
    public void deleteProject(Long id) {
        KanbanProject project = projectRepository.findByIdAndDeleted(id, "N")
                .orElseThrow(() -> new RuntimeException("Project not found: " + id));

        project.setDeleted("Y");
        projectRepository.save(project);
        log.info("Deleted project: id={}, name={}", project.getId(), project.getName());
    }

    /**
     * Validate directory path
     * - Checks if path is not null/empty
     * - Warns if directory doesn't exist (doesn't throw exception for flexibility)
     *
     * @param directoryPath Path to validate
     */
    public void validateDirectoryPath(String directoryPath) {
        if (directoryPath == null || directoryPath.trim().isEmpty()) {
            throw new RuntimeException("Directory path cannot be empty");
        }

        // Expand ~ to user home directory
        String expandedPath = directoryPath.startsWith("~")
                ? System.getProperty("user.home") + directoryPath.substring(1)
                : directoryPath;

        Path path = Paths.get(expandedPath);

        // Log warning if directory doesn't exist (don't throw exception to allow flexibility)
        if (!Files.exists(path)) {
            log.warn("Directory path does not exist (will be created on first use): {}", expandedPath);
        } else if (!Files.isDirectory(path)) {
            throw new RuntimeException("Path is not a directory: " + expandedPath);
        }
    }

    /**
     * Check if project name already exists (for duplicate prevention in UI)
     *
     * @param name Project name
     * @param excludeId Project ID to exclude from check (for update)
     * @return true if name exists
     */
    public boolean isNameDuplicate(String name, Long excludeId) {
        Long idToExclude = excludeId != null ? excludeId : -1L;
        return projectRepository.existsByNameAndDeletedAndIdNot(name, "N", idToExclude);
    }

    /**
     * Reorder projects by updating display order
     *
     * @param projectId Project ID to move
     * @param newOrder New display order
     */
    @Transactional
    public void updateDisplayOrder(Long projectId, Integer newOrder) {
        KanbanProject project = projectRepository.findByIdAndDeleted(projectId, "N")
                .orElseThrow(() -> new RuntimeException("Project not found: " + projectId));

        project.setDisplayOrder(newOrder);
        projectRepository.save(project);
        log.info("Updated project display order: id={}, newOrder={}", projectId, newOrder);
    }
}
