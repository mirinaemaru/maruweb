package com.maru.calendar.entity;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Entity
@Table(name = "calendar_events")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CalendarEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Title is required")
    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @NotNull(message = "Start date/time is required")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    @Column(name = "start_datetime", nullable = false)
    private LocalDateTime startDateTime;

    @NotNull(message = "End date/time is required")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    @Column(name = "end_datetime", nullable = false)
    private LocalDateTime endDateTime;

    @Column(name = "all_day", nullable = false, length = 1)
    private String allDay = "N"; // "Y" or "N"

    @Column(name = "location")
    private String location;

    // Google Calendar Integration (nullable for now, will be used later)
    @Column(name = "google_event_id", unique = true)
    private String googleEventId;

    @Column(name = "google_calendar_id")
    private String googleCalendarId;

    @Column(name = "sync_status", length = 20)
    private String syncStatus = "LOCAL_ONLY"; // "SYNCED", "PENDING", "FAILED", "LOCAL_ONLY"

    @Column(name = "last_synced_at")
    private LocalDateTime lastSyncedAt;

    // Soft delete flag (following Todo pattern)
    @Column(nullable = false, length = 1)
    private String deleted = "N";

    // Audit fields
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
