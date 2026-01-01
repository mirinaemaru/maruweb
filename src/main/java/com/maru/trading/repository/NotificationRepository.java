package com.maru.trading.repository;

import com.maru.trading.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * 삭제되지 않은 알림 목록 조회 (최신순)
     */
    List<Notification> findByDeletedOrderByCreatedAtDesc(String deleted);

    /**
     * 타입별 알림 조회
     */
    List<Notification> findByTypeAndDeletedOrderByCreatedAtDesc(String type, String deleted);

    /**
     * 읽지 않은 알림 조회
     */
    List<Notification> findByReadStatusAndDeletedOrderByCreatedAtDesc(String readStatus, String deleted);

    /**
     * 기간별 알림 조회
     */
    List<Notification> findByCreatedAtBetweenAndDeletedOrderByCreatedAtDesc(
            LocalDateTime startDate, LocalDateTime endDate, String deleted);

    /**
     * 읽지 않은 알림 개수
     */
    long countByReadStatusAndDeleted(String readStatus, String deleted);
}
