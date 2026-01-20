package com.maru.trading.service;

import com.maru.trading.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * 마켓 데이터 구독 관리 서비스
 */
@Slf4j
@Service
public class MarketDataService {

    private final RestTemplate restTemplate;
    private final String tradingApiBaseUrl;

    public MarketDataService(
            RestTemplate restTemplate,
            @Value("${trading.api.base-url:http://localhost:8099}") String tradingApiBaseUrl
    ) {
        this.restTemplate = restTemplate;
        this.tradingApiBaseUrl = tradingApiBaseUrl;
    }

    /**
     * 구독 종목 추가
     *
     * @param symbols 추가할 종목 코드 목록
     * @return 응답 결과
     */
    public AckResponse addSymbols(List<String> symbols) {
        if (symbols == null) {
            throw new IllegalArgumentException("Symbols cannot be null");
        }
        if (symbols.isEmpty()) {
            throw new IllegalArgumentException("Symbols cannot be empty");
        }

        log.info("[MarketDataService] Adding symbols: {}", symbols);

        String url = tradingApiBaseUrl + "/api/v1/admin/market-data/symbols";

        AddSymbolsRequest request = AddSymbolsRequest.builder()
                .symbols(symbols)
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<AddSymbolsRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<AckResponse> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                AckResponse.class
        );

        AckResponse ackResponse = response.getBody();
        log.info("[MarketDataService] Add symbols response: {}", ackResponse);

        return ackResponse;
    }

    /**
     * 구독 종목 삭제
     *
     * @param symbols 삭제할 종목 코드 목록
     * @return 응답 결과
     */
    public AckResponse removeSymbols(List<String> symbols) {
        if (symbols == null) {
            throw new IllegalArgumentException("Symbols cannot be null");
        }
        if (symbols.isEmpty()) {
            throw new IllegalArgumentException("Symbols cannot be empty");
        }

        log.info("[MarketDataService] Removing symbols: {}", symbols);

        String url = tradingApiBaseUrl + "/api/v1/admin/market-data/symbols";

        RemoveSymbolsRequest request = RemoveSymbolsRequest.builder()
                .symbols(symbols)
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<RemoveSymbolsRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<AckResponse> response = restTemplate.exchange(
                url,
                HttpMethod.DELETE,
                entity,
                AckResponse.class
        );

        AckResponse ackResponse = response.getBody();
        log.info("[MarketDataService] Remove symbols response: {}", ackResponse);

        return ackResponse;
    }

    /**
     * 구독 종목 목록 조회
     *
     * @return 구독 종목 목록
     */
    public SubscribedSymbolsResponse getSubscribedSymbols() {
        log.info("[MarketDataService] Getting subscribed symbols");

        String url = tradingApiBaseUrl + "/api/v1/admin/market-data/symbols";

        ResponseEntity<SubscribedSymbolsResponse> response = restTemplate.getForEntity(
                url,
                SubscribedSymbolsResponse.class
        );

        SubscribedSymbolsResponse symbolsResponse = response.getBody();
        log.info("[MarketDataService] Subscribed symbols: total={}, active={}",
                symbolsResponse != null ? symbolsResponse.getTotal() : 0,
                symbolsResponse != null ? symbolsResponse.isActive() : false
        );

        return symbolsResponse;
    }

    /**
     * 구독 상태 조회
     *
     * @return 구독 상태 정보
     */
    public MarketDataStatusResponse getStatus() {
        log.info("[MarketDataService] Getting market data status");

        String url = tradingApiBaseUrl + "/api/v1/admin/market-data/status";

        ResponseEntity<MarketDataStatusResponse> response = restTemplate.getForEntity(
                url,
                MarketDataStatusResponse.class
        );

        MarketDataStatusResponse statusResponse = response.getBody();
        log.info("[MarketDataService] Market data status: subscribed={}, symbolCount={}",
                statusResponse != null ? statusResponse.isSubscribed() : false,
                statusResponse != null ? statusResponse.getSymbolCount() : 0
        );

        return statusResponse;
    }

    /**
     * 재구독
     *
     * @return 응답 결과
     */
    public AckResponse resubscribe() {
        log.info("[MarketDataService] Resubscribing to market data");

        String url = tradingApiBaseUrl + "/api/v1/admin/market-data/resubscribe";

        ResponseEntity<AckResponse> response = restTemplate.postForEntity(
                url,
                null,
                AckResponse.class
        );

        AckResponse ackResponse = response.getBody();
        log.info("[MarketDataService] Resubscribe response: {}", ackResponse);

        return ackResponse;
    }
}
