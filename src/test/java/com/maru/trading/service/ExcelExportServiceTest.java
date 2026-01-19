package com.maru.trading.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ExcelExportService 테스트")
class ExcelExportServiceTest {

    private ExcelExportService excelExportService;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        excelExportService = new ExcelExportService();
        response = new MockHttpServletResponse();
    }

    @Nested
    @DisplayName("exportOrdersToExcel 메서드")
    class ExportOrdersToExcelTest {

        @Test
        @DisplayName("주문 내역 엑셀 내보내기 - 성공")
        void exportOrdersToExcel_Success() throws IOException {
            // given
            List<Map<String, Object>> orders = createSampleOrders();

            // when
            excelExportService.exportOrdersToExcel(orders, response);

            // then
            assertThat(response.getContentType())
                    .isEqualTo("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            assertThat(response.getHeader("Content-Disposition"))
                    .isEqualTo("attachment; filename=orders.xlsx");

            // Excel 파일 검증
            Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(response.getContentAsByteArray()));
            Sheet sheet = workbook.getSheet("주문내역");
            assertThat(sheet).isNotNull();

            // 헤더 검증
            Row headerRow = sheet.getRow(0);
            assertThat(headerRow.getCell(0).getStringCellValue()).isEqualTo("주문ID");
            assertThat(headerRow.getCell(1).getStringCellValue()).isEqualTo("계좌ID");
            assertThat(headerRow.getCell(2).getStringCellValue()).isEqualTo("종목");
            assertThat(headerRow.getCell(7).getStringCellValue()).isEqualTo("주문시간");

            // 데이터 행 검증
            Row dataRow = sheet.getRow(1);
            assertThat(dataRow.getCell(0).getStringCellValue()).isEqualTo("ORD001");
            assertThat(dataRow.getCell(2).getStringCellValue()).isEqualTo("005930");

            workbook.close();
        }

        @Test
        @DisplayName("빈 주문 목록 엑셀 내보내기")
        void exportOrdersToExcel_EmptyList() throws IOException {
            // given
            List<Map<String, Object>> orders = Collections.emptyList();

            // when
            excelExportService.exportOrdersToExcel(orders, response);

            // then
            Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(response.getContentAsByteArray()));
            Sheet sheet = workbook.getSheet("주문내역");
            assertThat(sheet).isNotNull();

            // 헤더만 존재하고 데이터 행은 없음
            assertThat(sheet.getRow(0)).isNotNull();
            assertThat(sheet.getRow(1)).isNull();

            workbook.close();
        }

        @Test
        @DisplayName("여러 주문 내역 엑셀 내보내기")
        void exportOrdersToExcel_MultipleOrders() throws IOException {
            // given
            List<Map<String, Object>> orders = new ArrayList<>();
            for (int i = 1; i <= 5; i++) {
                Map<String, Object> order = new HashMap<>();
                order.put("orderId", "ORD00" + i);
                order.put("accountId", "ACC001");
                order.put("symbol", "005930");
                order.put("side", "BUY");
                order.put("quantity", 100 * i);
                order.put("price", 70000);
                order.put("status", "FILLED");
                order.put("createdAt", "2024-01-01 10:0" + i + ":00");
                orders.add(order);
            }

            // when
            excelExportService.exportOrdersToExcel(orders, response);

            // then
            Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(response.getContentAsByteArray()));
            Sheet sheet = workbook.getSheet("주문내역");

            // 5개의 데이터 행이 존재해야 함
            for (int i = 1; i <= 5; i++) {
                assertThat(sheet.getRow(i)).isNotNull();
                assertThat(sheet.getRow(i).getCell(0).getStringCellValue()).isEqualTo("ORD00" + i);
            }

            workbook.close();
        }

        @Test
        @DisplayName("null 값이 포함된 주문 내역 내보내기")
        void exportOrdersToExcel_WithNullValues() throws IOException {
            // given
            List<Map<String, Object>> orders = new ArrayList<>();
            Map<String, Object> order = new HashMap<>();
            order.put("orderId", "ORD001");
            // 다른 필드들은 null (getOrDefault에서 "" 반환)
            orders.add(order);

            // when
            excelExportService.exportOrdersToExcel(orders, response);

            // then
            Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(response.getContentAsByteArray()));
            Sheet sheet = workbook.getSheet("주문내역");

            Row dataRow = sheet.getRow(1);
            assertThat(dataRow.getCell(0).getStringCellValue()).isEqualTo("ORD001");
            assertThat(dataRow.getCell(1).getStringCellValue()).isEqualTo("");
            assertThat(dataRow.getCell(2).getStringCellValue()).isEqualTo("");

            workbook.close();
        }

        @Test
        @DisplayName("헤더 스타일 적용 확인")
        void exportOrdersToExcel_HeaderStyle() throws IOException {
            // given
            List<Map<String, Object>> orders = createSampleOrders();

            // when
            excelExportService.exportOrdersToExcel(orders, response);

            // then
            Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(response.getContentAsByteArray()));
            Sheet sheet = workbook.getSheet("주문내역");
            Row headerRow = sheet.getRow(0);

            // 헤더 셀 스타일 확인
            CellStyle headerStyle = headerRow.getCell(0).getCellStyle();
            assertThat(headerStyle.getFillPattern()).isEqualTo(FillPatternType.SOLID_FOREGROUND);

            workbook.close();
        }
    }

    @Nested
    @DisplayName("exportFillsToExcel 메서드")
    class ExportFillsToExcelTest {

        @Test
        @DisplayName("체결 내역 엑셀 내보내기 - 성공")
        void exportFillsToExcel_Success() throws IOException {
            // given
            List<Map<String, Object>> fills = createSampleFills();

            // when
            excelExportService.exportFillsToExcel(fills, response);

            // then
            assertThat(response.getContentType())
                    .isEqualTo("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            assertThat(response.getHeader("Content-Disposition"))
                    .isEqualTo("attachment; filename=fills.xlsx");

            // Excel 파일 검증
            Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(response.getContentAsByteArray()));
            Sheet sheet = workbook.getSheet("체결내역");
            assertThat(sheet).isNotNull();

            // 헤더 검증
            Row headerRow = sheet.getRow(0);
            assertThat(headerRow.getCell(0).getStringCellValue()).isEqualTo("체결ID");
            assertThat(headerRow.getCell(1).getStringCellValue()).isEqualTo("주문ID");
            assertThat(headerRow.getCell(2).getStringCellValue()).isEqualTo("종목");
            assertThat(headerRow.getCell(5).getStringCellValue()).isEqualTo("체결시간");

            // 데이터 행 검증
            Row dataRow = sheet.getRow(1);
            assertThat(dataRow.getCell(0).getStringCellValue()).isEqualTo("FILL001");
            assertThat(dataRow.getCell(2).getStringCellValue()).isEqualTo("005930");

            workbook.close();
        }

        @Test
        @DisplayName("빈 체결 목록 엑셀 내보내기")
        void exportFillsToExcel_EmptyList() throws IOException {
            // given
            List<Map<String, Object>> fills = Collections.emptyList();

            // when
            excelExportService.exportFillsToExcel(fills, response);

            // then
            Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(response.getContentAsByteArray()));
            Sheet sheet = workbook.getSheet("체결내역");
            assertThat(sheet).isNotNull();

            // 헤더만 존재하고 데이터 행은 없음
            assertThat(sheet.getRow(0)).isNotNull();
            assertThat(sheet.getRow(1)).isNull();

            workbook.close();
        }

        @Test
        @DisplayName("여러 체결 내역 엑셀 내보내기")
        void exportFillsToExcel_MultipleFills() throws IOException {
            // given
            List<Map<String, Object>> fills = new ArrayList<>();
            for (int i = 1; i <= 3; i++) {
                Map<String, Object> fill = new HashMap<>();
                fill.put("fillId", "FILL00" + i);
                fill.put("orderId", "ORD001");
                fill.put("symbol", "005930");
                fill.put("quantity", 50 * i);
                fill.put("price", 70000 + (i * 100));
                fill.put("filledAt", "2024-01-01 10:0" + i + ":00");
                fills.add(fill);
            }

            // when
            excelExportService.exportFillsToExcel(fills, response);

            // then
            Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(response.getContentAsByteArray()));
            Sheet sheet = workbook.getSheet("체결내역");

            // 3개의 데이터 행이 존재해야 함
            for (int i = 1; i <= 3; i++) {
                assertThat(sheet.getRow(i)).isNotNull();
                assertThat(sheet.getRow(i).getCell(0).getStringCellValue()).isEqualTo("FILL00" + i);
            }

            workbook.close();
        }

        @Test
        @DisplayName("null 값이 포함된 체결 내역 내보내기")
        void exportFillsToExcel_WithNullValues() throws IOException {
            // given
            List<Map<String, Object>> fills = new ArrayList<>();
            Map<String, Object> fill = new HashMap<>();
            fill.put("fillId", "FILL001");
            // 다른 필드들은 없음
            fills.add(fill);

            // when
            excelExportService.exportFillsToExcel(fills, response);

            // then
            Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(response.getContentAsByteArray()));
            Sheet sheet = workbook.getSheet("체결내역");

            Row dataRow = sheet.getRow(1);
            assertThat(dataRow.getCell(0).getStringCellValue()).isEqualTo("FILL001");
            assertThat(dataRow.getCell(1).getStringCellValue()).isEqualTo("");

            workbook.close();
        }

        @Test
        @DisplayName("헤더 스타일 적용 확인")
        void exportFillsToExcel_HeaderStyle() throws IOException {
            // given
            List<Map<String, Object>> fills = createSampleFills();

            // when
            excelExportService.exportFillsToExcel(fills, response);

            // then
            Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(response.getContentAsByteArray()));
            Sheet sheet = workbook.getSheet("체결내역");
            Row headerRow = sheet.getRow(0);

            // 헤더 셀 스타일 확인
            CellStyle headerStyle = headerRow.getCell(0).getCellStyle();
            assertThat(headerStyle.getFillPattern()).isEqualTo(FillPatternType.SOLID_FOREGROUND);

            workbook.close();
        }

        @Test
        @DisplayName("6개의 헤더 컬럼 확인")
        void exportFillsToExcel_SixHeaderColumns() throws IOException {
            // given
            List<Map<String, Object>> fills = createSampleFills();

            // when
            excelExportService.exportFillsToExcel(fills, response);

            // then
            Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(response.getContentAsByteArray()));
            Sheet sheet = workbook.getSheet("체결내역");
            Row headerRow = sheet.getRow(0);

            String[] expectedHeaders = {"체결ID", "주문ID", "종목", "수량", "가격", "체결시간"};
            for (int i = 0; i < expectedHeaders.length; i++) {
                assertThat(headerRow.getCell(i).getStringCellValue()).isEqualTo(expectedHeaders[i]);
            }

            workbook.close();
        }
    }

    // Helper methods
    private List<Map<String, Object>> createSampleOrders() {
        List<Map<String, Object>> orders = new ArrayList<>();
        Map<String, Object> order = new HashMap<>();
        order.put("orderId", "ORD001");
        order.put("accountId", "ACC001");
        order.put("symbol", "005930");
        order.put("side", "BUY");
        order.put("quantity", 100);
        order.put("price", 70000);
        order.put("status", "FILLED");
        order.put("createdAt", "2024-01-01 10:00:00");
        orders.add(order);
        return orders;
    }

    private List<Map<String, Object>> createSampleFills() {
        List<Map<String, Object>> fills = new ArrayList<>();
        Map<String, Object> fill = new HashMap<>();
        fill.put("fillId", "FILL001");
        fill.put("orderId", "ORD001");
        fill.put("symbol", "005930");
        fill.put("quantity", 100);
        fill.put("price", 70000);
        fill.put("filledAt", "2024-01-01 10:00:05");
        fills.add(fill);
        return fills;
    }
}
