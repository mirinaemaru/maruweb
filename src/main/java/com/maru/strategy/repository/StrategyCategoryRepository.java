package com.maru.strategy.repository;

import com.maru.strategy.entity.StrategyCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StrategyCategoryRepository extends JpaRepository<StrategyCategory, Long> {

    List<StrategyCategory> findByDeletedOrderByDisplayOrderAsc(String deleted);

    Optional<StrategyCategory> findByIdAndDeleted(Long id, String deleted);
}
