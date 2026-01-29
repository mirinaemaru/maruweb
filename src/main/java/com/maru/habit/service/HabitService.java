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

    @Transactional
    public void saveNumericRecord(Long habitId, LocalDate date, Double value) {
        habitRepository.findById(habitId)
                .filter(habit -> "N".equals(habit.getDeleted()))
                .ifPresent(habit -> {
                    Optional<HabitRecord> existingRecord = habitRecordRepository.findByHabitAndRecordDate(habit, date);
                    boolean isEmptyValue = value == null || value == 0.0;
                    if (existingRecord.isPresent()) {
                        HabitRecord record = existingRecord.get();
                        if (isEmptyValue) {
                            habitRecordRepository.delete(record);
                        } else {
                            record.setNumericValue(value);
                            habitRecordRepository.save(record);
                        }
                    } else if (!isEmptyValue) {
                        habitRecordRepository.save(new HabitRecord(habit, date, value));
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

    public Map<Long, Map<LocalDate, Double>> getNumericRecordsForMonth(int year, int month) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.plusMonths(1).minusDays(1);

        Map<Long, Map<LocalDate, Double>> numericMap = new HashMap<>();
        List<Habit> habits = getAllHabits();

        for (Habit habit : habits) {
            List<HabitRecord> records = habitRecordRepository.findByHabitAndRecordDateBetween(habit, startDate, endDate);
            Map<LocalDate, Double> dateValueMap = new HashMap<>();
            for (HabitRecord record : records) {
                if (record.getNumericValue() != null && record.getNumericValue() != 0.0) {
                    dateValueMap.put(record.getRecordDate(), record.getNumericValue());
                }
            }
            numericMap.put(habit.getId(), dateValueMap);
        }

        return numericMap;
    }

    public Map<Long, Integer> getMonthlyCounts(int year, int month) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.plusMonths(1).minusDays(1);

        Map<Long, Integer> countMap = new HashMap<>();
        List<Habit> habits = getAllHabits();

        for (Habit habit : habits) {
            List<HabitRecord> records = habitRecordRepository.findByHabitAndRecordDateBetween(habit, startDate, endDate);
            int count = 0;
            for (HabitRecord record : records) {
                if (record.getNumericValue() != null) {
                    if (record.getNumericValue() != 0.0) {
                        count++;
                    }
                } else {
                    count++;
                }
            }
            countMap.put(habit.getId(), count);
        }

        return countMap;
    }
}
