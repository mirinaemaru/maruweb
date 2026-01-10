package com.maru.trading.service;

import com.maru.trading.entity.Notification;
import com.maru.trading.entity.NotificationSettings;
import com.maru.trading.repository.NotificationRepository;
import com.maru.trading.repository.NotificationSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService 단위 테스트")
class NotificationServiceTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationSettingsRepository settingsRepository;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(
                messagingTemplate,
                notificationRepository,
                settingsRepository
        );
    }

    private NotificationSettings createEnabledSettings() {
        NotificationSettings settings = new NotificationSettings();
        settings.setUserId("default");
        settings.setEnableTradeNotifications(true);
        settings.setEnableStrategyNotifications(true);
        settings.setEnableErrorNotifications(true);
        settings.setEnableWarningNotifications(true);
        settings.setEnableSuccessNotifications(true);
        settings.setEnableInfoNotifications(true);
        return settings;
    }

    // ==================== sendNotificationToAll Tests ====================

    @Test
    @DisplayName("전체 알림 전송 - 성공 (알림 활성화)")
    void sendNotificationToAll_Enabled_Success() {
        // given
        NotificationSettings settings = createEnabledSettings();
        when(settingsRepository.findByUserId("default")).thenReturn(Optional.of(settings));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        // when
        notificationService.sendNotificationToAll("TRADE", "테스트 제목", "테스트 메시지");

        // then
        verify(notificationRepository).save(any(Notification.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/notifications"), any(Map.class));
    }

    @Test
    @DisplayName("전체 알림 전송 - 비활성화된 알림 타입은 전송하지 않음")
    void sendNotificationToAll_Disabled_DoesNotSend() {
        // given
        NotificationSettings settings = createEnabledSettings();
        settings.setEnableTradeNotifications(false);
        when(settingsRepository.findByUserId("default")).thenReturn(Optional.of(settings));

        // when
        notificationService.sendNotificationToAll("TRADE", "테스트 제목", "테스트 메시지");

        // then
        verify(notificationRepository, never()).save(any(Notification.class));
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Map.class));
    }

    // ==================== notifyTradeExecution Tests ====================

    @Test
    @DisplayName("거래 체결 알림 - 매수")
    void notifyTradeExecution_Buy() {
        // given
        NotificationSettings settings = createEnabledSettings();
        when(settingsRepository.findByUserId("default")).thenReturn(Optional.of(settings));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        // when
        notificationService.notifyTradeExecution("005930", "BUY", 100, 70000);

        // then
        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());

        Notification savedNotification = notificationCaptor.getValue();
        assertThat(savedNotification.getType()).isEqualTo("TRADE");
        assertThat(savedNotification.getTitle()).contains("005930");
        assertThat(savedNotification.getTitle()).contains("매수");
    }

    @Test
    @DisplayName("거래 체결 알림 - 매도")
    void notifyTradeExecution_Sell() {
        // given
        NotificationSettings settings = createEnabledSettings();
        when(settingsRepository.findByUserId("default")).thenReturn(Optional.of(settings));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        // when
        notificationService.notifyTradeExecution("005930", "SELL", 50, 75000);

        // then
        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());

        Notification savedNotification = notificationCaptor.getValue();
        assertThat(savedNotification.getTitle()).contains("매도");
    }

    // ==================== notifyStrategyStatusChange Tests ====================

    @Test
    @DisplayName("전략 상태 변경 알림")
    void notifyStrategyStatusChange_Success() {
        // given
        NotificationSettings settings = createEnabledSettings();
        when(settingsRepository.findByUserId("default")).thenReturn(Optional.of(settings));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        // when
        notificationService.notifyStrategyStatusChange("MA Crossover", "INACTIVE", "ACTIVE");

        // then
        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());

        Notification savedNotification = notificationCaptor.getValue();
        assertThat(savedNotification.getType()).isEqualTo("STRATEGY");
        assertThat(savedNotification.getMessage()).contains("MA Crossover");
        assertThat(savedNotification.getMessage()).contains("INACTIVE → ACTIVE");
    }

    // ==================== notifyBalanceUpdate Tests ====================

    @Test
    @DisplayName("잔고 변동 알림 - 증가 (SUCCESS)")
    void notifyBalanceUpdate_Increase() {
        // given
        NotificationSettings settings = createEnabledSettings();
        when(settingsRepository.findByUserId("default")).thenReturn(Optional.of(settings));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        // when
        notificationService.notifyBalanceUpdate("test-account", 10500000, 500000);

        // then
        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());

        Notification savedNotification = notificationCaptor.getValue();
        assertThat(savedNotification.getType()).isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("잔고 변동 알림 - 감소 (WARNING)")
    void notifyBalanceUpdate_Decrease() {
        // given
        NotificationSettings settings = createEnabledSettings();
        when(settingsRepository.findByUserId("default")).thenReturn(Optional.of(settings));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        // when
        notificationService.notifyBalanceUpdate("test-account", 9500000, -500000);

        // then
        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());

        Notification savedNotification = notificationCaptor.getValue();
        assertThat(savedNotification.getType()).isEqualTo("WARNING");
    }

    // ==================== notifyError Tests ====================

    @Test
    @DisplayName("에러 알림")
    void notifyError_Success() {
        // given
        NotificationSettings settings = createEnabledSettings();
        when(settingsRepository.findByUserId("default")).thenReturn(Optional.of(settings));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        // when
        notificationService.notifyError("주문 실패", "서버 연결 오류");

        // then
        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());

        Notification savedNotification = notificationCaptor.getValue();
        assertThat(savedNotification.getType()).isEqualTo("ERROR");
        assertThat(savedNotification.getTitle()).isEqualTo("주문 실패");
    }

    // ==================== Notification Query Tests ====================

    @Test
    @DisplayName("전체 알림 목록 조회")
    void getAllNotifications_Success() {
        // given
        Notification n1 = new Notification();
        n1.setId(1L);
        n1.setType("TRADE");

        Notification n2 = new Notification();
        n2.setId(2L);
        n2.setType("ERROR");

        when(notificationRepository.findByDeletedOrderByCreatedAtDesc("N"))
                .thenReturn(Arrays.asList(n1, n2));

        // when
        List<Notification> result = notificationService.getAllNotifications();

        // then
        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("타입별 알림 조회")
    void getNotificationsByType_Success() {
        // given
        Notification n1 = new Notification();
        n1.setId(1L);
        n1.setType("TRADE");

        when(notificationRepository.findByTypeAndDeletedOrderByCreatedAtDesc("TRADE", "N"))
                .thenReturn(Arrays.asList(n1));

        // when
        List<Notification> result = notificationService.getNotificationsByType("TRADE");

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getType()).isEqualTo("TRADE");
    }

    @Test
    @DisplayName("읽지 않은 알림 조회")
    void getUnreadNotifications_Success() {
        // given
        Notification n1 = new Notification();
        n1.setId(1L);
        n1.setReadStatus("N");

        when(notificationRepository.findByReadStatusAndDeletedOrderByCreatedAtDesc("N", "N"))
                .thenReturn(Arrays.asList(n1));

        // when
        List<Notification> result = notificationService.getUnreadNotifications();

        // then
        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("읽지 않은 알림 개수 조회")
    void getUnreadCount_Success() {
        // given
        when(notificationRepository.countByReadStatusAndDeleted("N", "N")).thenReturn(5L);

        // when
        long count = notificationService.getUnreadCount();

        // then
        assertThat(count).isEqualTo(5L);
    }

    // ==================== markAsRead Tests ====================

    @Test
    @DisplayName("알림 읽음 처리")
    void markAsRead_Success() {
        // given
        Notification notification = new Notification();
        notification.setId(1L);
        notification.setReadStatus("N");

        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        // when
        notificationService.markAsRead(1L);

        // then
        verify(notificationRepository).save(notification);
        assertThat(notification.getReadStatus()).isEqualTo("Y");
    }

    @Test
    @DisplayName("알림 읽음 처리 - 존재하지 않는 알림")
    void markAsRead_NotFound() {
        // given
        when(notificationRepository.findById(999L)).thenReturn(Optional.empty());

        // when
        notificationService.markAsRead(999L);

        // then
        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("모든 알림 읽음 처리")
    void markAllAsRead_Success() {
        // given
        Notification n1 = new Notification();
        n1.setId(1L);
        n1.setReadStatus("N");

        Notification n2 = new Notification();
        n2.setId(2L);
        n2.setReadStatus("N");

        when(notificationRepository.findByReadStatusAndDeletedOrderByCreatedAtDesc("N", "N"))
                .thenReturn(Arrays.asList(n1, n2));

        // when
        notificationService.markAllAsRead();

        // then
        verify(notificationRepository).saveAll(any());
        assertThat(n1.getReadStatus()).isEqualTo("Y");
        assertThat(n2.getReadStatus()).isEqualTo("Y");
    }

    // ==================== deleteNotification Tests ====================

    @Test
    @DisplayName("알림 삭제 (소프트 삭제)")
    void deleteNotification_Success() {
        // given
        Notification notification = new Notification();
        notification.setId(1L);
        notification.setDeleted("N");

        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        // when
        notificationService.deleteNotification(1L);

        // then
        verify(notificationRepository).save(notification);
        assertThat(notification.getDeleted()).isEqualTo("Y");
    }

    // ==================== Settings Tests ====================

    @Test
    @DisplayName("알림 설정 조회 - 기존 설정 반환")
    void getSettings_ExistingSettings() {
        // given
        NotificationSettings settings = createEnabledSettings();
        when(settingsRepository.findByUserId("default")).thenReturn(Optional.of(settings));

        // when
        NotificationSettings result = notificationService.getSettings();

        // then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo("default");
        verify(settingsRepository, never()).save(any());
    }

    @Test
    @DisplayName("알림 설정 조회 - 없으면 기본값 생성")
    void getSettings_CreateDefault() {
        // given
        when(settingsRepository.findByUserId("default")).thenReturn(Optional.empty());
        when(settingsRepository.save(any(NotificationSettings.class))).thenAnswer(inv -> inv.getArgument(0));

        // when
        NotificationSettings result = notificationService.getSettings();

        // then
        assertThat(result).isNotNull();
        verify(settingsRepository).save(any(NotificationSettings.class));
    }

    @Test
    @DisplayName("알림 설정 저장")
    void saveSettings_Success() {
        // given
        NotificationSettings settings = new NotificationSettings();
        settings.setEnableTradeNotifications(false);

        when(settingsRepository.save(any(NotificationSettings.class))).thenAnswer(inv -> inv.getArgument(0));

        // when
        NotificationSettings result = notificationService.saveSettings(settings);

        // then
        assertThat(result.getUserId()).isEqualTo("default");
        verify(settingsRepository).save(settings);
    }
}
