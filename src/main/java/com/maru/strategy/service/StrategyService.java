package com.maru.strategy.service;

import com.maru.strategy.entity.Strategy;
import com.maru.strategy.entity.StrategyCategory;
import com.maru.strategy.repository.StrategyCategoryRepository;
import com.maru.strategy.repository.StrategyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StrategyService {

    private final StrategyRepository strategyRepository;
    private final StrategyCategoryRepository categoryRepository;

    // ========== Category Methods ==========

    public List<StrategyCategory> getAllCategories() {
        return categoryRepository.findByDeletedOrderByDisplayOrderAsc("N");
    }

    public List<StrategyCategory> getAllCategoriesWithStrategies() {
        List<StrategyCategory> categories = categoryRepository.findByDeletedOrderByDisplayOrderAsc("N");
        // Filter out deleted strategies
        for (StrategyCategory category : categories) {
            category.getStrategies().removeIf(s -> "Y".equals(s.getDeleted()));
        }
        return categories;
    }

    public Optional<StrategyCategory> getCategoryById(Long id) {
        return categoryRepository.findByIdAndDeleted(id, "N");
    }

    @Transactional
    public StrategyCategory createCategory(StrategyCategory category) {
        if (category.getDisplayOrder() == null) {
            category.setDisplayOrder(0);
        }
        return categoryRepository.save(category);
    }

    @Transactional
    public StrategyCategory updateCategory(Long id, StrategyCategory updated) {
        StrategyCategory category = categoryRepository.findByIdAndDeleted(id, "N")
                .orElseThrow(() -> new RuntimeException("Category not found: " + id));
        category.setName(updated.getName());
        category.setDescription(updated.getDescription());
        if (updated.getDisplayOrder() != null) {
            category.setDisplayOrder(updated.getDisplayOrder());
        }
        return categoryRepository.save(category);
    }

    @Transactional
    public void deleteCategory(Long id) {
        StrategyCategory category = categoryRepository.findByIdAndDeleted(id, "N")
                .orElseThrow(() -> new RuntimeException("Category not found: " + id));
        category.setDeleted("Y");
        // Cascade soft delete to all strategies in this category
        for (Strategy strategy : category.getStrategies()) {
            if (!"Y".equals(strategy.getDeleted())) {
                strategy.setDeleted("Y");
            }
        }
        categoryRepository.save(category);
    }

    // ========== Strategy Methods ==========

    public List<Strategy> getAllStrategies() {
        return strategyRepository.findByDeletedOrderByCreatedAtDesc("N");
    }

    public List<Strategy> getStrategiesByStatus(String status) {
        return strategyRepository.findByStatusAndDeletedOrderByCreatedAtDesc(status, "N");
    }

    public List<Strategy> getStrategiesByPriority(String priority) {
        return strategyRepository.findByPriorityAndDeletedOrderByCreatedAtDesc(priority, "N");
    }

    public List<Strategy> getStrategiesByCategory(Long categoryId) {
        StrategyCategory category = categoryRepository.findByIdAndDeleted(categoryId, "N")
                .orElseThrow(() -> new RuntimeException("Category not found: " + categoryId));
        return strategyRepository.findByCategoryAndDeletedOrderByCreatedAtDesc(category, "N");
    }

    public List<Strategy> searchStrategies(String status, String priority, Long categoryId, String keyword) {
        return strategyRepository.searchStrategies(status, priority, categoryId, keyword, "N");
    }

    public List<Strategy> getStrategiesByStartDateRange(LocalDate from, LocalDate to) {
        return strategyRepository.findByStartDateRange(from, to, "N");
    }

    public List<Strategy> getStrategiesByTargetDateRange(LocalDate from, LocalDate to) {
        return strategyRepository.findByTargetDateRange(from, to, "N");
    }

    public Optional<Strategy> getStrategyById(Long id) {
        return strategyRepository.findByIdAndDeleted(id, "N");
    }

    @Transactional
    public Strategy createStrategy(Strategy strategy) {
        // Validation
        if (strategy.getStatus() == null || strategy.getStatus().isEmpty()) {
            strategy.setStatus("PLANNING");
        }
        if (strategy.getPriority() == null || strategy.getPriority().isEmpty()) {
            strategy.setPriority("MEDIUM");
        }
        return strategyRepository.save(strategy);
    }

    @Transactional
    public Strategy updateStrategy(Long id, Strategy updated) {
        Strategy strategy = strategyRepository.findByIdAndDeleted(id, "N")
                .orElseThrow(() -> new RuntimeException("Strategy not found: " + id));

        strategy.setTitle(updated.getTitle());
        strategy.setDescription(updated.getDescription());
        strategy.setStatus(updated.getStatus());
        strategy.setPriority(updated.getPriority());
        strategy.setStartDate(updated.getStartDate());
        strategy.setEndDate(updated.getEndDate());
        strategy.setTargetDate(updated.getTargetDate());

        // Update strategy type
        if (updated.getStrategyType() != null) {
            strategy.setStrategyType(updated.getStrategyType());
        }

        if (updated.getCategory() != null) {
            strategy.setCategory(updated.getCategory());
        } else {
            strategy.setCategory(null);
        }

        return strategyRepository.save(strategy);
    }

    @Transactional
    public void deleteStrategy(Long id) {
        Strategy strategy = strategyRepository.findByIdAndDeleted(id, "N")
                .orElseThrow(() -> new RuntimeException("Strategy not found: " + id));
        strategy.setDeleted("Y");
        strategyRepository.save(strategy);
    }
}
