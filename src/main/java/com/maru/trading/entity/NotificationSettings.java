package com.maru.trading.entity;

import lombok.Data;
import javax.persistence.*;

/**
 * 알림 설정 엔티티
 */
@Entity
@Table(name = "notification_settings")
@Data
public class NotificationSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", length = 100)
    private String userId = "default"; // 단일 사용자 시스템이므로 default 사용

    @Column(name = "enable_trade_notifications")
    private Boolean enableTradeNotifications = true;

    @Column(name = "enable_strategy_notifications")
    private Boolean enableStrategyNotifications = true;

    @Column(name = "enable_error_notifications")
    private Boolean enableErrorNotifications = true;

    @Column(name = "enable_info_notifications")
    private Boolean enableInfoNotifications = true;

    @Column(name = "enable_success_notifications")
    private Boolean enableSuccessNotifications = true;

    @Column(name = "enable_warning_notifications")
    private Boolean enableWarningNotifications = true;

    @Column(name = "enable_sound")
    private Boolean enableSound = true;

    @Column(name = "enable_browser_notification")
    private Boolean enableBrowserNotification = false;
}
