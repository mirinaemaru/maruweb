package com.maru.trading.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Trading System API 호출 시 발생하는 예외를 처리하는 커스텀 예외 클래스
 */
@Getter
public class TradingApiException extends RuntimeException {

    private final ErrorCode errorCode;
    private final HttpStatus httpStatus;
    private final String detail;

    public TradingApiException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.httpStatus = errorCode.getHttpStatus();
        this.detail = null;
    }

    public TradingApiException(ErrorCode errorCode, String detail) {
        super(errorCode.getMessage() + (detail != null ? ": " + detail : ""));
        this.errorCode = errorCode;
        this.httpStatus = errorCode.getHttpStatus();
        this.detail = detail;
    }

    public TradingApiException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
        this.httpStatus = errorCode.getHttpStatus();
        this.detail = cause != null ? cause.getMessage() : null;
    }

    public TradingApiException(ErrorCode errorCode, String detail, Throwable cause) {
        super(errorCode.getMessage() + (detail != null ? ": " + detail : ""), cause);
        this.errorCode = errorCode;
        this.httpStatus = errorCode.getHttpStatus();
        this.detail = detail;
    }

    /**
     * Trading API 에러 코드 정의
     */
    @Getter
    public enum ErrorCode {
        // 연결 에러 (5xx)
        CONNECTION_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "Trading System에 연결할 수 없습니다"),
        TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "Trading System 응답 시간이 초과되었습니다"),
        SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "Trading System이 일시적으로 사용 불가합니다"),

        // 클라이언트 에러 (4xx)
        BAD_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다"),
        UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다"),
        FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다"),
        NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다"),
        CONFLICT(HttpStatus.CONFLICT, "리소스 충돌이 발생했습니다"),

        // 서버 에러 (5xx)
        INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Trading System 내부 오류가 발생했습니다"),

        // 비즈니스 에러
        ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "계좌를 찾을 수 없습니다"),
        ACCOUNT_CREATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "계좌 등록에 실패했습니다"),
        ACCOUNT_UPDATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "계좌 수정에 실패했습니다"),
        ACCOUNT_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "계좌 삭제에 실패했습니다"),
        ACCOUNT_PERMISSION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "계좌 권한 업데이트에 실패했습니다"),

        STRATEGY_NOT_FOUND(HttpStatus.NOT_FOUND, "전략을 찾을 수 없습니다"),
        STRATEGY_CREATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "전략 등록에 실패했습니다"),
        STRATEGY_UPDATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "전략 수정에 실패했습니다"),
        STRATEGY_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "전략 삭제에 실패했습니다"),
        STRATEGY_STATUS_UPDATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "전략 상태 변경에 실패했습니다"),
        STRATEGY_EXECUTE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "전략 수동 실행에 실패했습니다"),

        ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "주문을 찾을 수 없습니다"),
        ORDER_CANCEL_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "주문 취소에 실패했습니다"),
        ORDER_MODIFY_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "주문 수정에 실패했습니다"),

        POSITION_NOT_FOUND(HttpStatus.NOT_FOUND, "포지션을 찾을 수 없습니다"),
        FILL_NOT_FOUND(HttpStatus.NOT_FOUND, "체결 정보를 찾을 수 없습니다"),
        BALANCE_FETCH_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "계좌 잔고를 가져올 수 없습니다"),

        KILL_SWITCH_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "Kill Switch 변경에 실패했습니다"),
        RISK_RULE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "리스크 룰 업데이트에 실패했습니다"),

        BACKTEST_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "백테스트 실행에 실패했습니다"),
        BACKTEST_NOT_FOUND(HttpStatus.NOT_FOUND, "백테스트 결과를 찾을 수 없습니다"),

        OPTIMIZATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "최적화 실행에 실패했습니다"),
        SIMULATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "시뮬레이션 실행에 실패했습니다"),

        DEMO_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "데모 실행에 실패했습니다"),

        INSTRUMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "종목을 찾을 수 없습니다"),
        INSTRUMENT_UPDATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "종목 상태 업데이트에 실패했습니다"),

        PERFORMANCE_FETCH_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "성과 데이터를 가져올 수 없습니다"),

        UNKNOWN_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "알 수 없는 오류가 발생했습니다");

        private final HttpStatus httpStatus;
        private final String message;

        ErrorCode(HttpStatus httpStatus, String message) {
            this.httpStatus = httpStatus;
            this.message = message;
        }
    }
}
