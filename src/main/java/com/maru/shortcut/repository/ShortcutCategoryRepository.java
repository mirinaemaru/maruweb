package com.maru.shortcut.repository;

import com.maru.shortcut.entity.ShortcutCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShortcutCategoryRepository extends JpaRepository<ShortcutCategory, Long> {

    List<ShortcutCategory> findByDeletedOrderByDisplayOrderAsc(String deleted);

    Optional<ShortcutCategory> findByIdAndDeleted(Long id, String deleted);

    @Query("SELECT c FROM ShortcutCategory c LEFT JOIN FETCH c.shortcuts s " +
           "WHERE c.deleted = 'N' AND (s IS NULL OR s.deleted = 'N') " +
           "ORDER BY c.displayOrder ASC, s.displayOrder ASC")
    List<ShortcutCategory> findAllCategoriesWithShortcuts();
}
