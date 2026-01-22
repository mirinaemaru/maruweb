package com.maru.trading.service;

import com.maru.trading.exception.TradingApiException;
import com.maru.trading.exception.TradingApiException.ErrorCode;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.ConnectException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Trading API 호출 헬퍼 클래스
 * - Retry/Circuit Breaker 적용
 * - 에러 처리 표준화
 * - 공통 API 호출 패턴 제공
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TradingApiHelper {

    private final RestTemplate tradingApiRestTemplate;
    private final Retry tradingApiRetry;
    private final CircuitBreaker tradingApiCircuitBreaker;

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE_REF =
            new ParameterizedTypeReference<Map<String, Object>>() {};

    /**
     * GET 요청 실행 (재시도 적용)
     */
    public Map<String, Object> get(String url, ErrorCode errorCode) {
        return executeWithResilience(() -> doGet(url), errorCode);
    }

    /**
     * GET 요청 실행 (재시도 적용, 기본값 반환)
     */
    public Map<String, Object> getWithDefault(String url, Map<String, Object> defaultValue) {
        try {
            return executeWithResilience(() -> doGet(url), null);
        } catch (Exception e) {
            log.warn("GET request failed, returning default value: {}", e.getMessage());
            return defaultValue;
        }
    }

    /**
     * POST 요청 실행 (재시도 적용)
     */
    public Map<String, Object> post(String url, Map<String, Object> body, ErrorCode errorCode) {
        return executeWithResilience(() -> doPost(url, body), errorCode);
    }

    /**
     * POST 요청 실행 (재시도 적용, 기본값 반환)
     */
    public Map<String, Object> postWithDefault(String url, Map<String, Object> body, Map<String, Object> defaultValue) {
        try {
            return executeWithResilience(() -> doPost(url, body), null);
        } catch (Exception e) {
            log.warn("POST request failed, returning default value: {}", e.getMessage());
            return defaultValue;
        }
    }

    /**
     * PUT 요청 실행 (재시도 적용)
     */
    public Map<String, Object> put(String url, Map<String, Object> body, ErrorCode errorCode) {
        return executeWithResilience(() -> doPut(url, body), errorCode);
    }

    /**
     * PATCH 요청 실행 (재시도 적용)
     */
    public Map<String, Object> patch(String url, Map<String, Object> body, ErrorCode errorCode) {
        return executeWithResilience(() -> doPatch(url, body), errorCode);
    }

    /**
     * DELETE 요청 실행 (재시도 적용)
     */
    public void delete(String url, ErrorCode errorCode) {
        executeWithResilience(() -> {
            doDelete(url);
            return null;
        }, errorCode);
    }

    /**
     * Resilience4j 적용 실행
     */
    private <T> T executeWithResilience(Supplier<T> supplier, ErrorCode errorCode) {
        Supplier<T> decoratedSupplier = Retry.decorateSupplier(tradingApiRetry,
                CircuitBreaker.decorateSupplier(tradingApiCircuitBreaker, supplier));

        try {
            return decoratedSupplier.get();
        } catch (Exception e) {
            throw translateException(e, errorCode);
        }
    }

    /**
     * 실제 GET 요청 수행
     */
    private Map<String, Object> doGet(String url) {
        log.debug("Trading API GET: {}", url);
        ResponseEntity<Map<String, Object>> response = tradingApiRestTemplate.exchange(
                url, HttpMethod.GET, null, MAP_TYPE_REF);
        return response.getBody();
    }

    /**
     * 실제 POST 요청 수행
     */
    private Map<String, Object> doPost(String url, Map<String, Object> body) {
        log.debug("Trading API POST: {}", url);
        HttpEntity<Map<String, Object>> request = createJsonRequest(body);
        ResponseEntity<Map<String, Object>> response = tradingApiRestTemplate.exchange(
                url, HttpMethod.POST, request, MAP_TYPE_REF);
        return response.getBody();
    }

    /**
     * 실제 PUT 요청 수행
     */
    private Map<String, Object> doPut(String url, Map<String, Object> body) {
        log.debug("Trading API PUT: {}", url);
        HttpEntity<Map<String, Object>> request = createJsonRequest(body);
        ResponseEntity<Map<String, Object>> response = tradingApiRestTemplate.exchange(
                url, HttpMethod.PUT, request, MAP_TYPE_REF);
        return response.getBody();
    }

    /**
     * 실제 PATCH 요청 수행
     */
    private Map<String, Object> doPatch(String url, Map<String, Object> body) {
        log.debug("Trading API PATCH: {}", url);
        HttpEntity<Map<String, Object>> request = createJsonRequest(body);
        ResponseEntity<Map<String, Object>> response = tradingApiRestTemplate.exchange(
                url, HttpMethod.PATCH, request, MAP_TYPE_REF);
        return response.getBody();
    }

    /**
     * 실제 DELETE 요청 수행
     */
    private void doDelete(String url) {
        log.debug("Trading API DELETE: {}", url);
        tradingApiRestTemplate.exchange(url, HttpMethod.DELETE, null, MAP_TYPE_REF);
    }

    /**
     * JSON Request Entity 생성
     */
    private HttpEntity<Map<String, Object>> createJsonRequest(Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    /**
     * 예외 변환
     */
    private TradingApiException translateException(Exception e, ErrorCode errorCode) {
        // 이미 TradingApiException인 경우 그대로 반환
        if (e instanceof TradingApiException) {
            return (TradingApiException) e;
        }

        // HttpClientErrorException (4xx)
        if (e instanceof HttpClientErrorException) {
            HttpClientErrorException httpEx = (HttpClientErrorException) e;
            HttpStatus status = httpEx.getStatusCode();
            String detail = extractErrorMessage(httpEx.getResponseBodyAsString());

            if (status == HttpStatus.NOT_FOUND) {
                return new TradingApiException(ErrorCode.NOT_FOUND, detail, e);
            } else if (status == HttpStatus.BAD_REQUEST) {
                return new TradingApiException(ErrorCode.BAD_REQUEST, detail, e);
            } else if (status == HttpStatus.UNAUTHORIZED) {
                return new TradingApiException(ErrorCode.UNAUTHORIZED, detail, e);
            } else if (status == HttpStatus.FORBIDDEN) {
                return new TradingApiException(ErrorCode.FORBIDDEN, detail, e);
            } else if (status == HttpStatus.CONFLICT) {
                return new TradingApiException(ErrorCode.CONFLICT, detail, e);
            }
            return new TradingApiException(errorCode != null ? errorCode : ErrorCode.BAD_REQUEST, detail, e);
        }

        // HttpServerErrorException (5xx)
        if (e instanceof HttpServerErrorException) {
            HttpServerErrorException httpEx = (HttpServerErrorException) e;
            String detail = extractErrorMessage(httpEx.getResponseBodyAsString());
            return new TradingApiException(ErrorCode.INTERNAL_ERROR, detail, e);
        }

        // ResourceAccessException (연결 실패)
        if (e instanceof ResourceAccessException) {
            Throwable cause = e.getCause();
            if (cause instanceof ConnectException) {
                return new TradingApiException(ErrorCode.CONNECTION_FAILED, e);
            }
            return new TradingApiException(ErrorCode.SERVICE_UNAVAILABLE, e);
        }

        // RestClientException (기타)
        if (e instanceof RestClientException) {
            return new TradingApiException(
                    errorCode != null ? errorCode : ErrorCode.UNKNOWN_ERROR,
                    e.getMessage(), e);
        }

        // 기타 예외
        return new TradingApiException(
                errorCode != null ? errorCode : ErrorCode.UNKNOWN_ERROR,
                e.getMessage(), e);
    }

    /**
     * API 응답에서 에러 메시지 추출
     */
    private String extractErrorMessage(String responseBody) {
        if (responseBody == null || responseBody.isEmpty()) {
            return null;
        }
        try {
            if (responseBody.contains("\"message\"")) {
                int start = responseBody.indexOf("\"message\":\"") + 11;
                int end = responseBody.indexOf("\"", start);
                if (start > 10 && end > start) {
                    return responseBody.substring(start, end);
                }
            }
            if (responseBody.contains("\"detail\"")) {
                int start = responseBody.indexOf("\"detail\":\"") + 10;
                int end = responseBody.indexOf("\"", start);
                if (start > 9 && end > start) {
                    return responseBody.substring(start, end);
                }
            }
        } catch (Exception e) {
            log.debug("Failed to parse error response: {}", responseBody);
        }
        return responseBody.length() > 100 ? responseBody.substring(0, 100) : responseBody;
    }

    // ==================== URL 빌더 유틸리티 ====================

    /**
     * 쿼리 파라미터를 포함한 URL 빌드
     */
    public String buildUrl(String basePath, Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return basePath;
        }

        StringBuilder url = new StringBuilder(basePath);
        url.append("?");

        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                if (!first) {
                    url.append("&");
                }
                url.append(entry.getKey()).append("=").append(entry.getValue());
                first = false;
            }
        }

        // 끝에 ? 또는 &만 있으면 제거
        String result = url.toString();
        return result.replaceAll("[&?]$", "");
    }

    /**
     * 빈 Map 생성 헬퍼
     */
    public Map<String, Object> emptyMap() {
        return new HashMap<>();
    }

    /**
     * 기본 에러 응답 생성
     */
    public Map<String, Object> createErrorResponse(String error) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", error);
        errorResponse.put("success", false);
        return errorResponse;
    }
}
