package com.maru.trading.service;

import com.maru.trading.entity.Notification;
import com.maru.trading.entity.NotificationSettings;
import com.maru.trading.repository.NotificationRepository;
import com.maru.trading.repository.NotificationSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 실시간 알림 서비스
 * WebSocket을 통해 클라이언트에게 실시간 알림 전송
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationRepository notificationRepository;
    private final NotificationSettingsRepository settingsRepository;

    /**
     * 모든 클라이언트에게 알림 전송 및 DB 저장
     */
    @Transactional
    public void sendNotificationToAll(String type, String title, String message) {
        // 알림 설정 확인
        if (!isNotificationEnabled(type)) {
            log.debug("Notification disabled for type: {}", type);
            return;
        }

        // DB에 저장
        Notification notification = new Notification();
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notificationRepository.save(notification);

        // WebSocket으로 전송
        Map<String, Object> payload = createNotification(type, title, message);
        log.info("Sending notification to all clients: {}", payload);
        messagingTemplate.convertAndSend("/topic/notifications", payload);
    }

    /**
     * 특정 사용자에게 알림 전송
     */
    public void sendNotificationToUser(String userId, String type, String title, String message) {
        Map<String, Object> notification = createNotification(type, title, message);
        log.info("Sending notification to user {}: {}", userId, notification);
        messagingTemplate.convertAndSendToUser(userId, "/queue/notifications", notification);
    }

    /**
     * 거래 체결 알림
     */
    public void notifyTradeExecution(String symbol, String side, double quantity, double price) {
        String title = String.format("%s %s 체결", symbol, side.equals("BUY") ? "매수" : "매도");
        String message = String.format("수량: %.0f, 가격: %,.0f원", quantity, price);
        sendNotificationToAll("TRADE", title, message);
    }

    /**
     * 전략 상태 변경 알림
     */
    public void notifyStrategyStatusChange(String strategyName, String oldStatus, String newStatus) {
        String title = "전략 상태 변경";
        String message = String.format("%s: %s → %s", strategyName, oldStatus, newStatus);
        sendNotificationToAll("STRATEGY", title, message);
    }

    /**
     * 계좌 잔고 알림
     */
    public void notifyBalanceUpdate(String accountId, double balance, double change) {
        String title = "잔고 변동";
        String message = String.format("계좌 %s: %,.0f원 (%+,.0f원)", accountId, balance, change);
        String type = change >= 0 ? "SUCCESS" : "WARNING";
        sendNotificationToAll(type, title, message);
    }

    /**
     * 에러 알림
     */
    public void notifyError(String title, String message) {
        sendNotificationToAll("ERROR", title, message);
    }

    /**
     * 시스템 알림
     */
    public void notifySystemMessage(String message) {
        sendNotificationToAll("INFO", "시스템 알림", message);
    }

    /**
     * 알림 객체 생성
     */
    private Map<String, Object> createNotification(String type, String title, String message) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", type);  // TRADE, STRATEGY, SUCCESS, WARNING, ERROR, INFO
        notification.put("title", title);
        notification.put("message", message);
        notification.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        return notification;
    }

    /**
     * 알림 목록 조회
     */
    public List<Notification> getAllNotifications() {
        return notificationRepository.findByDeletedOrderByCreatedAtDesc("N");
    }

    /**
     * 타입별 알림 조회
     */
    public List<Notification> getNotificationsByType(String type) {
        return notificationRepository.findByTypeAndDeletedOrderByCreatedAtDesc(type, "N");
    }

    /**
     * 읽지 않은 알림 조회
     */
    public List<Notification> getUnreadNotifications() {
        return notificationRepository.findByReadStatusAndDeletedOrderByCreatedAtDesc("N", "N");
    }

    /**
     * 기간별 알림 조회
     */
    public List<Notification> getNotificationsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return notificationRepository.findByCreatedAtBetweenAndDeletedOrderByCreatedAtDesc(startDate, endDate, "N");
    }

    /**
     * 읽지 않은 알림 개수
     */
    public long getUnreadCount() {
        return notificationRepository.countByReadStatusAndDeleted("N", "N");
    }

    /**
     * 알림 읽음 처리
     */
    @Transactional
    public void markAsRead(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            notification.setReadStatus("Y");
            notificationRepository.save(notification);
        });
    }

    /**
     * 모든 알림 읽음 처리
     */
    @Transactional
    public void markAllAsRead() {
        List<Notification> unreadNotifications = getUnreadNotifications();
        unreadNotifications.forEach(notification -> notification.setReadStatus("Y"));
        notificationRepository.saveAll(unreadNotifications);
    }

    /**
     * 알림 삭제 (소프트 삭제)
     */
    @Transactional
    public void deleteNotification(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            notification.setDeleted("Y");
            notificationRepository.save(notification);
        });
    }

    /**
     * 알림 설정 조회 (없으면 기본값 생성)
     */
    public NotificationSettings getSettings() {
        return settingsRepository.findByUserId("default")
                .orElseGet(() -> {
                    NotificationSettings settings = new NotificationSettings();
                    settings.setUserId("default");
                    return settingsRepository.save(settings);
                });
    }

    /**
     * 알림 설정 저장
     */
    @Transactional
    public NotificationSettings saveSettings(NotificationSettings settings) {
        settings.setUserId("default"); // 단일 사용자 시스템
        return settingsRepository.save(settings);
    }

    /**
     * 타입별 알림 활성화 여부 확인
     */
    private boolean isNotificationEnabled(String type) {
        NotificationSettings settings = getSettings();

        switch (type) {
            case "TRADE":
                return settings.getEnableTradeNotifications();
            case "STRATEGY":
                return settings.getEnableStrategyNotifications();
            case "ERROR":
                return settings.getEnableErrorNotifications();
            case "WARNING":
                return settings.getEnableWarningNotifications();
            case "SUCCESS":
                return settings.getEnableSuccessNotifications();
            case "INFO":
                return settings.getEnableInfoNotifications();
            default:
                return true;
        }
    }
}
