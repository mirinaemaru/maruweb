package com.maru.habit.repository;

import com.maru.habit.entity.Habit;
import com.maru.habit.entity.HabitRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface HabitRecordRepository extends JpaRepository<HabitRecord, Long> {

    List<HabitRecord> findByHabitAndRecordDateBetween(Habit habit, LocalDate startDate, LocalDate endDate);

    Optional<HabitRecord> findByHabitAndRecordDate(Habit habit, LocalDate recordDate);

    List<HabitRecord> findByHabit(Habit habit);

    void deleteByHabitAndRecordDate(Habit habit, LocalDate recordDate);
}
