package com.maru.trading.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 알림 히스토리 엔티티
 */
@Entity
@Table(name = "notifications")
@Data
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String type; // INFO, SUCCESS, WARNING, ERROR, TRADE, STRATEGY

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "read_status", length = 1)
    private String readStatus = "N"; // Y/N

    @Column(name = "deleted", length = 1)
    private String deleted = "N"; // Y/N

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (readStatus == null) readStatus = "N";
        if (deleted == null) deleted = "N";
    }
}
