package com.maru.trading.controller;

import com.maru.trading.service.TradingApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 성과 분석 및 리포팅 컨트롤러
 */
@Slf4j
@Controller
@RequestMapping("/trading/performance")
@RequiredArgsConstructor
public class PerformanceController {

    private final TradingApiService tradingApiService;

    /**
     * 성과 분석 메인 페이지 (일별/월별)
     */
    @GetMapping
    public String performanceAnalysis(
            @RequestParam(required = false) String period,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String strategyId,
            Model model) {

        try {
            // 기본값 설정
            if (period == null) period = "daily";
            if (startDate == null) startDate = LocalDate.now().minusDays(30).toString();
            if (endDate == null) endDate = LocalDate.now().toString();

            log.info("Loading performance analysis: period={}, startDate={}, endDate={}, strategyId={}",
                    period, startDate, endDate, strategyId);

            // 성과 데이터 조회
            Map<String, Object> result = tradingApiService.getPerformanceAnalysis(period, startDate, endDate, strategyId);

            model.addAttribute("period", period);
            model.addAttribute("startDate", startDate);
            model.addAttribute("endDate", endDate);
            model.addAttribute("strategyId", strategyId);
            model.addAttribute("performanceData", result.get("data"));
            model.addAttribute("summary", result.get("summary"));

            return "trading/performance-analysis";

        } catch (Exception e) {
            log.error("Failed to load performance analysis", e);
            model.addAttribute("error", "성과 분석을 불러오는데 실패했습니다: " + e.getMessage());
            return "trading/performance-analysis";
        }
    }

    /**
     * 전략별 통계 페이지
     */
    @GetMapping("/strategies")
    public String strategyStatistics(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            Model model) {

        try {
            if (startDate == null) startDate = LocalDate.now().minusDays(30).toString();
            if (endDate == null) endDate = LocalDate.now().toString();

            log.info("Loading strategy statistics: startDate={}, endDate={}", startDate, endDate);

            // 전략별 통계 조회
            Map<String, Object> result = tradingApiService.getStrategyStatistics(startDate, endDate);

            model.addAttribute("startDate", startDate);
            model.addAttribute("endDate", endDate);
            model.addAttribute("strategies", result.get("strategies"));
            model.addAttribute("summary", result.get("summary"));

            return "trading/strategy-statistics";

        } catch (Exception e) {
            log.error("Failed to load strategy statistics", e);
            model.addAttribute("error", "전략 통계를 불러오는데 실패했습니다: " + e.getMessage());
            return "trading/strategy-statistics";
        }
    }

