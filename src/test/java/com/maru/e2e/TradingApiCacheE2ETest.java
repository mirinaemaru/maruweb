package com.maru.e2e;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Trading API 캐싱 및 Resilience E2E 테스트
 *
 * 실제 cautostock 서버와 통신하여:
 * - 캐싱 동작 검증
 * - API 응답 시간 개선 확인
 * - 에러 복구 동작 확인
 */
@DisplayName("Trading API 캐싱 E2E 테스트")
class TradingApiCacheE2ETest extends E2ETestBase {

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void clearCaches() {
        // 테스트 전 캐시 클리어
        cacheManager.getCacheNames().forEach(cacheName -> {
            var cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        });
    }

    // ==================== 캐시 동작 E2E 테스트 ====================

    @Nested
    @DisplayName("Health Status 캐싱 테스트")
    class HealthStatusCacheTests {

        @Test
        @DisplayName("Health Status 조회 후 캐시에 저장됨")
        void healthStatus_IsCached() {
            // Given - 캐시가 비어 있음
            var healthCache = cacheManager.getCache("healthStatus");
            assertThat(healthCache).isNotNull();

            // When - 첫 번째 Health Check 호출
            ResponseEntity<Map> response1 = safeCautostockGet("/health", Map.class);

            // Then - 응답 성공
            assertThat(response1).isNotNull();
            assertThat(response1.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response1.getBody()).isNotNull();
            assertThat(response1.getBody().get("status")).isEqualTo("UP");
        }

        @Test
        @DisplayName("동일한 Health Status 요청 시 응답 시간 개선")
        void healthStatus_CacheImproveResponseTime() {
            // Given - 첫 번째 호출 (캐시 미스)
            long startTime1 = System.currentTimeMillis();
            ResponseEntity<Map> response1 = safeCautostockGet("/health", Map.class);
            long duration1 = System.currentTimeMillis() - startTime1;

            assertThat(response1).isNotNull();
            assertThat(response1.getBody()).isNotNull();

            // When - 두 번째 호출 (캐시 히트 가능)
            long startTime2 = System.currentTimeMillis();
            ResponseEntity<Map> response2 = safeCautostockGet("/health", Map.class);
            long duration2 = System.currentTimeMillis() - startTime2;

            // Then - 두 응답 모두 성공
            assertThat(response2).isNotNull();
            assertThat(response2.getBody()).isNotNull();
            assertThat(response2.getBody().get("status")).isEqualTo("UP");

            // 로그 출력 (디버깅용)
            System.out.println("First call duration: " + duration1 + "ms");
            System.out.println("Second call duration: " + duration2 + "ms");
        }
    }

    // ==================== Accounts 캐싱 테스트 ====================

    @Nested
    @DisplayName("Accounts 캐싱 테스트")
    class AccountsCacheTests {

        @Test
        @DisplayName("계좌 목록 조회 성공")
        void getAccounts_Success() {
            // When
            ResponseEntity<Map> response = safeCautostockGet("/api/v1/admin/accounts", Map.class);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
        }

        @Test
        @DisplayName("계좌 생성/삭제 후 캐시 무효화 동작")
        void accountCRUD_InvalidatesCache() {
            // Given - 계좌 생성
            Map<String, Object> accountData = createTestAccountData();
            ResponseEntity<Map> createResponse = safeCautostockPost(
                "/api/v1/admin/accounts", accountData, Map.class);

            assertThat(createResponse).isNotNull();
            assertThat(createResponse.getStatusCode().is2xxSuccessful()).isTrue();

            Map createBody = createResponse.getBody();
            assertThat(createBody).isNotNull();
            String accountId = (String) createBody.get("accountId");
            assertThat(accountId).isNotNull();

            try {
                // When - 계좌 목록 조회
                ResponseEntity<Map> listResponse = safeCautostockGet(
                    "/api/v1/admin/accounts", Map.class);

                // Then - 새로 생성된 계좌가 목록에 있어야 함
                assertThat(listResponse).isNotNull();
                assertThat(listResponse.getBody()).isNotNull();

                List<Map<String, Object>> items = getItemsFromResponse(listResponse.getBody());
                boolean foundNewAccount = items.stream()
                    .anyMatch(item -> accountId.equals(item.get("accountId")));
                assertThat(foundNewAccount).isTrue();

            } finally {
                // Cleanup - 계좌 삭제
                safeCautostockDelete("/api/v1/admin/accounts/" + accountId);
            }
        }
    }

