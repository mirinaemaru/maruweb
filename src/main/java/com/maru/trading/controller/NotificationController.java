package com.maru.trading.controller;

import com.maru.trading.entity.Notification;
import com.maru.trading.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 실시간 알림 컨트롤러
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * 테스트 알림 전송 API
     */
    @PostMapping("/api/notifications/test")
    @ResponseBody
    public Map<String, Object> sendTestNotification(
            @RequestParam(defaultValue = "INFO") String type,
            @RequestParam(defaultValue = "테스트 알림") String title,
            @RequestParam(defaultValue = "WebSocket 연결이 정상적으로 작동합니다.") String message) {

        log.info("Sending test notification: type={}, title={}, message={}", type, title, message);
        notificationService.sendNotificationToAll(type, title, message);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "알림이 전송되었습니다.");
        return response;
    }

    /**
     * 거래 체결 알림 테스트 API
     */
    @PostMapping("/api/notifications/test/trade")
    @ResponseBody
    public Map<String, Object> sendTestTradeNotification(
            @RequestParam(defaultValue = "005930") String symbol,
            @RequestParam(defaultValue = "BUY") String side,
            @RequestParam(defaultValue = "10") double quantity,
            @RequestParam(defaultValue = "75000") double price) {

        log.info("Sending test trade notification: symbol={}, side={}, quantity={}, price={}",
                symbol, side, quantity, price);
        notificationService.notifyTradeExecution(symbol, side, quantity, price);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "거래 체결 알림이 전송되었습니다.");
        response.put("details", Map.of(
                "symbol", symbol,
                "side", side,
                "quantity", quantity,
                "price", price
        ));
        return response;
    }

    /**
     * WebSocket을 통한 메시지 수신 처리
     */
    @MessageMapping("/notify")
    @SendTo("/topic/notifications")
    public Map<String, Object> handleNotification(Map<String, Object> notification) {
        log.info("Received notification via WebSocket: {}", notification);
        return notification;
    }

    /**
     * 알림 테스트 페이지
     */
    @GetMapping("/trading/notifications/test")
    public String testPage() {
        return "trading/notification-test";
    }

    /**
     * 알림 히스토리 페이지
     */
    @GetMapping("/trading/notifications/history")
    public String historyPage(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String readStatus,
            Model model) {

        List<Notification> notifications;

        if (type != null && !type.isEmpty()) {
            notifications = notificationService.getNotificationsByType(type);
        } else if ("N".equals(readStatus)) {
            notifications = notificationService.getUnreadNotifications();
        } else {
            notifications = notificationService.getAllNotifications();
        }

        model.addAttribute("notifications", notifications);
        model.addAttribute("unreadCount", notificationService.getUnreadCount());
        model.addAttribute("selectedType", type);
        model.addAttribute("selectedReadStatus", readStatus);

        return "trading/notification-history";
    }

    /**
     * 알림 읽음 처리
     */
    @PostMapping("/api/notifications/{id}/read")
    @ResponseBody
    public Map<String, Object> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "알림이 읽음 처리되었습니다.");
        return response;
    }

    /**
     * 모든 알림 읽음 처리
     */
    @PostMapping("/api/notifications/read-all")
    @ResponseBody
    public Map<String, Object> markAllAsRead() {
        notificationService.markAllAsRead();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "모든 알림이 읽음 처리되었습니다.");
        return response;
    }

    /**
     * 알림 삭제
     */
    @PostMapping("/api/notifications/{id}/delete")
    @ResponseBody
    public Map<String, Object> deleteNotification(@PathVariable Long id) {
        notificationService.deleteNotification(id);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "알림이 삭제되었습니다.");
        return response;
    }

    /**
     * 읽지 않은 알림 개수 조회
     */
    @GetMapping("/api/notifications/unread-count")
    @ResponseBody
    public Map<String, Object> getUnreadCount() {
        Map<String, Object> response = new HashMap<>();
        response.put("count", notificationService.getUnreadCount());
        return response;
    }

    /**
     * 알림 설정 페이지
     */
    @GetMapping("/trading/notifications/settings")
    public String settingsPage(Model model) {
        model.addAttribute("settings", notificationService.getSettings());
        return "trading/notification-settings";
    }

    /**
     * 알림 설정 저장
     */
    @PostMapping("/trading/notifications/settings")
    public String saveSettings(
            @RequestParam(required = false) Boolean enableTradeNotifications,
            @RequestParam(required = false) Boolean enableStrategyNotifications,
            @RequestParam(required = false) Boolean enableErrorNotifications,
            @RequestParam(required = false) Boolean enableWarningNotifications,
            @RequestParam(required = false) Boolean enableSuccessNotifications,
            @RequestParam(required = false) Boolean enableInfoNotifications,
            @RequestParam(required = false) Boolean enableSound,
            @RequestParam(required = false) Boolean enableBrowserNotification,
            Model model) {

        var settings = notificationService.getSettings();
        settings.setEnableTradeNotifications(enableTradeNotifications != null && enableTradeNotifications);
        settings.setEnableStrategyNotifications(enableStrategyNotifications != null && enableStrategyNotifications);
        settings.setEnableErrorNotifications(enableErrorNotifications != null && enableErrorNotifications);
        settings.setEnableWarningNotifications(enableWarningNotifications != null && enableWarningNotifications);
        settings.setEnableSuccessNotifications(enableSuccessNotifications != null && enableSuccessNotifications);
        settings.setEnableInfoNotifications(enableInfoNotifications != null && enableInfoNotifications);
        settings.setEnableSound(enableSound != null && enableSound);
        settings.setEnableBrowserNotification(enableBrowserNotification != null && enableBrowserNotification);

        notificationService.saveSettings(settings);

        model.addAttribute("settings", settings);
        model.addAttribute("success", "설정이 저장되었습니다.");
        return "trading/notification-settings";
    }
}
