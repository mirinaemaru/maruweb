package com.maru.trading.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 엑셀 내보내기 서비스
 */
@Slf4j
@Service
public class ExcelExportService {

    /**
     * 주문 내역 엑셀 다운로드
     */
    public void exportOrdersToExcel(List<Map<String, Object>> orders, HttpServletResponse response) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("주문내역");

        // Header style
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        // Header row
        Row headerRow = sheet.createRow(0);
        String[] headers = {"주문ID", "계좌ID", "종목", "주문유형", "수량", "가격", "상태", "주문시간"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Data rows
        int rowNum = 1;
        for (Map<String, Object> order : orders) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(String.valueOf(order.getOrDefault("orderId", "")));
            row.createCell(1).setCellValue(String.valueOf(order.getOrDefault("accountId", "")));
            row.createCell(2).setCellValue(String.valueOf(order.getOrDefault("symbol", "")));
            row.createCell(3).setCellValue(String.valueOf(order.getOrDefault("side", "")));
            row.createCell(4).setCellValue(String.valueOf(order.getOrDefault("quantity", "")));
            row.createCell(5).setCellValue(String.valueOf(order.getOrDefault("price", "")));
            row.createCell(6).setCellValue(String.valueOf(order.getOrDefault("status", "")));
            row.createCell(7).setCellValue(String.valueOf(order.getOrDefault("createdAt", "")));
        }

        // Auto-size columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        // Write to response
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=orders.xlsx");
        workbook.write(response.getOutputStream());
        workbook.close();
    }

    /**
     * 체결 내역 엑셀 다운로드
     */
    public void exportFillsToExcel(List<Map<String, Object>> fills, HttpServletResponse response) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("체결내역");

        // Header style
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        // Header row
        Row headerRow = sheet.createRow(0);
        String[] headers = {"체결ID", "주문ID", "종목", "수량", "가격", "체결시간"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Data rows
        int rowNum = 1;
        for (Map<String, Object> fill : fills) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(String.valueOf(fill.getOrDefault("fillId", "")));
            row.createCell(1).setCellValue(String.valueOf(fill.getOrDefault("orderId", "")));
            row.createCell(2).setCellValue(String.valueOf(fill.getOrDefault("symbol", "")));
            row.createCell(3).setCellValue(String.valueOf(fill.getOrDefault("quantity", "")));
            row.createCell(4).setCellValue(String.valueOf(fill.getOrDefault("price", "")));
            row.createCell(5).setCellValue(String.valueOf(fill.getOrDefault("filledAt", "")));
        }

        // Auto-size columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        // Write to response
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=fills.xlsx");
        workbook.write(response.getOutputStream());
        workbook.close();
    }
}
