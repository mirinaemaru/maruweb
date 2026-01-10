package com.maru.trading.controller;

import com.maru.trading.entity.Notification;
import com.maru.trading.entity.NotificationSettings;
import com.maru.trading.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest
@ContextConfiguration(classes = TestConfig.class)
@Import(NotificationController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("NotificationController 단위 테스트")
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationService notificationService;

    @Test
    @DisplayName("실시간 알림 테스트 페이지")
    void testPage() throws Exception {
        mockMvc.perform(get("/trading/notifications/test"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/notification-test"));
    }

    @Test
    @DisplayName("알림 히스토리 페이지 - 성공")
    void history_Success() throws Exception {
        Notification n1 = new Notification();
        n1.setId(1L);
        n1.setType("TRADE");
        n1.setTitle("테스트 알림");

        when(notificationService.getAllNotifications()).thenReturn(Arrays.asList(n1));
        when(notificationService.getUnreadCount()).thenReturn(1L);

        mockMvc.perform(get("/trading/notifications/history"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/notification-history"))
                .andExpect(model().attributeExists("notifications"))
                .andExpect(model().attributeExists("unreadCount"));
    }

    @Test
    @DisplayName("알림 히스토리 - 타입별 필터")
    void history_FilterByType() throws Exception {
        Notification n1 = new Notification();
        n1.setId(1L);
        n1.setType("TRADE");

        when(notificationService.getNotificationsByType("TRADE")).thenReturn(Arrays.asList(n1));
        when(notificationService.getUnreadCount()).thenReturn(0L);

        mockMvc.perform(get("/trading/notifications/history")
                        .param("type", "TRADE"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/notification-history"))
                .andExpect(model().attributeExists("notifications"));
    }

    @Test
    @DisplayName("알림 설정 페이지")
    void settings_Success() throws Exception {
        NotificationSettings settings = new NotificationSettings();
        settings.setUserId("default");
        settings.setEnableTradeNotifications(true);

        when(notificationService.getSettings()).thenReturn(settings);

        mockMvc.perform(get("/trading/notifications/settings"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/notification-settings"))
                .andExpect(model().attributeExists("settings"));
    }

    @Test
    @DisplayName("알림 설정 저장 - 성공")
    void saveSettings_Success() throws Exception {
        NotificationSettings settings = new NotificationSettings();
        settings.setUserId("default");
        settings.setEnableTradeNotifications(true);

        when(notificationService.getSettings()).thenReturn(settings);
        when(notificationService.saveSettings(any())).thenReturn(settings);

        mockMvc.perform(post("/trading/notifications/settings")
                        .param("enableTradeNotifications", "true")
                        .param("enableStrategyNotifications", "true")
                        .param("enableErrorNotifications", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("trading/notification-settings"))
                .andExpect(model().attributeExists("settings"))
                .andExpect(model().attributeExists("success"));
    }

    @Test
    @DisplayName("알림 읽음 처리 - API")
    void markAsRead_Success() throws Exception {
        doNothing().when(notificationService).markAsRead(anyLong());

        mockMvc.perform(post("/api/notifications/1/read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").exists());

        verify(notificationService).markAsRead(1L);
    }

    @Test
    @DisplayName("모든 알림 읽음 처리 - API")
    void markAllAsRead_Success() throws Exception {
        doNothing().when(notificationService).markAllAsRead();

        mockMvc.perform(post("/api/notifications/read-all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").exists());

        verify(notificationService).markAllAsRead();
    }

    @Test
    @DisplayName("알림 삭제 - API")
    void deleteNotification_Success() throws Exception {
        doNothing().when(notificationService).deleteNotification(anyLong());

        mockMvc.perform(post("/api/notifications/1/delete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").exists());

        verify(notificationService).deleteNotification(1L);
    }

    @Test
    @DisplayName("읽지 않은 알림 개수 조회 - API")
    void getUnreadCount_Success() throws Exception {
        when(notificationService.getUnreadCount()).thenReturn(5L);

        mockMvc.perform(get("/api/notifications/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(5));
    }
}
