package com.maru.habit.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "habit_records", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"habit_id", "record_date"})
})
@Getter
@Setter
@NoArgsConstructor
public class HabitRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "habit_id", nullable = false)
    private Habit habit;

    @Column(name = "record_date", nullable = false)
    private LocalDate recordDate;

    @Column(name = "completed", length = 1)
    private String completed = "Y";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public HabitRecord(Habit habit, LocalDate recordDate) {
        this.habit = habit;
        this.recordDate = recordDate;
        this.completed = "Y";
    }
}
