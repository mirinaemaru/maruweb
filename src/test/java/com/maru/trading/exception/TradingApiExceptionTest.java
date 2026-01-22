package com.maru.trading.exception;

import com.maru.trading.exception.TradingApiException.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TradingApiException 테스트
 */
@DisplayName("TradingApiException 테스트")
class TradingApiExceptionTest {

    @Test
    @DisplayName("기본 생성자로 예외 생성")
    void createExceptionWithErrorCode() {
        // when
        TradingApiException exception = new TradingApiException(ErrorCode.CONNECTION_FAILED);

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CONNECTION_FAILED);
        assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(exception.getMessage()).isEqualTo("Trading System에 연결할 수 없습니다");
        assertThat(exception.getDetail()).isNull();
    }

    @Test
    @DisplayName("상세 메시지와 함께 예외 생성")
    void createExceptionWithDetail() {
        // given
        String detail = "Connection refused: localhost:8099";

        // when
        TradingApiException exception = new TradingApiException(ErrorCode.CONNECTION_FAILED, detail);

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CONNECTION_FAILED);
        assertThat(exception.getMessage()).contains(detail);
        assertThat(exception.getDetail()).isEqualTo(detail);
    }

    @Test
    @DisplayName("원인 예외와 함께 예외 생성")
    void createExceptionWithCause() {
        // given
        RuntimeException cause = new RuntimeException("Original error");

        // when
        TradingApiException exception = new TradingApiException(ErrorCode.INTERNAL_ERROR, cause);

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INTERNAL_ERROR);
        assertThat(exception.getCause()).isEqualTo(cause);
        assertThat(exception.getDetail()).isEqualTo("Original error");
    }

    @Test
    @DisplayName("상세 메시지와 원인 예외 모두 포함하여 예외 생성")
    void createExceptionWithDetailAndCause() {
        // given
        String detail = "Failed to create strategy";
        RuntimeException cause = new RuntimeException("Database error");

        // when
        TradingApiException exception = new TradingApiException(ErrorCode.STRATEGY_CREATE_FAILED, detail, cause);

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.STRATEGY_CREATE_FAILED);
        assertThat(exception.getMessage()).contains(detail);
        assertThat(exception.getCause()).isEqualTo(cause);
        assertThat(exception.getDetail()).isEqualTo(detail);
    }

    @Test
    @DisplayName("ErrorCode별 HttpStatus 매핑 확인")
    void errorCodeHttpStatusMapping() {
        // Client errors (4xx)
        assertThat(ErrorCode.BAD_REQUEST.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ErrorCode.UNAUTHORIZED.getHttpStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(ErrorCode.FORBIDDEN.getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(ErrorCode.NOT_FOUND.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ErrorCode.CONFLICT.getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);

        // Server errors (5xx)
        assertThat(ErrorCode.INTERNAL_ERROR.getHttpStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(ErrorCode.CONNECTION_FAILED.getHttpStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(ErrorCode.TIMEOUT.getHttpStatus()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
        assertThat(ErrorCode.SERVICE_UNAVAILABLE.getHttpStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    @DisplayName("비즈니스 에러 코드 메시지 확인")
    void businessErrorCodeMessages() {
        assertThat(ErrorCode.ACCOUNT_NOT_FOUND.getMessage()).contains("계좌");
        assertThat(ErrorCode.STRATEGY_NOT_FOUND.getMessage()).contains("전략");
        assertThat(ErrorCode.ORDER_NOT_FOUND.getMessage()).contains("주문");
        assertThat(ErrorCode.POSITION_NOT_FOUND.getMessage()).contains("포지션");
        assertThat(ErrorCode.BACKTEST_FAILED.getMessage()).contains("백테스트");
        assertThat(ErrorCode.KILL_SWITCH_FAILED.getMessage()).contains("Kill Switch");
    }
}