    /**
     * 성과 캘린더 페이지
     */
    @GetMapping("/calendar")
    public String performanceCalendar(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) String strategyId,
            Model model) {

        try {
            // 기본값: 현재 년/월
            if (year == null) year = LocalDate.now().getYear();
            if (month == null) month = LocalDate.now().getMonthValue();

            log.info("Loading performance calendar: year={}, month={}, strategyId={}", year, month, strategyId);

            // 월별 일일 성과 데이터 조회
            Map<String, Object> result = tradingApiService.getMonthlyDailyPerformance(year, month, strategyId);

            // 전략 목록 조회 (필터용)
            Map<String, Object> strategiesResult = tradingApiService.getStrategies();

            model.addAttribute("year", year);
            model.addAttribute("month", month);
            model.addAttribute("strategyId", strategyId);
            model.addAttribute("calendarData", result);
            model.addAttribute("strategies", strategiesResult.get("strategies"));

            return "trading/performance-calendar";

        } catch (Exception e) {
            log.error("Failed to load performance calendar", e);
            model.addAttribute("error", "성과 캘린더를 불러오는데 실패했습니다: " + e.getMessage());
            model.addAttribute("year", year != null ? year : LocalDate.now().getYear());
            model.addAttribute("month", month != null ? month : LocalDate.now().getMonthValue());
            return "trading/performance-calendar";
        }
    }

    /**
     * 캘린더 데이터 API (AJAX용)
     */
    @GetMapping("/api/calendar/data")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getCalendarData(
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam(required = false) String strategyId) {

        try {
            log.info("API: Loading calendar data: year={}, month={}, strategyId={}", year, month, strategyId);
            Map<String, Object> result = tradingApiService.getMonthlyDailyPerformance(year, month, strategyId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to get calendar data", e);
            Map<String, Object> errorResult = new java.util.HashMap<>();
            errorResult.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResult);
        }
    }

    /**
     * Excel 리포트 생성 및 다운로드
     */
    @GetMapping("/export")
    public ResponseEntity<ByteArrayResource> exportToExcel(
            @RequestParam(required = false) String period,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String strategyId) {

        try {
            // 기본값 설정
            if (period == null) period = "daily";
            if (startDate == null) startDate = LocalDate.now().minusDays(30).toString();
            if (endDate == null) endDate = LocalDate.now().toString();

            log.info("Exporting performance data to Excel: period={}, startDate={}, endDate={}, strategyId={}",
                    period, startDate, endDate, strategyId);

            // 성과 데이터 조회
            Map<String, Object> result = tradingApiService.getPerformanceAnalysis(period, startDate, endDate, strategyId);
            List<Map<String, Object>> performanceData = (List<Map<String, Object>>) result.get("data");
            Map<String, Object> summary = (Map<String, Object>) result.get("summary");

            // Excel 생성
            Workbook workbook = new XSSFWorkbook();

            // 스타일 생성
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);

            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);

            CellStyle numberStyle = workbook.createCellStyle();
            numberStyle.cloneStyleFrom(dataStyle);
            numberStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0"));

            CellStyle percentStyle = workbook.createCellStyle();
            percentStyle.cloneStyleFrom(dataStyle);
            percentStyle.setDataFormat(workbook.createDataFormat().getFormat("0.00%"));

            // 요약 시트
            Sheet summarySheet = workbook.createSheet("요약");
            int summaryRowNum = 0;

            Row summaryTitleRow = summarySheet.createRow(summaryRowNum++);
            Cell titleCell = summaryTitleRow.createCell(0);
            titleCell.setCellValue("성과 분석 리포트");
            titleCell.setCellStyle(headerStyle);

            summaryRowNum++;
            Row periodRow = summarySheet.createRow(summaryRowNum++);
            periodRow.createCell(0).setCellValue("분석 기간:");
            periodRow.createCell(1).setCellValue(startDate + " ~ " + endDate);

            Row periodTypeRow = summarySheet.createRow(summaryRowNum++);
            periodTypeRow.createCell(0).setCellValue("집계 단위:");
            periodTypeRow.createCell(1).setCellValue(period.equals("daily") ? "일별" : "월별");

            if (strategyId != null && !strategyId.isEmpty()) {
                Row strategyRow = summarySheet.createRow(summaryRowNum++);
                strategyRow.createCell(0).setCellValue("전략 ID:");
                strategyRow.createCell(1).setCellValue(strategyId);
            }

            summaryRowNum++;
            if (summary != null) {
                Row summaryHeaderRow = summarySheet.createRow(summaryRowNum++);
                summaryHeaderRow.createCell(0).setCellValue("항목");
                summaryHeaderRow.createCell(1).setCellValue("값");
                summaryHeaderRow.getCell(0).setCellStyle(headerStyle);
                summaryHeaderRow.getCell(1).setCellStyle(headerStyle);

                Row totalPLRow = summarySheet.createRow(summaryRowNum++);
                totalPLRow.createCell(0).setCellValue("총 수익/손실");
                Cell plCell = totalPLRow.createCell(1);
                plCell.setCellValue(((Number) summary.get("totalProfitLoss")).doubleValue());
                plCell.setCellStyle(numberStyle);

                Row totalTradesRow = summarySheet.createRow(summaryRowNum++);
                totalTradesRow.createCell(0).setCellValue("총 거래 횟수");
                totalTradesRow.createCell(1).setCellValue(((Number) summary.get("totalTrades")).doubleValue());

                Row winRateRow = summarySheet.createRow(summaryRowNum++);
                winRateRow.createCell(0).setCellValue("승률");
                Cell winRateCell = winRateRow.createCell(1);
                winRateCell.setCellValue(((Number) summary.get("winRate")).doubleValue() / 100);
                winRateCell.setCellStyle(percentStyle);

                Row avgPLRow = summarySheet.createRow(summaryRowNum++);
                avgPLRow.createCell(0).setCellValue("평균 수익/손실");
                Cell avgPLCell = avgPLRow.createCell(1);
                avgPLCell.setCellValue(((Number) summary.get("avgProfitLoss")).doubleValue());
                avgPLCell.setCellStyle(numberStyle);
            }

            summarySheet.autoSizeColumn(0);
            summarySheet.autoSizeColumn(1);

            // 상세 데이터 시트
            if (performanceData != null && !performanceData.isEmpty()) {
                Sheet detailSheet = workbook.createSheet("상세 데이터");
                int detailRowNum = 0;

                // 헤더
                Row headerRow = detailSheet.createRow(detailRowNum++);
                String[] headers = {"날짜", "거래 횟수", "수익/손실", "승률", "최대 이익", "최대 손실"};
                for (int i = 0; i < headers.length; i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(headers[i]);
                    cell.setCellStyle(headerStyle);
                }

                // 데이터 행
                for (Map<String, Object> data : performanceData) {
                    Row row = detailSheet.createRow(detailRowNum++);

                    row.createCell(0).setCellValue((String) data.get("date"));

                    Cell tradesCell = row.createCell(1);
                    tradesCell.setCellValue(((Number) data.get("trades")).doubleValue());
                    tradesCell.setCellStyle(dataStyle);

                    Cell plCell = row.createCell(2);
                    plCell.setCellValue(((Number) data.get("profitLoss")).doubleValue());
                    plCell.setCellStyle(numberStyle);

                    Cell winRateCell = row.createCell(3);
                    winRateCell.setCellValue(((Number) data.get("winRate")).doubleValue() / 100);
                    winRateCell.setCellStyle(percentStyle);

                    Cell maxProfitCell = row.createCell(4);
                    maxProfitCell.setCellValue(((Number) data.get("maxProfit")).doubleValue());
                    maxProfitCell.setCellStyle(numberStyle);

                    Cell maxLossCell = row.createCell(5);
                    maxLossCell.setCellValue(((Number) data.get("maxLoss")).doubleValue());
                    maxLossCell.setCellStyle(numberStyle);
                }

                // 컬럼 너비 자동 조정
                for (int i = 0; i < headers.length; i++) {
                    detailSheet.autoSizeColumn(i);
                }
            }

            // Excel 파일을 바이트 배열로 변환
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            workbook.close();

            ByteArrayResource resource = new ByteArrayResource(outputStream.toByteArray());

            // 파일명 생성
            String filename = String.format("성과분석_%s_%s.xlsx",
                    period,
                    LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")));

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(resource);

        } catch (Exception e) {
            log.error("Failed to export performance data to Excel", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
