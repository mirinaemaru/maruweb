package com.maru.dday.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "ddays")
@Getter
@Setter
@NoArgsConstructor
public class DDay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Title is required")
    @Column(nullable = false)
    private String title;

    private String description;

    @NotNull(message = "Target date is required")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Column(name = "target_date", nullable = false)
    private LocalDate targetDate;

    @Column(name = "icon")
    private String icon = "🎯";

    @Column(name = "deleted", length = 1)
    private String deleted = "N";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Calculate D-Day (negative = days left, positive = days passed)
    public long getDaysRemaining() {
        return ChronoUnit.DAYS.between(LocalDate.now(), targetDate);
    }

    // Format D-Day display string
    public String getDDayDisplay() {
        long days = getDaysRemaining();
        if (days == 0) {
            return "D-Day";
        } else if (days > 0) {
            return "D-" + days;
        } else {
            return "D+" + Math.abs(days);
        }
    }

    public boolean isPast() {
        return targetDate.isBefore(LocalDate.now());
    }

    public boolean isToday() {
        return targetDate.equals(LocalDate.now());
    }
}
