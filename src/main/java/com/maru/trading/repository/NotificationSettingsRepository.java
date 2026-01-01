package com.maru.trading.repository;

import com.maru.trading.entity.NotificationSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationSettingsRepository extends JpaRepository<NotificationSettings, Long> {

    /**
     * 사용자 ID로 알림 설정 조회
     */
    Optional<NotificationSettings> findByUserId(String userId);
}
