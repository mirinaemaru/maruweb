package com.maru.trading.controller;

import com.maru.trading.dto.AckResponse;
import com.maru.trading.dto.MarketDataStatusResponse;
import com.maru.trading.dto.SubscribedSymbolsResponse;
import com.maru.trading.service.MarketDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 마켓 데이터 구독 관리 컨트롤러
 */
@Slf4j
@Controller
@RequestMapping("/trading/market-data")
@RequiredArgsConstructor
public class MarketDataController {

    private final MarketDataService marketDataService;

    /**
     * 구독 종목 관리 페이지
     */
    @GetMapping
    public String marketDataPage(Model model) {
        log.info("GET /trading/market-data - Market data management page");

        try {
            // 구독 종목 목록 조회
            SubscribedSymbolsResponse symbolsResponse = marketDataService.getSubscribedSymbols();
            model.addAttribute("symbols", symbolsResponse);

            // 구독 상태 조회
            MarketDataStatusResponse statusResponse = marketDataService.getStatus();
            model.addAttribute("status", statusResponse);

            log.info("Market data page loaded: {} symbols, subscribed={}",
                    symbolsResponse.getTotal(), statusResponse.isSubscribed());

        } catch (Exception e) {
            log.error("Failed to load market data page", e);
            model.addAttribute("error", "구독 정보를 불러오는 데 실패했습니다: " + e.getMessage());
        }

        return "trading/market-data";
    }

    /**
     * 종목 추가
     */
    @PostMapping("/add")
    public String addSymbols(
            @RequestParam(required = false) String symbols,
            RedirectAttributes redirectAttributes
    ) {
        log.info("POST /trading/market-data/add - symbols={}", symbols);

        try {
            // 입력 검증
            if (symbols == null || symbols.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "추가할 종목 코드를 입력해주세요.");
                return "redirect:/trading/market-data";
            }

            // 쉼표로 구분된 종목 코드 파싱
            List<String> symbolList = Arrays.stream(symbols.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());

            if (symbolList.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "유효한 종목 코드를 입력해주세요.");
                return "redirect:/trading/market-data";
            }

            // 종목 추가
            AckResponse response = marketDataService.addSymbols(symbolList);

            if (response.getOk()) {
                redirectAttributes.addFlashAttribute("message", response.getMessage());
                log.info("Successfully added symbols: {}", symbolList);
            } else {
                redirectAttributes.addFlashAttribute("error", "종목 추가 실패: " + response.getMessage());
                log.warn("Failed to add symbols: {}", response.getMessage());
            }

        } catch (Exception e) {
            log.error("Failed to add symbols", e);
            redirectAttributes.addFlashAttribute("error", "종목 추가 중 오류가 발생했습니다: " + e.getMessage());
        }

        return "redirect:/trading/market-data";
    }

    /**
     * 종목 삭제
     */
    @PostMapping("/remove")
    public String removeSymbols(
            @RequestParam(required = false) String symbols,
            RedirectAttributes redirectAttributes
    ) {
        log.info("POST /trading/market-data/remove - symbols={}", symbols);

        try {
            // 입력 검증
            if (symbols == null || symbols.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "삭제할 종목 코드를 입력해주세요.");
                return "redirect:/trading/market-data";
            }

            // 쉼표로 구분된 종목 코드 파싱
            List<String> symbolList = Arrays.stream(symbols.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());

            if (symbolList.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "유효한 종목 코드를 입력해주세요.");
                return "redirect:/trading/market-data";
            }

            // 종목 삭제
            AckResponse response = marketDataService.removeSymbols(symbolList);

            if (response.getOk()) {
                redirectAttributes.addFlashAttribute("message", response.getMessage());
                log.info("Successfully removed symbols: {}", symbolList);
            } else {
                redirectAttributes.addFlashAttribute("error", "종목 삭제 실패: " + response.getMessage());
                log.warn("Failed to remove symbols: {}", response.getMessage());
            }

        } catch (Exception e) {
            log.error("Failed to remove symbols", e);
            redirectAttributes.addFlashAttribute("error", "종목 삭제 중 오류가 발생했습니다: " + e.getMessage());
        }

        return "redirect:/trading/market-data";
    }

    /**
     * 재구독
     */
    @PostMapping("/resubscribe")
    public String resubscribe(RedirectAttributes redirectAttributes) {
        log.info("POST /trading/market-data/resubscribe - Resubscribing to market data");

        try {
            AckResponse response = marketDataService.resubscribe();

            if (response != null && response.getOk()) {
                redirectAttributes.addFlashAttribute("message", response.getMessage());
                log.info("Successfully resubscribed to market data");
            } else {
                redirectAttributes.addFlashAttribute("error", "재구독 실패: " + (response != null ? response.getMessage() : "응답 없음"));
                log.warn("Failed to resubscribe: {}", response != null ? response.getMessage() : "null response");
            }

        } catch (Exception e) {
            log.error("Failed to resubscribe", e);
            redirectAttributes.addFlashAttribute("error", "재구독 중 오류가 발생했습니다: " + e.getMessage());
        }

        return "redirect:/trading/market-data";
    }
}
