package com.maru.shortcut.service;

import com.maru.shortcut.entity.Shortcut;
import com.maru.shortcut.entity.ShortcutCategory;
import com.maru.shortcut.repository.ShortcutCategoryRepository;
import com.maru.shortcut.repository.ShortcutRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class ShortcutService {

    private final ShortcutRepository shortcutRepository;
    private final ShortcutCategoryRepository categoryRepository;

    // ========== Category Methods ==========

    @Transactional(readOnly = true)
    public List<ShortcutCategory> getAllCategories() {
        return categoryRepository.findByDeletedOrderByDisplayOrderAsc("N");
    }

    @Transactional(readOnly = true)
    public List<ShortcutCategory> getAllCategoriesWithShortcuts() {
        List<ShortcutCategory> categories = categoryRepository.findByDeletedOrderByDisplayOrderAsc("N");
        // Filter out deleted shortcuts
        for (ShortcutCategory category : categories) {
            category.getShortcuts().removeIf(s -> "Y".equals(s.getDeleted()));
            category.getShortcuts().sort((a, b) -> {
                int orderA = a.getDisplayOrder() != null ? a.getDisplayOrder() : 0;
                int orderB = b.getDisplayOrder() != null ? b.getDisplayOrder() : 0;
                return Integer.compare(orderA, orderB);
            });
        }
        return categories;
    }

    @Transactional(readOnly = true)
    public Optional<ShortcutCategory> getCategoryById(Long id) {
        return categoryRepository.findByIdAndDeleted(id, "N");
    }

    public ShortcutCategory createCategory(ShortcutCategory category) {
        if (category.getDisplayOrder() == null) {
            category.setDisplayOrder(0);
        }
        return categoryRepository.save(category);
    }

    public ShortcutCategory updateCategory(Long id, ShortcutCategory updated) {
        ShortcutCategory category = categoryRepository.findByIdAndDeleted(id, "N")
                .orElseThrow(() -> new RuntimeException("Category not found: " + id));
        category.setName(updated.getName());
        category.setDescription(updated.getDescription());
        if (updated.getDisplayOrder() != null) {
            category.setDisplayOrder(updated.getDisplayOrder());
        }
        return categoryRepository.save(category);
    }

    public void deleteCategory(Long id) {
        ShortcutCategory category = categoryRepository.findByIdAndDeleted(id, "N")
                .orElseThrow(() -> new RuntimeException("Category not found: " + id));
        category.setDeleted("Y");
        // Also soft delete all shortcuts in this category
        for (Shortcut shortcut : category.getShortcuts()) {
            shortcut.setDeleted("Y");
        }
        categoryRepository.save(category);
    }

    // ========== Shortcut Methods ==========

    @Transactional(readOnly = true)
    public List<Shortcut> getAllShortcuts() {
        return shortcutRepository.findByDeletedOrderByDisplayOrderAsc("N");
    }

    @Transactional(readOnly = true)
    public List<Shortcut> getShortcutsByCategory(ShortcutCategory category) {
        return shortcutRepository.findByCategoryAndDeletedOrderByDisplayOrderAsc(category, "N");
    }

    @Transactional(readOnly = true)
    public Optional<Shortcut> getShortcutById(Long id) {
        return shortcutRepository.findByIdAndDeleted(id, "N");
    }

    public Shortcut createShortcut(Shortcut shortcut) {
        if (shortcut.getDisplayOrder() == null) {
            shortcut.setDisplayOrder(0);
        }
        return shortcutRepository.save(shortcut);
    }

    public Shortcut updateShortcut(Long id, Shortcut updated) {
        Shortcut shortcut = shortcutRepository.findByIdAndDeleted(id, "N")
                .orElseThrow(() -> new RuntimeException("Shortcut not found: " + id));
        shortcut.setName(updated.getName());
        shortcut.setUrl(updated.getUrl());
        shortcut.setDescription(updated.getDescription());
        if (updated.getDisplayOrder() != null) {
            shortcut.setDisplayOrder(updated.getDisplayOrder());
        }
        if (updated.getCategory() != null) {
            shortcut.setCategory(updated.getCategory());
        }
        return shortcutRepository.save(shortcut);
    }

    public void deleteShortcut(Long id) {
        Shortcut shortcut = shortcutRepository.findByIdAndDeleted(id, "N")
                .orElseThrow(() -> new RuntimeException("Shortcut not found: " + id));
        shortcut.setDeleted("Y");
        shortcutRepository.save(shortcut);
    }
}
