package com.maru.kanban.repository;

import com.maru.kanban.entity.KanbanProject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KanbanProjectRepository extends JpaRepository<KanbanProject, Long> {

    // Find all active projects ordered by display order
    List<KanbanProject> findByDeletedOrderByDisplayOrderAsc(String deleted);

    // Find by ID with active check
    Optional<KanbanProject> findByIdAndDeleted(Long id, String deleted);

    // Find by directory path (for validation)
    Optional<KanbanProject> findByDirectoryPathAndDeleted(String directoryPath, String deleted);

    // Check if name exists (for duplicate prevention)
    boolean existsByNameAndDeletedAndIdNot(String name, String deleted, Long id);

    // Get projects with task counts (eager fetch)
    @Query("SELECT DISTINCT p FROM KanbanProject p LEFT JOIN FETCH p.tasks t " +
           "WHERE p.deleted = :deleted AND (t.deleted = 'N' OR t IS NULL) " +
           "ORDER BY p.displayOrder ASC")
    List<KanbanProject> findAllWithTasks(@Param("deleted") String deleted);
}
