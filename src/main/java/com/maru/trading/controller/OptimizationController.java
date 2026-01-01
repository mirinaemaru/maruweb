package com.maru.trading.controller;

import com.maru.trading.service.TradingApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 전략 최적화 컨트롤러
 */
@Slf4j
@Controller
@RequestMapping("/trading/optimization")
@RequiredArgsConstructor
public class OptimizationController {

    private final TradingApiService tradingApiService;

    /**
     * 파라미터 최적화 메인 페이지
     */
    @GetMapping
    public String optimizationPage(@RequestParam(required = false) String strategyId, Model model) {
        try {
            log.info("Loading optimization page for strategyId: {}", strategyId);

            // 전략 목록 조회
            Map<String, Object> strategiesResult = tradingApiService.getStrategies();
            model.addAttribute("strategies", strategiesResult.get("strategies"));
            model.addAttribute("selectedStrategyId", strategyId);

            return "trading/optimization";

        } catch (Exception e) {
            log.error("Failed to load optimization page", e);
            model.addAttribute("error", "최적화 페이지를 불러오는데 실패했습니다: " + e.getMessage());
            return "trading/optimization";
        }
    }

    /**
     * 그리드 서치 실행
     */
    @PostMapping("/grid-search")
    @ResponseBody
    public Map<String, Object> runGridSearch(
            @RequestParam String strategyId,
            @RequestParam String parameterName,
            @RequestParam double startValue,
            @RequestParam double endValue,
            @RequestParam double stepValue,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        try {
            log.info("Running grid search: strategyId={}, param={}, start={}, end={}, step={}",
                    strategyId, parameterName, startValue, endValue, stepValue);

            // 그리드 서치 실행
            Map<String, Object> request = new HashMap<>();
            request.put("strategyId", strategyId);
            request.put("parameterName", parameterName);
            request.put("startValue", startValue);
            request.put("endValue", endValue);
            request.put("stepValue", stepValue);
            request.put("startDate", startDate);
            request.put("endDate", endDate);

            Map<String, Object> result = tradingApiService.runGridSearch(request);

            return result;

        } catch (Exception e) {
            log.error("Failed to run grid search", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", "그리드 서치 실행에 실패했습니다: " + e.getMessage());
            return errorResult;
        }
    }

    /**
     * 최적화 결과 조회
     */
    @GetMapping("/results")
    public String optimizationResults(
            @RequestParam(required = false) String strategyId,
            Model model) {

        try {
            log.info("Loading optimization results for strategyId: {}", strategyId);

            Map<String, Object> result = tradingApiService.getOptimizationResults(strategyId);

            model.addAttribute("results", result.get("results"));
            model.addAttribute("strategyId", strategyId);

            return "trading/optimization-results";

        } catch (Exception e) {
            log.error("Failed to load optimization results", e);
            model.addAttribute("error", "최적화 결과를 불러오는데 실패했습니다: " + e.getMessage());
            return "trading/optimization-results";
        }
    }

    /**
     * 유전 알고리즘 실행 (고급 기능)
     */
    @PostMapping("/genetic-algorithm")
    @ResponseBody
    public Map<String, Object> runGeneticAlgorithm(
            @RequestParam String strategyId,
            @RequestParam(required = false, defaultValue = "100") int populationSize,
            @RequestParam(required = false, defaultValue = "50") int generations,
            @RequestParam(required = false, defaultValue = "0.01") double mutationRate,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        try {
            log.info("Running genetic algorithm: strategyId={}, population={}, generations={}, mutation={}",
                    strategyId, populationSize, generations, mutationRate);

            Map<String, Object> request = new HashMap<>();
            request.put("strategyId", strategyId);
            request.put("populationSize", populationSize);
            request.put("generations", generations);
            request.put("mutationRate", mutationRate);
            request.put("startDate", startDate);
            request.put("endDate", endDate);

            Map<String, Object> result = tradingApiService.runGeneticAlgorithm(request);

            return result;

        } catch (Exception e) {
            log.error("Failed to run genetic algorithm", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", "유전 알고리즘 실행에 실패했습니다: " + e.getMessage());
            return errorResult;
        }
    }
}