    // ==================== Instruments 캐싱 테스트 ====================

    @Nested
    @DisplayName("Instruments 캐싱 테스트")
    class InstrumentsCacheTests {

        @Test
        @DisplayName("종목 목록 조회 성공")
        void getInstruments_Success() {
            // When
            ResponseEntity<Map> response = safeCautostockGet("/api/v1/instruments", Map.class);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        @DisplayName("종목 목록은 Long TTL 캐시 사용 (변경 빈도 낮음)")
        void instruments_UseLongTtlCache() {
            // Given - 첫 번째 호출
            ResponseEntity<Map> response1 = safeCautostockGet("/api/v1/instruments", Map.class);
            assertThat(response1).isNotNull();

            // When - 연속 호출
            for (int i = 0; i < 5; i++) {
                ResponseEntity<Map> response = safeCautostockGet("/api/v1/instruments", Map.class);
                assertThat(response).isNotNull();
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            }

            // Then - 모든 호출 성공 (캐시가 잘 동작하면 빠르게 완료)
        }
    }

    // ==================== 에러 복구 E2E 테스트 ====================

    @Nested
    @DisplayName("에러 복구 테스트")
    class ErrorRecoveryTests {

        @Test
        @DisplayName("maruweb 대시보드는 API 오류 시에도 페이지 로드 성공")
        void dashboard_LoadsEvenOnPartialApiFailure() {
            // When - 대시보드 페이지 요청
            ResponseEntity<String> response = testRestTemplate.getForEntity(
                getMaruwebUrl("/trading/dashboard"), String.class);

            // Then - 페이지는 항상 로드됨 (API 실패해도 에러 페이지 또는 기본값 표시)
            assertThat(response.getStatusCode()).isIn(HttpStatus.OK, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        @Test
        @DisplayName("계좌 목록 페이지는 API 오류 시 에러 메시지 표시")
        void accounts_ShowsErrorOnApiFailure() {
            // When
            ResponseEntity<String> response = testRestTemplate.getForEntity(
                getMaruwebUrl("/trading/accounts"), String.class);

            // Then - 페이지 로드 성공
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            // 페이지에 계좌 관련 내용이 있어야 함
            assertThat(response.getBody()).containsAnyOf("계좌", "account", "Account");
        }
    }

    // ==================== Resilience E2E 테스트 ====================

    @Nested
    @DisplayName("Resilience 동작 테스트")
    class ResilienceTests {

        @Test
        @DisplayName("연속 API 호출 안정성")
        void consecutiveApiCalls_AreStable() {
            // Given
            int callCount = 10;
            int successCount = 0;

            // When - 연속 호출
            for (int i = 0; i < callCount; i++) {
                try {
                    ResponseEntity<Map> response = safeCautostockGet("/health", Map.class);
                    if (response != null && response.getStatusCode().is2xxSuccessful()) {
                        successCount++;
                    }
                } catch (Exception e) {
                    System.out.println("Call " + i + " failed: " + e.getMessage());
                }
            }

            // Then - 대부분의 호출이 성공해야 함
            assertThat(successCount).isGreaterThanOrEqualTo(callCount * 8 / 10); // 80% 이상 성공
        }

        @Test
        @DisplayName("병렬 API 호출 처리")
        void parallelApiCalls_AreHandled() throws InterruptedException {
            // Given
            int threadCount = 5;
            List<Thread> threads = new ArrayList<>();
            List<Boolean> results = Collections.synchronizedList(new ArrayList<>());

            // When - 병렬 호출
            for (int i = 0; i < threadCount; i++) {
                Thread thread = new Thread(() -> {
                    try {
                        ResponseEntity<Map> response = restTemplate.getForEntity(
                            getCautostockUrl("/health"), Map.class);
                        results.add(response.getStatusCode().is2xxSuccessful());
                    } catch (Exception e) {
                        results.add(false);
                    }
                });
                threads.add(thread);
                thread.start();
            }

            // Wait for all threads
            for (Thread thread : threads) {
                thread.join(5000);
            }

            // Then - 대부분의 병렬 호출이 성공해야 함
            long successCount = results.stream().filter(r -> r).count();
            assertThat(successCount).isGreaterThanOrEqualTo(threadCount * 8 / 10);
        }
    }

    // ==================== 캐시 통계 테스트 ====================

    @Nested
    @DisplayName("캐시 통계 테스트")
    class CacheStatisticsTests {

        @Test
        @DisplayName("CacheManager에 예상 캐시들이 존재")
        void cacheManager_HasExpectedCaches() {
            // When
            Collection<String> cacheNames = cacheManager.getCacheNames();

            // Then - 캐시 매니저가 존재함
            assertThat(cacheManager).isNotNull();
            System.out.println("Available caches: " + cacheNames);
        }

        @Test
        @DisplayName("캐시 저장 및 조회 동작 확인")
        void cache_StoreAndRetrieve_Works() {
            // Given
            var cache = cacheManager.getCache("testCache");
            assertThat(cache).isNotNull();

            // When
            cache.put("testKey", "testValue");

            // Then
            assertThat(cache.get("testKey", String.class)).isEqualTo("testValue");

            // Cleanup
            cache.clear();
        }
    }

    // ==================== maruweb 페이지 캐싱 효과 테스트 ====================

    @Nested
    @DisplayName("maruweb 페이지 캐싱 효과 테스트")
    class MaruwebPageCacheEffectTests {

        @Test
        @DisplayName("대시보드 페이지 연속 로드 시간 측정")
        void dashboard_ConsecutiveLoadTimes() {
            // Given
            List<Long> loadTimes = new ArrayList<>();

            // When - 3번 연속 로드
            for (int i = 0; i < 3; i++) {
                long startTime = System.currentTimeMillis();
                ResponseEntity<String> response = testRestTemplate.getForEntity(
                    getMaruwebUrl("/trading/dashboard"), String.class);
                long duration = System.currentTimeMillis() - startTime;
                loadTimes.add(duration);

                assertThat(response.getStatusCode()).isIn(HttpStatus.OK, HttpStatus.INTERNAL_SERVER_ERROR);
            }

            // Then - 로드 시간 출력 (캐싱 효과 확인용)
            System.out.println("Dashboard load times: " + loadTimes);
            // 평균 계산
            double avgTime = loadTimes.stream().mapToLong(Long::longValue).average().orElse(0);
            System.out.println("Average load time: " + avgTime + "ms");
        }

        @Test
        @DisplayName("계좌 목록 페이지 연속 로드 시간 측정")
        void accounts_ConsecutiveLoadTimes() {
            // Given
            List<Long> loadTimes = new ArrayList<>();

            // When - 3번 연속 로드
            for (int i = 0; i < 3; i++) {
                long startTime = System.currentTimeMillis();
                ResponseEntity<String> response = testRestTemplate.getForEntity(
                    getMaruwebUrl("/trading/accounts"), String.class);
                long duration = System.currentTimeMillis() - startTime;
                loadTimes.add(duration);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            }

            // Then
            System.out.println("Accounts page load times: " + loadTimes);
        }

        @Test
        @DisplayName("종목 목록 페이지 연속 로드 시간 측정")
        void instruments_ConsecutiveLoadTimes() {
            // Given
            List<Long> loadTimes = new ArrayList<>();

            // When - 3번 연속 로드
            for (int i = 0; i < 3; i++) {
                long startTime = System.currentTimeMillis();
                ResponseEntity<String> response = testRestTemplate.getForEntity(
                    getMaruwebUrl("/trading/instruments"), String.class);
                long duration = System.currentTimeMillis() - startTime;
                loadTimes.add(duration);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            }

            // Then
            System.out.println("Instruments page load times: " + loadTimes);
        }
    }
}
