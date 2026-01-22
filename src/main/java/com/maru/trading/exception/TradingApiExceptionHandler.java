package com.maru.trading.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;

/**
 * Trading API 예외를 처리하는 글로벌 예외 핸들러
 */
@Slf4j
@ControllerAdvice(basePackages = "com.maru.trading")
public class TradingApiExceptionHandler {

    /**
     * TradingApiException 처리 - JSON 응답
     */
    @ExceptionHandler(TradingApiException.class)
    public ResponseEntity<Map<String, Object>> handleTradingApiException(TradingApiException ex) {
        log.error("Trading API Error: [{}] {}", ex.getErrorCode(), ex.getMessage(), ex);

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("errorCode", ex.getErrorCode().name());
        errorResponse.put("message", ex.getMessage());
        if (ex.getDetail() != null) {
            errorResponse.put("detail", ex.getDetail());
        }

        return ResponseEntity.status(ex.getHttpStatus()).body(errorResponse);
    }

    /**
     * 에러 메시지를 RedirectAttributes에 추가 (페이지 리다이렉트 시 사용)
     */
    public static void addErrorToRedirect(RedirectAttributes redirectAttributes, TradingApiException ex) {
        redirectAttributes.addFlashAttribute("error", ex.getMessage());
        redirectAttributes.addFlashAttribute("errorCode", ex.getErrorCode().name());
    }
}
