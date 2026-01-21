package com.maru.integration;

import com.maru.todo.TodoApplication;
import com.maru.trading.dto.LogEntry;
import com.maru.trading.dto.LogSearchCriteria;
import com.maru.trading.service.SystemLogService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = {TodoApplication.class, TestConfig.class})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("SystemLogController 통합테스트")
class SystemLogControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SystemLogService systemLogService;

    // ==================== Helper Methods ====================

    private com.maru.trading.dto.LogFileInfo createLogFileInfo(String filename, boolean current) {
        com.maru.trading.dto.LogFileInfo info = new com.maru.trading.dto.LogFileInfo(
                filename,
                1024L,
                LocalDateTime.now()
        );
        info.setCurrent(current);
        return info;
    }

    private LogEntry createLogEntry(int lineNumber, String level, String message) {
        LogEntry entry = new LogEntry("raw line", lineNumber);
        entry.setTimestamp("2026-01-20 16:30:00.123");
        entry.setThread("main");
        entry.setLevel(level);
        entry.setLogger("com.maru.test");
        entry.setMessage(message);
        return entry;
    }

    // ==================== systemLogsPage Tests ====================

    @Nested
    @DisplayName("systemLogsPage - GET /trading/system-logs")
    class SystemLogsPageTests {

        @Test
        @DisplayName("시스템 로그 페이지 조회 - 성공")
        void systemLogsPage_Success() throws Exception {
            // Given
            List<com.maru.trading.dto.LogFileInfo> files = Arrays.asList(
                    createLogFileInfo("maruweb.log", true),
                    createLogFileInfo("app.log", false)
            );
            when(systemLogService.listLogFiles()).thenReturn(files);
            when(systemLogService.getCurrentLogFile()).thenReturn("maruweb.log");
            when(systemLogService.getLogDirectory()).thenReturn("/var/logs/trading");

            // When & Then
            mockMvc.perform(get("/trading/system-logs"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("trading/system-logs"))
                    .andExpect(model().attributeExists("logFiles"))
                    .andExpect(model().attributeExists("currentFile"))
                    .andExpect(model().attributeExists("logDirectory"))
                    .andExpect(model().attribute("currentFile", "maruweb.log"));
        }

        @Test
        @DisplayName("시스템 로그 페이지 조회 - 빈 파일 목록")
        void systemLogsPage_EmptyFiles() throws Exception {
            // Given
            when(systemLogService.listLogFiles()).thenReturn(Collections.emptyList());
            when(systemLogService.getCurrentLogFile()).thenReturn("maruweb.log");
            when(systemLogService.getLogDirectory()).thenReturn("/var/logs/trading");

            // When & Then
            mockMvc.perform(get("/trading/system-logs"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("trading/system-logs"))
                    .andExpect(model().attribute("logFiles", Collections.emptyList()));
        }
    }

    // ==================== listLogFiles API Tests ====================

    @Nested
    @DisplayName("listLogFiles API - GET /trading/api/logs/files")
    class ListLogFilesApiTests {

        @Test
        @DisplayName("로그 파일 목록 API - 성공")
        void listLogFiles_Success() throws Exception {
            // Given
            List<com.maru.trading.dto.LogFileInfo> files = Arrays.asList(
                    createLogFileInfo("maruweb.log", true),
                    createLogFileInfo("app.log.gz", false)
            );
            when(systemLogService.listLogFiles()).thenReturn(files);
            when(systemLogService.getCurrentLogFile()).thenReturn("maruweb.log");

            // When & Then
            mockMvc.perform(get("/trading/api/logs/files")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.files").isArray())
                    .andExpect(jsonPath("$.files.length()").value(2))
                    .andExpect(jsonPath("$.currentFile").value("maruweb.log"));
        }

        @Test
        @DisplayName("로그 파일 목록 API - 예외 발생")
        void listLogFiles_Exception() throws Exception {
            // Given
            when(systemLogService.listLogFiles()).thenThrow(new RuntimeException("Test error"));

            // When & Then
            mockMvc.perform(get("/trading/api/logs/files")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error").exists());
        }
    }

    // ==================== tailLog API Tests ====================

    @Nested
    @DisplayName("tailLog API - GET /trading/api/logs/tail")
    class TailLogApiTests {

        @Test
        @DisplayName("로그 tail API - 성공")
        void tailLog_Success() throws Exception {
            // Given
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("entries", Arrays.asList(
                    createLogEntry(1, "INFO", "First log"),
                    createLogEntry(2, "ERROR", "Error log")
            ));
            result.put("filename", "maruweb.log");
            result.put("totalLines", 2);

            when(systemLogService.tailLog(any(LogSearchCriteria.class))).thenReturn(result);

            // When & Then
            mockMvc.perform(get("/trading/api/logs/tail")
                            .param("lines", "100")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.entries").isArray())
                    .andExpect(jsonPath("$.entries.length()").value(2))
                    .andExpect(jsonPath("$.filename").value("maruweb.log"));
        }

        @Test
        @DisplayName("로그 tail API - 특정 파일 지정")
        void tailLog_SpecificFile() throws Exception {
            // Given
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("entries", Collections.emptyList());
            result.put("filename", "app.log");
            result.put("totalLines", 0);

            when(systemLogService.tailLog(any(LogSearchCriteria.class))).thenReturn(result);

            // When & Then
            mockMvc.perform(get("/trading/api/logs/tail")
                            .param("filename", "app.log")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.filename").value("app.log"));

            verify(systemLogService).tailLog(argThat(criteria ->
                    "app.log".equals(criteria.getFilename())
            ));
        }

        @Test
        @DisplayName("로그 tail API - 레벨 필터")
        void tailLog_WithLevelFilter() throws Exception {
            // Given
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("entries", Collections.singletonList(createLogEntry(1, "ERROR", "Error")));
            result.put("filename", "maruweb.log");
            result.put("totalLines", 1);

            when(systemLogService.tailLog(any(LogSearchCriteria.class))).thenReturn(result);

            // When & Then
            mockMvc.perform(get("/trading/api/logs/tail")
                            .param("levels", "ERROR", "WARN")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(systemLogService).tailLog(argThat(criteria ->
                    criteria.getLevels() != null &&
                            criteria.getLevels().contains("ERROR") &&
                            criteria.getLevels().contains("WARN")
            ));
        }

        @Test
        @DisplayName("로그 tail API - 키워드 필터")
        void tailLog_WithKeywordFilter() throws Exception {
            // Given
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("entries", Collections.emptyList());
            result.put("filename", "maruweb.log");
            result.put("totalLines", 0);

            when(systemLogService.tailLog(any(LogSearchCriteria.class))).thenReturn(result);

            // When & Then
            mockMvc.perform(get("/trading/api/logs/tail")
                            .param("keyword", "exception")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            verify(systemLogService).tailLog(argThat(criteria ->
                    "exception".equals(criteria.getKeyword())
            ));
        }

        @Test
        @DisplayName("로그 tail API - 실패")
        void tailLog_Failure() throws Exception {
            // Given
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", "파일을 찾을 수 없습니다");

            when(systemLogService.tailLog(any(LogSearchCriteria.class))).thenReturn(result);

            // When & Then
            mockMvc.perform(get("/trading/api/logs/tail")
                            .param("filename", "nonexistent.log")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error").exists());
        }
    }

    // ==================== searchLogs API Tests ====================

    @Nested
    @DisplayName("searchLogs API - GET /trading/api/logs/search")
    class SearchLogsApiTests {

        @Test
        @DisplayName("로그 검색 API - 성공")
        void searchLogs_Success() throws Exception {
            // Given
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("entries", Arrays.asList(
                    createLogEntry(10, "ERROR", "Error found"),
                    createLogEntry(50, "ERROR", "Another error")
            ));
            result.put("filename", "maruweb.log");
            result.put("totalLines", 2);
            result.put("truncated", false);

            when(systemLogService.searchLogs(any(LogSearchCriteria.class))).thenReturn(result);

            // When & Then
            mockMvc.perform(get("/trading/api/logs/search")
                            .param("keyword", "error")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.entries").isArray())
                    .andExpect(jsonPath("$.truncated").value(false));
        }

        @Test
        @DisplayName("로그 검색 API - 시간 범위 필터")
        void searchLogs_WithTimeRange() throws Exception {
            // Given
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("entries", Collections.emptyList());
            result.put("filename", "maruweb.log");
            result.put("totalLines", 0);

            when(systemLogService.searchLogs(any(LogSearchCriteria.class))).thenReturn(result);

            // When & Then
            mockMvc.perform(get("/trading/api/logs/search")
                            .param("startTime", "2026-01-20 00:00:00")
                            .param("endTime", "2026-01-20 23:59:59")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            verify(systemLogService).searchLogs(argThat(criteria ->
                    criteria.getStartTime() != null && criteria.getEndTime() != null
            ));
        }

        @Test
        @DisplayName("로그 검색 API - 결과 제한됨 (truncated)")
        void searchLogs_Truncated() throws Exception {
            // Given
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("entries", Collections.emptyList());
            result.put("filename", "maruweb.log");
            result.put("totalLines", 5000);
            result.put("truncated", true);

            when(systemLogService.searchLogs(any(LogSearchCriteria.class))).thenReturn(result);

            // When & Then
            mockMvc.perform(get("/trading/api/logs/search")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.truncated").value(true));
        }

        @Test
        @DisplayName("로그 검색 API - 실패")
        void searchLogs_Failure() throws Exception {
            // Given
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", "검색 실패");

            when(systemLogService.searchLogs(any(LogSearchCriteria.class))).thenReturn(result);

            // When & Then
            mockMvc.perform(get("/trading/api/logs/search")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    // ==================== downloadLog API Tests ====================

    @Nested
    @DisplayName("downloadLog API - GET /trading/api/logs/download")
    class DownloadLogApiTests {

        @Test
        @DisplayName("로그 다운로드 API - 성공")
        void downloadLog_Success() throws Exception {
            // Given
            String content = "test log content";
            InputStreamResource resource = new InputStreamResource(
                    new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));

            when(systemLogService.getLogFileStream("app.log")).thenReturn(resource);
            when(systemLogService.getFileSize("app.log")).thenReturn((long) content.length());

            // When & Then
            mockMvc.perform(get("/trading/api/logs/download")
                            .param("filename", "app.log"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Disposition", "attachment; filename=\"app.log\""))
                    .andExpect(header().string("Content-Type", "application/octet-stream"))
                    .andExpect(content().string(content));
        }

        @Test
        @DisplayName("로그 다운로드 API - 파일 없음 (404)")
        void downloadLog_FileNotFound() throws Exception {
            // Given
            when(systemLogService.getLogFileStream("nonexistent.log"))
                    .thenThrow(new FileNotFoundException("파일 없음"));

            // When & Then
            mockMvc.perform(get("/trading/api/logs/download")
                            .param("filename", "nonexistent.log"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").exists());
        }

        @Test
        @DisplayName("로그 다운로드 API - 보안 위반 (403)")
        void downloadLog_SecurityViolation() throws Exception {
            // Given
            when(systemLogService.getLogFileStream("../etc/passwd"))
                    .thenThrow(new SecurityException("접근 거부"));

            // When & Then
            mockMvc.perform(get("/trading/api/logs/download")
                            .param("filename", "../etc/passwd"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error").value("접근이 거부되었습니다"));
        }

        @Test
        @DisplayName("로그 다운로드 API - filename 파라미터 필수")
        void downloadLog_MissingFilename() throws Exception {
            // When & Then
            mockMvc.perform(get("/trading/api/logs/download"))
                    .andExpect(status().isBadRequest());
        }
    }

    // ==================== Combined Filter Tests ====================

    @Nested
    @DisplayName("복합 필터 테스트")
    class CombinedFilterTests {

        @Test
        @DisplayName("tail API - 모든 필터 조합")
        void tailLog_AllFilters() throws Exception {
            // Given
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("entries", Collections.emptyList());
            result.put("filename", "app.log");
            result.put("totalLines", 0);

            when(systemLogService.tailLog(any(LogSearchCriteria.class))).thenReturn(result);

            // When & Then
            mockMvc.perform(get("/trading/api/logs/tail")
                            .param("filename", "app.log")
                            .param("lines", "50")
                            .param("levels", "ERROR", "WARN")
                            .param("keyword", "database")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            verify(systemLogService).tailLog(argThat(criteria ->
                    "app.log".equals(criteria.getFilename()) &&
                            Integer.valueOf(50).equals(criteria.getLines()) &&
                            criteria.getLevels().contains("ERROR") &&
                            criteria.getLevels().contains("WARN") &&
                            "database".equals(criteria.getKeyword())
            ));
        }

        @Test
        @DisplayName("search API - 모든 필터 조합")
        void searchLogs_AllFilters() throws Exception {
            // Given
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("entries", Collections.emptyList());
            result.put("filename", "app.log");
            result.put("totalLines", 0);

            when(systemLogService.searchLogs(any(LogSearchCriteria.class))).thenReturn(result);

            // When & Then
            mockMvc.perform(get("/trading/api/logs/search")
                            .param("filename", "app.log")
                            .param("levels", "ERROR")
                            .param("keyword", "exception")
                            .param("startTime", "2026-01-01 00:00:00")
                            .param("endTime", "2026-01-31 23:59:59")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            verify(systemLogService).searchLogs(argThat(criteria ->
                    "app.log".equals(criteria.getFilename()) &&
                            criteria.getLevels().contains("ERROR") &&
                            "exception".equals(criteria.getKeyword()) &&
                            "2026-01-01 00:00:00".equals(criteria.getStartTime()) &&
                            "2026-01-31 23:59:59".equals(criteria.getEndTime())
            ));
        }
    }
}
