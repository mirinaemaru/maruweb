package com.maru.calendar.entity;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "google_oauth_tokens")
@Data
public class GoogleOAuthToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String userIdentifier = "default"; // For future multi-user support

    @Column(nullable = false, columnDefinition = "TEXT")
    private String accessToken; // Encrypted

    @Column(columnDefinition = "TEXT")
    private String refreshToken; // Encrypted

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(length = 500)
    private String scope;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
