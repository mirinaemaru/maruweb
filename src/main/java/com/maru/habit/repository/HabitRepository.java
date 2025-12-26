package com.maru.habit.repository;

import com.maru.habit.entity.Habit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HabitRepository extends JpaRepository<Habit, Long> {

    List<Habit> findByDeletedOrderByDisplayOrderAscCreatedAtAsc(String deleted);
}
