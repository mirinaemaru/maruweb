package com.maru.habit.service;

import com.maru.habit.entity.Habit;
import com.maru.habit.entity.HabitRecord;
import com.maru.habit.repository.HabitRecordRepository;
import com.maru.habit.repository.HabitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HabitService {

    private final HabitRepository habitRepository;
    private final HabitRecordRepository habitRecordRepository;

    public List<Habit> getAllHabits() {
        return habitRepository.findByDeletedOrderByDisplayOrderAscCreatedAtAsc("N");
    }

    public Optional<Habit> getHabitById(Long id) {
        return habitRepository.findById(id)
                .filter(habit -> "N".equals(habit.getDeleted()));
    }

    @Transactional
    public Habit createHabit(Habit habit) {
        return habitRepository.save(habit);
    }

    @Transactional
    public Habit updateHabit(Long id, Habit updatedHabit) {
        return habitRepository.findById(id)
                .filter(habit -> "N".equals(habit.getDeleted()))
                .map(habit -> {
                    habit.setName(updatedHabit.getName());
                    habit.setDescription(updatedHabit.getDescription());
                    habit.setIcon(updatedHabit.getIcon());
                    habit.setColor(updatedHabit.getColor());
                    return habitRepository.save(habit);
                })
                .orElseThrow(() -> new IllegalArgumentException("Habit not found with id: " + id));
    }

    @Transactional
    public void deleteHabit(Long id) {
        habitRepository.findById(id)
                .filter(habit -> "N".equals(habit.getDeleted()))
                .ifPresent(habit -> {
                    habit.setDeleted("Y");
                    habitRepository.save(habit);
                });
    }

    @Transactional
    public void toggleRecord(Long habitId, LocalDate date) {
        habitRepository.findById(habitId)
                .filter(habit -> "N".equals(habit.getDeleted()))
                .ifPresent(habit -> {
                    Optional<HabitRecord> existingRecord = habitRecordRepository.findByHabitAndRecordDate(habit, date);
                    if (existingRecord.isPresent()) {
                        habitRecordRepository.delete(existingRecord.get());
                    } else {
                        habitRecordRepository.save(new HabitRecord(habit, date));
                    }
                });
    }

    public Map<Long, Set<LocalDate>> getRecordsForMonth(int year, int month) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.plusMonths(1).minusDays(1);

        Map<Long, Set<LocalDate>> recordMap = new HashMap<>();
        List<Habit> habits = getAllHabits();

        for (Habit habit : habits) {
            List<HabitRecord> records = habitRecordRepository.findByHabitAndRecordDateBetween(habit, startDate, endDate);
            Set<LocalDate> dates = new HashSet<>();
            for (HabitRecord record : records) {
                dates.add(record.getRecordDate());
            }
            recordMap.put(habit.getId(), dates);
        }

        return recordMap;
    }

    public int getStreakCount(Habit habit) {
        LocalDate today = LocalDate.now();
        int streak = 0;
        LocalDate checkDate = today;

        while (true) {
            Optional<HabitRecord> record = habitRecordRepository.findByHabitAndRecordDate(habit, checkDate);
            if (record.isPresent()) {
                streak++;
                checkDate = checkDate.minusDays(1);
            } else {
                break;
            }
        }
        return streak;
    }

    public Map<Long, Integer> getStreakCounts() {
        Map<Long, Integer> streakMap = new HashMap<>();
        List<Habit> habits = getAllHabits();
        for (Habit habit : habits) {
            streakMap.put(habit.getId(), getStreakCount(habit));
        }
        return streakMap;
    }
}
