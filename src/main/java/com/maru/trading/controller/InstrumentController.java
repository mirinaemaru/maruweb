package com.maru.trading.controller;

import com.maru.trading.service.TradingApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

/**
 * 종목 관리 컨트롤러
 */
@Slf4j
@Controller
@RequestMapping("/trading/instruments")
@RequiredArgsConstructor
public class InstrumentController {

    private final TradingApiService tradingApiService;

    /**
     * 종목 목록 페이지
     */
    @GetMapping
    public String listInstruments(
            @RequestParam(required = false) String market,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean tradable,
            @RequestParam(required = false) String search,
            Model model) {

        // 초기 페이지 로드 시 기본값 설정: KOSPI, 상장, 거래가능
        if (market == null && status == null && tradable == null && search == null) {
            market = "KOSPI";
            status = "LISTED";
            tradable = true;
        }

        log.info("Listing instruments: market={}, status={}, tradable={}, search={}", market, status, tradable, search);

        Map<String, Object> result = tradingApiService.getInstruments(market, status, tradable, search);
        model.addAttribute("instruments", result.get("items"));
        model.addAttribute("total", result.get("total"));
        model.addAttribute("error", result.get("error"));

        // 필터 값 유지
        model.addAttribute("selectedMarket", market);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedTradable", tradable);
        model.addAttribute("searchKeyword", search);

        return "trading/instruments";
    }

    /**
     * 종목 상세 페이지
     */
    @GetMapping("/{symbol}")
    public String getInstrument(@PathVariable String symbol, Model model) {
        log.info("Getting instrument detail: {}", symbol);

        Map<String, Object> instrument = tradingApiService.getInstrument(symbol);

        if (instrument.containsKey("error")) {
            model.addAttribute("error", instrument.get("error"));
        }

        model.addAttribute("instrument", instrument);
        return "trading/instrument-detail";
    }

    /**
     * 종목 상태 업데이트
     */
    @PostMapping("/{symbol}/status")
    public String updateInstrumentStatus(
            @PathVariable String symbol,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean tradable,
            @RequestParam(required = false) Boolean halted,
            RedirectAttributes redirectAttributes) {

        log.info("Updating instrument status: symbol={}, status={}, tradable={}, halted={}",
                symbol, status, tradable, halted);

        try {
            tradingApiService.updateInstrumentStatus(symbol, status, tradable, halted);
            redirectAttributes.addFlashAttribute("successMessage", "종목 상태가 업데이트되었습니다.");
        } catch (Exception e) {
            log.error("Failed to update instrument status", e);
            redirectAttributes.addFlashAttribute("errorMessage", "종목 상태 업데이트에 실패했습니다: " + e.getMessage());
        }

        return "redirect:/trading/instruments/" + symbol;
    }

    /**
     * 거래 가능 상태 토글 (AJAX)
     */
    @PostMapping("/{symbol}/toggle-tradable")
    @ResponseBody
    public Map<String, Object> toggleTradable(@PathVariable String symbol, @RequestParam boolean tradable) {
        log.info("Toggling tradable status: symbol={}, tradable={}", symbol, tradable);
        return tradingApiService.updateInstrumentStatus(symbol, null, tradable, null);
    }

    /**
     * 거래 정지 상태 토글 (AJAX)
     */
    @PostMapping("/{symbol}/toggle-halted")
    @ResponseBody
    public Map<String, Object> toggleHalted(@PathVariable String symbol, @RequestParam boolean halted) {
        log.info("Toggling halted status: symbol={}, halted={}", symbol, halted);
        return tradingApiService.updateInstrumentStatus(symbol, null, null, halted);
    }
}
