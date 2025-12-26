package com.maru.note.repository;

import com.maru.note.entity.Note;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NoteRepository extends JpaRepository<Note, Long> {

    List<Note> findByDeletedOrderByPinnedDescUpdatedAtDesc(String deleted);

    List<Note> findByDeletedAndCategoryOrderByPinnedDescUpdatedAtDesc(String deleted, String category);

    @Query("SELECT DISTINCT n.category FROM Note n WHERE n.deleted = 'N' AND n.category IS NOT NULL AND n.category != '' ORDER BY n.category")
    List<String> findAllCategories();

    @Query("SELECT n FROM Note n WHERE n.deleted = 'N' AND " +
           "(LOWER(n.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(n.content) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY n.pinned DESC, n.updatedAt DESC")
    List<Note> searchByKeyword(@Param("keyword") String keyword);
}
