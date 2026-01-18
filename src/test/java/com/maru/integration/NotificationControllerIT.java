package com.maru.integration;

import com.maru.todo.TodoApplication;
import com.maru.trading.entity.Notification;
import com.maru.trading.entity.NotificationSettings;
import com.maru.trading.service.NotificationService;
import com.maru.trading.service.TradingApiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = {TodoApplication.class, TestConfig.class})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("NotificationController 통합테스트")
class NotificationControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationService notificationService;

    @MockBean
    private TradingApiService tradingApiService;

    @BeforeEach
    void setUp() {
        Map<String, Object> healthStatus = new HashMap<>();
        healthStatus.put("status", "UP");
        when(tradingApiService.getHealthStatus()).thenReturn(healthStatus);
    }

    // ========== 테스트 알림 전송 API ==========

    @Test
    @DisplayName("테스트 알림 전송 - 기본값")
    void sendTestNotification_DefaultValues() throws Exception {
        // Given
        doNothing().when(notificationService).sendNotificationToAll(anyString(), anyString(), anyString());

        // When & Then
        mockMvc.perform(post("/api/notifications/test"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").exists());

        verify(notificationService).sendNotificationToAll("INFO", "테스트 알림",
                "WebSocket 연결이 정상적으로 작동합니다.");
    }

    @Test
    @DisplayName("테스트 알림 전송 - 커스텀 파라미터")
    void sendTestNotification_CustomParams() throws Exception {
        // Given
        doNothing().when(notificationService).sendNotificationToAll(anyString(), anyString(), anyString());

        // When & Then
        mockMvc.perform(post("/api/notifications/test")
                        .param("type", "WARNING")
                        .param("title", "경고 알림")
                        .param("message", "테스트 경고 메시지"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(notificationService).sendNotificationToAll("WARNING", "경고 알림", "테스트 경고 메시지");
    }

    // ========== 거래 체결 알림 테스트 ==========

    @Test
    @DisplayName("거래 체결 알림 테스트 - 기본값")
    void sendTestTradeNotification_DefaultValues() throws Exception {
        // Given
        doNothing().when(notificationService).notifyTradeExecution(anyString(), anyString(), anyDouble(), anyDouble());

        // When & Then
        mockMvc.perform(post("/api/notifications/test/trade"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.details.symbol").value("005930"))
                .andExpect(jsonPath("$.details.side").value("BUY"));

        verify(notificationService).notifyTradeExecution("005930", "BUY", 10.0, 75000.0);
    }

    @Test
    @DisplayName("거래 체결 알림 테스트 - 커스텀 파라미터")
    void sendTestTradeNotification_CustomParams() throws Exception {
        // Given
        doNothing().when(notificationService).notifyTradeExecution(anyString(), anyString(), anyDouble(), anyDouble());

        // When & Then
        mockMvc.perform(post("/api/notifications/test/trade")
                        .param("symbol", "000660")
                        .param("side", "SELL")
                        .param("quantity", "50")
                        .param("price", "180000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.details.symbol").value("000660"))
                .andExpect(jsonPath("$.details.side").value("SELL"))
                .andExpect(jsonPath("$.details.quantity").value(50.0))
                .andExpect(jsonPath("$.details.price").value(180000.0));
    }

    // ========== 알림 테스트 페이지 ==========

    @Test
    @DisplayName("알림 테스트 페이지 - 성공")
    void testPage_Success() throws Exception {
        mockMvc.perform(get("/trading/notifications/test"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/notification-test"));
    }

    // ========== 알림 히스토리 ==========

    @Test
    @DisplayName("알림 히스토리 페이지 - 전체 조회")
    void historyPage_AllNotifications() throws Exception {
        // Given
        List<Notification> notifications = createNotificationsList();
        when(notificationService.getAllNotifications()).thenReturn(notifications);
        when(notificationService.getUnreadCount()).thenReturn(2L);

        // When & Then
        mockMvc.perform(get("/trading/notifications/history"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/notification-history"))
                .andExpect(model().attributeExists("notifications"))
                .andExpect(model().attribute("unreadCount", 2L));
    }

    @Test
    @DisplayName("알림 히스토리 - 타입 필터")
    void historyPage_FilterByType() throws Exception {
        // Given
        List<Notification> notifications = createNotificationsList();
        when(notificationService.getNotificationsByType("ERROR")).thenReturn(notifications);
        when(notificationService.getUnreadCount()).thenReturn(1L);

        // When & Then
        mockMvc.perform(get("/trading/notifications/history")
                        .param("type", "ERROR"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/notification-history"))
                .andExpect(model().attribute("selectedType", "ERROR"));
    }

    @Test
    @DisplayName("알림 히스토리 - 읽지 않은 알림만")
    void historyPage_UnreadOnly() throws Exception {
        // Given
        List<Notification> notifications = createNotificationsList();
        when(notificationService.getUnreadNotifications()).thenReturn(notifications);
        when(notificationService.getUnreadCount()).thenReturn(2L);

        // When & Then
        mockMvc.perform(get("/trading/notifications/history")
                        .param("readStatus", "N"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/notification-history"))
                .andExpect(model().attribute("selectedReadStatus", "N"));
    }

    // ========== 알림 읽음 처리 ==========

    @Test
    @DisplayName("알림 읽음 처리 - 성공")
    void markAsRead_Success() throws Exception {
        // Given
        doNothing().when(notificationService).markAsRead(1L);

        // When & Then
        mockMvc.perform(post("/api/notifications/{id}/read", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(notificationService).markAsRead(1L);
    }

    @Test
    @DisplayName("모든 알림 읽음 처리 - 성공")
    void markAllAsRead_Success() throws Exception {
        // Given
        doNothing().when(notificationService).markAllAsRead();

        // When & Then
        mockMvc.perform(post("/api/notifications/read-all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(notificationService).markAllAsRead();
    }

    // ========== 알림 삭제 ==========

    @Test
    @DisplayName("알림 삭제 - 성공")
    void deleteNotification_Success() throws Exception {
        // Given
        doNothing().when(notificationService).deleteNotification(1L);

        // When & Then
        mockMvc.perform(post("/api/notifications/{id}/delete", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(notificationService).deleteNotification(1L);
    }

    // ========== 읽지 않은 알림 개수 ==========

    @Test
    @DisplayName("읽지 않은 알림 개수 조회 - 성공")
    void getUnreadCount_Success() throws Exception {
        // Given
        when(notificationService.getUnreadCount()).thenReturn(5L);

        // When & Then
        mockMvc.perform(get("/api/notifications/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(5));
    }

    // ========== 알림 설정 ==========

    @Test
    @DisplayName("알림 설정 페이지 - 성공")
    void settingsPage_Success() throws Exception {
        // Given
        NotificationSettings settings = createNotificationSettings();
        when(notificationService.getSettings()).thenReturn(settings);

        // When & Then
        mockMvc.perform(get("/trading/notifications/settings"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/notification-settings"))
                .andExpect(model().attributeExists("settings"));
    }

    @Test
    @DisplayName("알림 설정 저장 - 성공")
    void saveSettings_Success() throws Exception {
        // Given
        NotificationSettings settings = createNotificationSettings();
        when(notificationService.getSettings()).thenReturn(settings);
        when(notificationService.saveSettings(any(NotificationSettings.class))).thenReturn(settings);

        // When & Then
        mockMvc.perform(post("/trading/notifications/settings")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("enableTradeNotifications", "true")
                        .param("enableStrategyNotifications", "true")
                        .param("enableErrorNotifications", "true")
                        .param("enableSound", "false")
                        .param("enableBrowserNotification", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/notification-settings"))
                .andExpect(model().attributeExists("success"));

        verify(notificationService).saveSettings(any(NotificationSettings.class));
    }

    @Test
    @DisplayName("알림 설정 저장 - 전부 비활성화")
    void saveSettings_AllDisabled() throws Exception {
        // Given
        NotificationSettings settings = createNotificationSettings();
        when(notificationService.getSettings()).thenReturn(settings);
        when(notificationService.saveSettings(any(NotificationSettings.class))).thenReturn(settings);

        // When & Then - 모든 파라미터를 전달하지 않으면 모두 false로 설정됨
        mockMvc.perform(post("/trading/notifications/settings")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/notification-settings"));

        verify(notificationService).saveSettings(any(NotificationSettings.class));
    }

    // ========== Helper Methods ==========

    private List<Notification> createNotificationsList() {
        List<Notification> notifications = new ArrayList<>();

        Notification n1 = new Notification();
        n1.setId(1L);
        n1.setType("INFO");
        n1.setTitle("정보 알림");
        n1.setMessage("테스트 메시지 1");
        n1.setReadStatus("N");
        n1.setCreatedAt(LocalDateTime.now().minusHours(1));
        notifications.add(n1);

        Notification n2 = new Notification();
        n2.setId(2L);
        n2.setType("WARNING");
        n2.setTitle("경고 알림");
        n2.setMessage("테스트 메시지 2");
        n2.setReadStatus("N");
        n2.setCreatedAt(LocalDateTime.now().minusMinutes(30));
        notifications.add(n2);

        return notifications;
    }

    private NotificationSettings createNotificationSettings() {
        NotificationSettings settings = new NotificationSettings();
        settings.setId(1L);
        settings.setEnableTradeNotifications(true);
        settings.setEnableStrategyNotifications(true);
        settings.setEnableErrorNotifications(true);
        settings.setEnableWarningNotifications(true);
        settings.setEnableSuccessNotifications(true);
        settings.setEnableInfoNotifications(true);
        settings.setEnableSound(true);
        settings.setEnableBrowserNotification(false);
        return settings;
    }
}
