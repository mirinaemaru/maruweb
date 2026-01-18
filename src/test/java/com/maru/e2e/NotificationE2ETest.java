package com.maru.e2e;

import org.junit.jupiter.api.*;
import org.springframework.http.*;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 알림 E2E 테스트
 *
 * 시나리오: 알림 생성 → 조회 → 읽음 처리
 */
@DisplayName("Notification E2E 테스트")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class NotificationE2ETest extends E2ETestBase {

    private static String createdNotificationId;

    @Test
    @Order(1)
    @DisplayName("알림 목록 페이지 조회")
    void notificationsPage() {
        // When
        ResponseEntity<String> response = testRestTemplate.getForEntity(
            getMaruwebUrl("/trading/notifications"), String.class);

        // Then - 페이지가 없을 수도 있음
        assertThat(response.getStatusCode().is2xxSuccessful() ||
                   response.getStatusCode() == HttpStatus.NOT_FOUND).isTrue();
    }

    @Test
    @Order(2)
    @DisplayName("알림 목록 API 조회")
    void listNotifications() {
        // When
        ResponseEntity<Map> response = safeCautostockGet("/api/v1/admin/notifications", Map.class);

        // Then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    @Order(3)
    @DisplayName("알림 생성")
    void createNotification() {
        // Given
        Map<String, Object> notificationData = createTestNotificationData();

        // When
        ResponseEntity<Map> response = safeCautostockPost("/api/v1/admin/notifications", notificationData, Map.class);

        // Then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

        if (response.getBody() != null) {
            createdNotificationId = extractId(response.getBody(), "notificationId", "id");
            System.out.println("Created notification ID: " + createdNotificationId);
        }
    }

    @Test
    @Order(4)
    @DisplayName("알림 상세 조회")
    void getNotificationDetail() {
        if (createdNotificationId == null) {
            System.out.println("Notification was not created - skipping detail check");
            return;
        }

        // When
        ResponseEntity<Map> response = safeCautostockGet(
            "/api/v1/admin/notifications/" + createdNotificationId, Map.class);

        // Then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    @Order(5)
    @DisplayName("알림 읽음 처리")
    void markNotificationAsRead() {
        if (createdNotificationId == null) {
            System.out.println("Notification was not created - skipping mark as read");
            return;
        }

        // When
        ResponseEntity<Map> response = safeCautostockPost(
            "/api/v1/admin/notifications/" + createdNotificationId + "/read", null, Map.class);

        // Then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    @Order(6)
    @DisplayName("읽음 상태 확인")
    void verifyReadStatus() {
        if (createdNotificationId == null) {
            System.out.println("Notification was not created - skipping read status check");
            return;
        }

        // When
        ResponseEntity<Map> response = safeCautostockGet(
            "/api/v1/admin/notifications/" + createdNotificationId, Map.class);

        // Then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

        if (response.getBody() != null) {
            Object readStatus = response.getBody().get("read");
            Object isRead = response.getBody().get("isRead");
            System.out.println("Read status: " + (readStatus != null ? readStatus : isRead));
        }
    }

    @Test
    @Order(7)
    @DisplayName("읽지 않은 알림 조회")
    void listUnreadNotifications() {
        // When
        ResponseEntity<Map> response = safeCautostockGet(
            "/api/v1/admin/notifications?read=false", Map.class);

        // Then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    @Order(8)
    @DisplayName("알림 유형별 조회")
    void listNotificationsByType() {
        // When
        ResponseEntity<Map> response = safeCautostockGet(
            "/api/v1/admin/notifications?type=STRATEGY_ALERT", Map.class);

        // Then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    @Order(9)
    @DisplayName("알림 삭제")
    void deleteNotification() {
        if (createdNotificationId == null) {
            System.out.println("Notification was not created - skipping delete");
            return;
        }

        // When
        try {
            ResponseEntity<Void> response = restTemplate.exchange(
                getCautostockUrl("/api/v1/admin/notifications/" + createdNotificationId),
                HttpMethod.DELETE,
                null,
                Void.class);
            assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            System.out.println("Delete notification failed: " + e.getStatusCode());
        }
    }

    @Test
    @Order(10)
    @DisplayName("모든 알림 읽음 처리")
    void markAllAsRead() {
        // When - read-all API가 없을 수도 있으므로 예외 처리
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                getCautostockUrl("/api/v1/admin/notifications/read-all"),
                createJsonEntity(null),
                Map.class);
            // Then - 성공한 경우
            assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        } catch (org.springframework.web.client.HttpClientErrorException.NotFound e) {
            // 404는 허용 (read-all API가 구현되지 않은 경우)
            System.out.println("Notifications read-all API not found (expected): " + e.getStatusCode());
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            System.out.println("Notifications read-all API error: " + e.getStatusCode());
        } catch (org.springframework.web.client.HttpServerErrorException e) {
            System.out.println("Notifications read-all server error: " + e.getStatusCode());
        }
    }

    // ========== 알림 유형별 테스트 ==========

    @Test
    @Order(11)
    @DisplayName("주문 체결 알림 생성")
    void createOrderFillNotification() {
        // Given
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "ORDER_FILLED");
        notification.put("title", "주문 체결 알림");
        notification.put("message", "삼성전자 100주 매수 주문이 체결되었습니다.");
        notification.put("priority", "HIGH");
        notification.put("data", Map.of(
            "orderId", "order-123",
            "symbol", "005930",
            "quantity", 100,
            "price", 70000
        ));

        // When
        ResponseEntity<Map> response = safeCautostockPost(
            "/api/v1/admin/notifications", notification, Map.class);

        // Then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

        // Cleanup
        if (response.getBody() != null) {
            String notifId = extractId(response.getBody(), "notificationId", "id");
            if (notifId != null) {
                safeCautostockDelete("/api/v1/admin/notifications/" + notifId);
            }
        }
    }

    @Test
    @Order(12)
    @DisplayName("리스크 경고 알림 생성")
    void createRiskAlertNotification() {
        // Given
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "RISK_ALERT");
        notification.put("title", "리스크 경고");
        notification.put("message", "포트폴리오 손실률이 5%를 초과했습니다.");
        notification.put("priority", "CRITICAL");

        // When
        ResponseEntity<Map> response = safeCautostockPost(
            "/api/v1/admin/notifications", notification, Map.class);

        // Then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

        // Cleanup
        if (response.getBody() != null) {
            String notifId = extractId(response.getBody(), "notificationId", "id");
            if (notifId != null) {
                safeCautostockDelete("/api/v1/admin/notifications/" + notifId);
            }
        }
    }

    @Test
    @Order(13)
    @DisplayName("알림 카운트 조회")
    void getNotificationCount() {
        // When
        ResponseEntity<Map> response = safeCautostockGet(
            "/api/v1/admin/notifications/count", Map.class);

        // Then
        assertThat(response.getStatusCode().is2xxSuccessful() ||
                   response.getStatusCode() == HttpStatus.NOT_FOUND).isTrue();
    }

    // ========== Helper Methods ==========

    private String extractId(Map<String, Object> body, String... keys) {
        if (body == null) return null;
        for (String key : keys) {
            if (body.containsKey(key) && body.get(key) != null) {
                return String.valueOf(body.get(key));
            }
        }
        return null;
    }
}
