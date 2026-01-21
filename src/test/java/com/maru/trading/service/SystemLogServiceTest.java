package com.maru.trading.service;

import com.maru.trading.dto.LogEntry;
import com.maru.trading.dto.LogFileInfo;
import com.maru.trading.dto.LogSearchCriteria;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.InputStreamResource;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import static org.assertj.core.api.Assertions.*;

@DisplayName("SystemLogService 단위 테스트")
class SystemLogServiceTest {

    private SystemLogService systemLogService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        systemLogService = new SystemLogService();
        ReflectionTestUtils.setField(systemLogService, "logDirectory", tempDir.toString());
        ReflectionTestUtils.setField(systemLogService, "currentLogFile", "maruweb.log");
        ReflectionTestUtils.setField(systemLogService, "maxTailLines", 1000);
        ReflectionTestUtils.setField(systemLogService, "maxSearchResults", 5000);
    }

    // ==================== Helper Methods ====================

    private void createLogFile(String filename, String... lines) throws IOException {
        Path filePath = tempDir.resolve(filename);
        Files.write(filePath, Arrays.asList(lines), StandardCharsets.UTF_8);
    }

    private void createGzipLogFile(String filename, String... lines) throws IOException {
        Path filePath = tempDir.resolve(filename);
        try (GZIPOutputStream gzos = new GZIPOutputStream(Files.newOutputStream(filePath));
             OutputStreamWriter writer = new OutputStreamWriter(gzos, StandardCharsets.UTF_8)) {
            for (String line : lines) {
                writer.write(line);
                writer.write("\n");
            }
        }
    }

    private String createLogLine(String level, String message) {
        return "2026-01-20 16:30:00.123 [main] " + level + " com.maru.test - " + message;
    }

    // ==================== listLogFiles Tests ====================

    @Nested
    @DisplayName("listLogFiles")
    class ListLogFilesTests {

        @Test
        @DisplayName("로그 파일 목록 조회 - 빈 디렉토리")
        void listLogFiles_EmptyDirectory() {
            // When
            List<LogFileInfo> result = systemLogService.listLogFiles();

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("로그 파일 목록 조회 - 로그 파일만 반환")
        void listLogFiles_OnlyLogFiles() throws IOException {
            // Given
            createLogFile("app.log", "test log");
            createLogFile("app.log.gz", "archived log");
            createLogFile("app.txt", "not a log file");

            // When
            List<LogFileInfo> result = systemLogService.listLogFiles();

            // Then
            assertThat(result).hasSize(2);
            assertThat(result).extracting(LogFileInfo::getFilename)
                    .containsExactlyInAnyOrder("app.log", "app.log.gz");
        }

        @Test
        @DisplayName("로그 파일 목록 조회 - 현재 파일 우선 정렬")
        void listLogFiles_CurrentFileFirst() throws IOException, InterruptedException {
            // Given
            createLogFile("maruweb.log", "current log");
            Thread.sleep(10); // 파일 수정 시간 차이를 위해
            createLogFile("app.log", "other log");

            // When
            List<LogFileInfo> result = systemLogService.listLogFiles();

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getFilename()).isEqualTo("maruweb.log");
            assertThat(result.get(0).isCurrent()).isTrue();
        }

        @Test
        @DisplayName("로그 파일 목록 조회 - 아카이브 파일 감지")
        void listLogFiles_DetectsArchived() throws IOException {
            // Given
            createLogFile("app.log", "log");
            createGzipLogFile("app.log.gz", "archived");

            // When
            List<LogFileInfo> result = systemLogService.listLogFiles();

            // Then
            LogFileInfo gzFile = result.stream()
                    .filter(f -> f.getFilename().equals("app.log.gz"))
                    .findFirst().orElseThrow();
            assertThat(gzFile.isArchived()).isTrue();

            LogFileInfo logFile = result.stream()
                    .filter(f -> f.getFilename().equals("app.log"))
                    .findFirst().orElseThrow();
            assertThat(logFile.isArchived()).isFalse();
        }

        @Test
        @DisplayName("로그 파일 목록 조회 - 디렉토리 없음")
        void listLogFiles_DirectoryNotExists() {
            // Given
            ReflectionTestUtils.setField(systemLogService, "logDirectory", "/non/existent/path");

            // When
            List<LogFileInfo> result = systemLogService.listLogFiles();

            // Then
            assertThat(result).isEmpty();
        }
    }

    // ==================== tailLog Tests ====================

    @Nested
    @DisplayName("tailLog")
    class TailLogTests {

        @Test
        @DisplayName("로그 tail 조회 - 성공")
        void tailLog_Success() throws IOException {
            // Given
            createLogFile("maruweb.log",
                    createLogLine("INFO", "First message"),
                    createLogLine("DEBUG", "Debug message"),
                    createLogLine("ERROR", "Error message"));

            LogSearchCriteria criteria = new LogSearchCriteria();
            criteria.setLines(10);

            // When
            Map<String, Object> result = systemLogService.tailLog(criteria);

            // Then
            assertThat(result.get("success")).isEqualTo(true);
            assertThat((List<?>) result.get("entries")).hasSize(3);
            assertThat(result.get("filename")).isEqualTo("maruweb.log");
        }

        @Test
        @DisplayName("로그 tail 조회 - 레벨 필터링")
        void tailLog_FilterByLevel() throws IOException {
            // Given
            createLogFile("maruweb.log",
                    createLogLine("INFO", "Info message"),
                    createLogLine("ERROR", "Error message"),
                    createLogLine("WARN", "Warn message"));

            LogSearchCriteria criteria = new LogSearchCriteria();
            criteria.setLevels(Arrays.asList("ERROR", "WARN"));
            criteria.setLines(10);

            // When
            Map<String, Object> result = systemLogService.tailLog(criteria);

            // Then
            assertThat(result.get("success")).isEqualTo(true);
            List<LogEntry> entries = (List<LogEntry>) result.get("entries");
            assertThat(entries).hasSize(2);
            assertThat(entries).extracting(LogEntry::getLevel)
                    .containsExactlyInAnyOrder("ERROR", "WARN");
        }

        @Test
        @DisplayName("로그 tail 조회 - 키워드 필터링")
        void tailLog_FilterByKeyword() throws IOException {
            // Given
            createLogFile("maruweb.log",
                    createLogLine("INFO", "User login successful"),
                    createLogLine("ERROR", "Database connection failed"),
                    createLogLine("INFO", "User logout"));

            LogSearchCriteria criteria = new LogSearchCriteria();
            criteria.setKeyword("User");
            criteria.setLines(10);

            // When
            Map<String, Object> result = systemLogService.tailLog(criteria);

            // Then
            assertThat(result.get("success")).isEqualTo(true);
            List<LogEntry> entries = (List<LogEntry>) result.get("entries");
            assertThat(entries).hasSize(2);
        }

        @Test
        @DisplayName("로그 tail 조회 - 파일 없음")
        void tailLog_FileNotFound() {
            // Given
            LogSearchCriteria criteria = new LogSearchCriteria();
            criteria.setFilename("nonexistent.log");

            // When
            Map<String, Object> result = systemLogService.tailLog(criteria);

            // Then
            assertThat(result.get("success")).isEqualTo(false);
            assertThat(result.get("error")).isNotNull();
        }

        @Test
        @DisplayName("로그 tail 조회 - 빈 파일")
        void tailLog_EmptyFile() throws IOException {
            // Given
            createLogFile("maruweb.log");

            LogSearchCriteria criteria = new LogSearchCriteria();

            // When
            Map<String, Object> result = systemLogService.tailLog(criteria);

            // Then
            assertThat(result.get("success")).isEqualTo(true);
            assertThat((List<?>) result.get("entries")).isEmpty();
        }

        @Test
        @DisplayName("로그 tail 조회 - 특정 파일 지정")
        void tailLog_SpecificFile() throws IOException {
            // Given
            createLogFile("app.log", createLogLine("INFO", "App log"));
            createLogFile("maruweb.log", createLogLine("INFO", "Maruweb log"));

            LogSearchCriteria criteria = new LogSearchCriteria();
            criteria.setFilename("app.log");

            // When
            Map<String, Object> result = systemLogService.tailLog(criteria);

            // Then
            assertThat(result.get("success")).isEqualTo(true);
            assertThat(result.get("filename")).isEqualTo("app.log");
            List<LogEntry> entries = (List<LogEntry>) result.get("entries");
            assertThat(entries.get(0).getMessage()).contains("App log");
        }

        @Test
        @DisplayName("로그 tail 조회 - maxTailLines 제한")
        void tailLog_RespectMaxLines() throws IOException {
            // Given
            ReflectionTestUtils.setField(systemLogService, "maxTailLines", 5);
            String[] lines = new String[10];
            for (int i = 0; i < 10; i++) {
                lines[i] = createLogLine("INFO", "Message " + i);
            }
            createLogFile("maruweb.log", lines);

            LogSearchCriteria criteria = new LogSearchCriteria();
            criteria.setLines(100); // 100줄 요청해도 maxTailLines로 제한

            // When
            Map<String, Object> result = systemLogService.tailLog(criteria);

            // Then
            assertThat(result.get("success")).isEqualTo(true);
            List<LogEntry> entries = (List<LogEntry>) result.get("entries");
            assertThat(entries.size()).isLessThanOrEqualTo(5);
        }
    }

    // ==================== searchLogs Tests ====================

    @Nested
    @DisplayName("searchLogs")
    class SearchLogsTests {

        @Test
        @DisplayName("로그 검색 - 성공")
        void searchLogs_Success() throws IOException {
            // Given
            createLogFile("maruweb.log",
                    createLogLine("INFO", "First message"),
                    createLogLine("ERROR", "Error occurred"),
                    createLogLine("INFO", "Last message"));

            LogSearchCriteria criteria = new LogSearchCriteria();

            // When
            Map<String, Object> result = systemLogService.searchLogs(criteria);

            // Then
            assertThat(result.get("success")).isEqualTo(true);
            List<LogEntry> entries = (List<LogEntry>) result.get("entries");
            assertThat(entries).hasSize(3);
        }

        @Test
        @DisplayName("로그 검색 - 레벨 필터링")
        void searchLogs_FilterByLevel() throws IOException {
            // Given
            createLogFile("maruweb.log",
                    createLogLine("INFO", "Info message"),
                    createLogLine("ERROR", "Error message"),
                    createLogLine("DEBUG", "Debug message"));

            LogSearchCriteria criteria = new LogSearchCriteria();
            criteria.setLevels(Arrays.asList("ERROR"));

            // When
            Map<String, Object> result = systemLogService.searchLogs(criteria);

            // Then
            assertThat(result.get("success")).isEqualTo(true);
            List<LogEntry> entries = (List<LogEntry>) result.get("entries");
            assertThat(entries).hasSize(1);
            assertThat(entries.get(0).getLevel()).isEqualTo("ERROR");
        }

        @Test
        @DisplayName("로그 검색 - 키워드 필터링 (대소문자 무시)")
        void searchLogs_KeywordCaseInsensitive() throws IOException {
            // Given
            createLogFile("maruweb.log",
                    createLogLine("INFO", "User LOGIN successful"),
                    createLogLine("INFO", "login failed"),
                    createLogLine("INFO", "Other event"));

            LogSearchCriteria criteria = new LogSearchCriteria();
            criteria.setKeyword("LOGIN");

            // When
            Map<String, Object> result = systemLogService.searchLogs(criteria);

            // Then
            assertThat(result.get("success")).isEqualTo(true);
            List<LogEntry> entries = (List<LogEntry>) result.get("entries");
            assertThat(entries).hasSize(2);
        }

        @Test
        @DisplayName("로그 검색 - 파일 없음")
        void searchLogs_FileNotFound() {
            // Given
            LogSearchCriteria criteria = new LogSearchCriteria();
            criteria.setFilename("nonexistent.log");

            // When
            Map<String, Object> result = systemLogService.searchLogs(criteria);

            // Then
            assertThat(result.get("success")).isEqualTo(false);
            assertThat(result.get("error")).isNotNull();
        }

        @Test
        @DisplayName("로그 검색 - maxSearchResults 제한")
        void searchLogs_Truncated() throws IOException {
            // Given
            ReflectionTestUtils.setField(systemLogService, "maxSearchResults", 3);
            String[] lines = new String[10];
            for (int i = 0; i < 10; i++) {
                lines[i] = createLogLine("INFO", "Message " + i);
            }
            createLogFile("maruweb.log", lines);

            LogSearchCriteria criteria = new LogSearchCriteria();

            // When
            Map<String, Object> result = systemLogService.searchLogs(criteria);

            // Then
            assertThat(result.get("success")).isEqualTo(true);
            List<LogEntry> entries = (List<LogEntry>) result.get("entries");
            assertThat(entries).hasSize(3);
            assertThat(result.get("truncated")).isEqualTo(true);
        }

        @Test
        @DisplayName("로그 검색 - GZIP 파일")
        void searchLogs_GzipFile() throws IOException {
            // Given
            createGzipLogFile("app.log.gz",
                    createLogLine("INFO", "Archived message"),
                    createLogLine("ERROR", "Archived error"));

            LogSearchCriteria criteria = new LogSearchCriteria();
            criteria.setFilename("app.log.gz");

            // When
            Map<String, Object> result = systemLogService.searchLogs(criteria);

            // Then
            assertThat(result.get("success")).isEqualTo(true);
            List<LogEntry> entries = (List<LogEntry>) result.get("entries");
            assertThat(entries).hasSize(2);
        }
    }

    // ==================== getLogFileStream Tests ====================

    @Nested
    @DisplayName("getLogFileStream")
    class GetLogFileStreamTests {

        @Test
        @DisplayName("로그 파일 스트림 조회 - 성공")
        void getLogFileStream_Success() throws IOException {
            // Given
            createLogFile("app.log", "test content");

            // When
            InputStreamResource resource = systemLogService.getLogFileStream("app.log");

            // Then
            assertThat(resource).isNotNull();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream()))) {
                String line = reader.readLine();
                assertThat(line).isEqualTo("test content");
            }
        }

        @Test
        @DisplayName("로그 파일 스트림 조회 - 파일 없음")
        void getLogFileStream_FileNotFound() {
            // When/Then
            assertThatThrownBy(() -> systemLogService.getLogFileStream("nonexistent.log"))
                    .isInstanceOf(FileNotFoundException.class);
        }
    }

    // ==================== getFileSize Tests ====================

    @Nested
    @DisplayName("getFileSize")
    class GetFileSizeTests {

        @Test
        @DisplayName("파일 크기 조회 - 성공")
        void getFileSize_Success() throws IOException {
            // Given
            String content = "test content 123";
            createLogFile("app.log", content);

            // When
            long size = systemLogService.getFileSize("app.log");

            // Then
            assertThat(size).isGreaterThan(0);
        }

        @Test
        @DisplayName("파일 크기 조회 - 파일 없음")
        void getFileSize_FileNotFound() {
            // When/Then
            assertThatThrownBy(() -> systemLogService.getFileSize("nonexistent.log"))
                    .isInstanceOf(IOException.class);
        }
    }

    // ==================== Security Tests ====================

    @Nested
    @DisplayName("보안 검증")
    class SecurityTests {

        @Test
        @DisplayName("경로 조작 차단 - ..")
        void blockPathTraversal_DoubleDot() {
            // Given
            LogSearchCriteria criteria = new LogSearchCriteria();
            criteria.setFilename("../etc/passwd");

            // When
            Map<String, Object> result = systemLogService.tailLog(criteria);

            // Then
            assertThat(result.get("success")).isEqualTo(false);
            assertThat(result.get("error")).asString().contains("접근이 거부");
        }

        @Test
        @DisplayName("경로 조작 차단 - 슬래시")
        void blockPathTraversal_Slash() {
            // Given
            LogSearchCriteria criteria = new LogSearchCriteria();
            criteria.setFilename("/etc/passwd");

            // When
            Map<String, Object> result = systemLogService.tailLog(criteria);

            // Then
            assertThat(result.get("success")).isEqualTo(false);
            assertThat(result.get("error")).asString().contains("접근이 거부");
        }

        @Test
        @DisplayName("경로 조작 차단 - 백슬래시")
        void blockPathTraversal_Backslash() {
            // Given
            LogSearchCriteria criteria = new LogSearchCriteria();
            criteria.setFilename("..\\..\\etc\\passwd");

            // When
            Map<String, Object> result = systemLogService.tailLog(criteria);

            // Then
            assertThat(result.get("success")).isEqualTo(false);
            assertThat(result.get("error")).asString().contains("접근이 거부");
        }

        @Test
        @DisplayName("스트림 조회 시 경로 조작 차단")
        void blockPathTraversal_GetStream() {
            // When/Then
            assertThatThrownBy(() -> systemLogService.getLogFileStream("../etc/passwd"))
                    .isInstanceOf(SecurityException.class);
        }

        @Test
        @DisplayName("파일 크기 조회 시 경로 조작 차단")
        void blockPathTraversal_GetSize() {
            // When/Then
            assertThatThrownBy(() -> systemLogService.getFileSize("../etc/passwd"))
                    .isInstanceOf(SecurityException.class);
        }
    }

    // ==================== Log Parsing Tests ====================

    @Nested
    @DisplayName("로그 파싱")
    class LogParsingTests {

        @Test
        @DisplayName("정상 로그 라인 파싱")
        void parseNormalLogLine() throws IOException {
            // Given
            String logLine = "2026-01-20 16:30:00.123 [http-nio-8080-exec-1] INFO com.maru.controller.TestController - Request processed";
            createLogFile("maruweb.log", logLine);

            LogSearchCriteria criteria = new LogSearchCriteria();

            // When
            Map<String, Object> result = systemLogService.searchLogs(criteria);

            // Then
            assertThat(result.get("success")).isEqualTo(true);
            List<LogEntry> entries = (List<LogEntry>) result.get("entries");
            LogEntry entry = entries.get(0);

            assertThat(entry.getTimestamp()).isEqualTo("2026-01-20 16:30:00.123");
            assertThat(entry.getThread()).isEqualTo("http-nio-8080-exec-1");
            assertThat(entry.getLevel()).isEqualTo("INFO");
            assertThat(entry.getLogger()).isEqualTo("com.maru.controller.TestController");
            assertThat(entry.getMessage()).isEqualTo("Request processed");
        }

        @Test
        @DisplayName("비정상 로그 라인 파싱 (스택 트레이스)")
        void parseStackTrace() throws IOException {
            // Given
            String stackLine = "    at com.maru.service.TestService.method(TestService.java:42)";
            createLogFile("maruweb.log", stackLine);

            LogSearchCriteria criteria = new LogSearchCriteria();

            // When
            Map<String, Object> result = systemLogService.searchLogs(criteria);

            // Then
            assertThat(result.get("success")).isEqualTo(true);
            List<LogEntry> entries = (List<LogEntry>) result.get("entries");
            LogEntry entry = entries.get(0);

            assertThat(entry.getLevel()).isEqualTo("TRACE"); // 패턴 불일치 시 TRACE
            assertThat(entry.getMessage()).isEqualTo(stackLine);
        }
    }

    // ==================== Getter Tests ====================

    @Nested
    @DisplayName("Getter 메서드")
    class GetterTests {

        @Test
        @DisplayName("getLogDirectory 반환값 확인")
        void getLogDirectory() {
            assertThat(systemLogService.getLogDirectory()).isEqualTo(tempDir.toString());
        }

        @Test
        @DisplayName("getCurrentLogFile 반환값 확인")
        void getCurrentLogFile() {
            assertThat(systemLogService.getCurrentLogFile()).isEqualTo("maruweb.log");
        }
    }
}
