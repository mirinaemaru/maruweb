package com.maru.shortcut.repository;

import com.maru.shortcut.entity.Shortcut;
import com.maru.shortcut.entity.ShortcutCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShortcutRepository extends JpaRepository<Shortcut, Long> {

    List<Shortcut> findByCategoryAndDeletedOrderByDisplayOrderAsc(ShortcutCategory category, String deleted);

    List<Shortcut> findByDeletedOrderByDisplayOrderAsc(String deleted);

    Optional<Shortcut> findByIdAndDeleted(Long id, String deleted);
}
