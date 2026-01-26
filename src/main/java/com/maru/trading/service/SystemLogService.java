package com.maru.trading.service;

import com.maru.trading.dto.LogEntry;
import com.maru.trading.dto.LogFileInfo;
import com.maru.trading.dto.LogSearchCriteria;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

@Service
public class SystemLogService {

    private static final Logger logger = LoggerFactory.getLogger(SystemLogService.class);

    // 로그 라인 파싱 패턴: 2026-01-20 16:30:00.123 [thread-name] LEVEL logger.name - message
    private static final Pattern LOG_PATTERN = Pattern.compile(
            "^(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3})\\s+\\[([^\\]]+)\\]\\s+(\\w+)\\s+(\\S+)\\s+-\\s+(.*)$"
    );

    @Value("${system.log.directory:/var/logs/trading}")
    private String logDirectory;

    @Value("${system.log.current-file:maruweb.log}")
    private String currentLogFile;

    @Value("${system.log.max-tail-lines:1000}")
    private int maxTailLines;

    @Value("${system.log.max-search-results:5000}")
    private int maxSearchResults;

    /**
     * 로그 디렉토리의 모든 로그 파일 목록 반환
     */
    public List<LogFileInfo> listLogFiles() {
        List<LogFileInfo> files = new ArrayList<>();
        Path logDir = Paths.get(logDirectory);

        if (!Files.exists(logDir)) {
            logger.warn("로그 디렉토리가 존재하지 않습니다: {}", logDirectory);
            return files;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(logDir,
                path -> {
                    String name = path.getFileName().toString();
                    return name.endsWith(".log") || name.endsWith(".gz") || name.endsWith(".zip");
                })) {

            for (Path path : stream) {
                try {
                    LogFileInfo info = new LogFileInfo(
                            path.getFileName().toString(),
                            Files.size(path),
                            LocalDateTime.ofInstant(
                                    Files.getLastModifiedTime(path).toInstant(),
                                    ZoneId.systemDefault()
                            )
                    );
                    info.setCurrent(path.getFileName().toString().equals(currentLogFile));
                    files.add(info);
                } catch (IOException e) {
                    logger.error("파일 정보 읽기 실패: {}", path, e);
                }
            }
        } catch (IOException e) {
            logger.error("로그 디렉토리 읽기 실패: {}", logDirectory, e);
        }

        // 현재 파일 먼저, 그 다음 최신 파일 순으로 정렬
        files.sort((a, b) -> {
            if (a.isCurrent()) return -1;
            if (b.isCurrent()) return 1;
            return b.getLastModified().compareTo(a.getLastModified());
        });

        return files;
    }

    /**
     * 로그 파일의 마지막 N줄 조회 (필터링 포함)
     */
    public Map<String, Object> tailLog(LogSearchCriteria criteria) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);

        String filename = criteria.getFilename();
        if (filename == null || filename.isEmpty()) {
            filename = currentLogFile;
        }

        try {
            Path filePath = resolveAndValidatePath(filename);

            if (!Files.exists(filePath)) {
                result.put("error", "파일을 찾을 수 없습니다: " + filename);
                return result;
            }

            int requestedLines = criteria.getLines() != null ?
                    Math.min(criteria.getLines(), maxTailLines) : 100;

            List<String> lines = readLastLines(filePath, requestedLines * 3); // 필터링을 위해 더 많이 읽기
            List<LogEntry> entries = parseAndFilterLines(lines, criteria, requestedLines);

            result.put("success", true);
            result.put("entries", entries);
            result.put("filename", filename);
            result.put("totalLines", entries.size());

        } catch (SecurityException e) {
            logger.warn("보안 위반 시도: {}", e.getMessage());
            result.put("error", "접근이 거부되었습니다");
        } catch (Exception e) {
            logger.error("로그 읽기 실패: {}", e.getMessage(), e);
            result.put("error", "로그 읽기 실패: " + e.getMessage());
        }

        return result;
    }

    /**
     * 로그 검색 (전체 파일 검색)
     */
    public Map<String, Object> searchLogs(LogSearchCriteria criteria) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);

        String filename = criteria.getFilename();
        if (filename == null || filename.isEmpty()) {
            filename = currentLogFile;
        }

        try {
            Path filePath = resolveAndValidatePath(filename);

            if (!Files.exists(filePath)) {
                result.put("error", "파일을 찾을 수 없습니다: " + filename);
                return result;
            }

            List<LogEntry> entries = new ArrayList<>();
            int lineNumber = 0;

            try (BufferedReader reader = createReader(filePath)) {
                String line;
                while ((line = reader.readLine()) != null && entries.size() < maxSearchResults) {
                    lineNumber++;
                    LogEntry entry = parseLine(line, lineNumber);
                    if (matchesCriteria(entry, criteria)) {
                        entries.add(entry);
                    }
                }
            }

            result.put("success", true);
            result.put("entries", entries);
            result.put("filename", filename);
            result.put("totalLines", entries.size());
            result.put("truncated", entries.size() >= maxSearchResults);

        } catch (SecurityException e) {
            logger.warn("보안 위반 시도: {}", e.getMessage());
            result.put("error", "접근이 거부되었습니다");
        } catch (Exception e) {
            logger.error("로그 검색 실패: {}", e.getMessage(), e);
            result.put("error", "로그 검색 실패: " + e.getMessage());
        }

        return result;
    }

    /**
     * 로그 파일 다운로드용 리소스 반환
     * UrlResource를 사용하여 Spring이 리소스 수명 주기를 관리하도록 함
     */
    public Resource getLogFileResource(String filename) throws IOException {
        Path filePath = resolveAndValidatePath(filename);

        if (!Files.exists(filePath)) {
            throw new FileNotFoundException("파일을 찾을 수 없습니다: " + filename);
        }

        Resource resource = new UrlResource(filePath.toUri());
        if (!resource.exists() || !resource.isReadable()) {
            throw new FileNotFoundException("파일을 읽을 수 없습니다: " + filename);
        }

        return resource;
    }

    /**
     * 로그 파일 크기 반환
     */
    public long getFileSize(String filename) throws IOException {
        Path filePath = resolveAndValidatePath(filename);
        return Files.size(filePath);
    }

    /**
     * 경로 조작 방지를 위한 보안 검증
     */
    private Path resolveAndValidatePath(String filename) {
        // 경로 조작 문자 검사
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            throw new SecurityException("유효하지 않은 파일명: " + filename);
        }

        Path basePath = Paths.get(logDirectory).toAbsolutePath().normalize();
        Path filePath = basePath.resolve(filename).normalize();

        // 기준 경로 밖으로 벗어나는지 확인
        if (!filePath.startsWith(basePath)) {
            throw new SecurityException("허용되지 않은 경로 접근 시도");
        }

        return filePath;
    }

    /**
     * 파일의 마지막 N줄 읽기
     */
    private List<String> readLastLines(Path filePath, int numLines) throws IOException {
        String filename = filePath.getFileName().toString();

        if (filename.endsWith(".gz")) {
            return readLastLinesFromGzip(filePath, numLines);
        }

        // 일반 파일: RandomAccessFile로 효율적으로 읽기
        List<String> lines = new ArrayList<>();

        try (RandomAccessFile raf = new RandomAccessFile(filePath.toFile(), "r")) {
            long fileLength = raf.length();
            if (fileLength == 0) {
                return lines;
            }

            // 파일 끝에서부터 역순으로 읽기
            StringBuilder sb = new StringBuilder();
            long pos = fileLength - 1;

            while (pos >= 0 && lines.size() < numLines) {
                raf.seek(pos);
                int ch = raf.read();

                if (ch == '\n') {
                    if (sb.length() > 0) {
                        lines.add(0, sb.reverse().toString());
                        sb = new StringBuilder();
                    }
                } else if (ch != '\r') {
                    sb.append((char) ch);
                }
                pos--;
            }

            // 마지막 라인 처리
            if (sb.length() > 0) {
                lines.add(0, sb.reverse().toString());
            }
        }

        return lines;
    }

    /**
     * GZIP 파일에서 마지막 N줄 읽기
     */
    private List<String> readLastLinesFromGzip(Path filePath, int numLines) throws IOException {
        List<String> allLines = new ArrayList<>();

        try (GZIPInputStream gis = new GZIPInputStream(Files.newInputStream(filePath));
             BufferedReader reader = new BufferedReader(new InputStreamReader(gis, StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                allLines.add(line);
                // 메모리 보호: 너무 많은 라인은 앞에서 제거
                if (allLines.size() > numLines * 2) {
                    allLines.subList(0, allLines.size() - numLines).clear();
                }
            }
        }

        int startIndex = Math.max(0, allLines.size() - numLines);
        return new ArrayList<>(allLines.subList(startIndex, allLines.size()));
    }

    /**
     * 파일 타입에 따른 BufferedReader 생성
     */
    private BufferedReader createReader(Path filePath) throws IOException {
        String filename = filePath.getFileName().toString();

        if (filename.endsWith(".gz")) {
            return createGzipReader(filePath);
        }

        return Files.newBufferedReader(filePath, StandardCharsets.UTF_8);
    }

    /**
     * GZIP 파일용 BufferedReader 생성 (리소스 누수 방지)
     * 중첩 스트림 생성 중 예외 발생 시 이미 열린 스트림을 안전하게 닫음
     */
    private BufferedReader createGzipReader(Path filePath) throws IOException {
        InputStream rawStream = Files.newInputStream(filePath);
        try {
            GZIPInputStream gzipStream = new GZIPInputStream(rawStream);
            return new BufferedReader(new InputStreamReader(gzipStream, StandardCharsets.UTF_8));
        } catch (IOException e) {
            rawStream.close();
            throw e;
        }
    }

    /**
     * 로그 라인 파싱
     */
    private LogEntry parseLine(String line, int lineNumber) {
        LogEntry entry = new LogEntry(line, lineNumber);

        Matcher matcher = LOG_PATTERN.matcher(line);
        if (matcher.matches()) {
            entry.setTimestamp(matcher.group(1));
            entry.setThread(matcher.group(2));
            entry.setLevel(matcher.group(3));
            entry.setLogger(matcher.group(4));
            entry.setMessage(matcher.group(5));
        } else {
            // 패턴에 맞지 않는 경우 (스택 트레이스 등)
            entry.setLevel("TRACE");
            entry.setMessage(line);
        }

        return entry;
    }

    /**
     * 라인 목록을 파싱하고 필터링
     */
    private List<LogEntry> parseAndFilterLines(List<String> lines, LogSearchCriteria criteria, int maxResults) {
        List<LogEntry> entries = new ArrayList<>();
        int lineNumber = 0;

        for (String line : lines) {
            lineNumber++;
            LogEntry entry = parseLine(line, lineNumber);
            if (matchesCriteria(entry, criteria)) {
                entries.add(entry);
            }
        }

        // 최신 로그가 아래에 오도록 유지
        if (entries.size() > maxResults) {
            return entries.subList(entries.size() - maxResults, entries.size());
        }

        return entries;
    }

    /**
     * 검색 조건에 맞는지 확인
     */
    private boolean matchesCriteria(LogEntry entry, LogSearchCriteria criteria) {
        // 레벨 필터
        if (criteria.hasLevelFilter()) {
            if (entry.getLevel() == null || !criteria.getLevels().contains(entry.getLevel())) {
                return false;
            }
        }

        // 키워드 필터
        if (criteria.hasKeywordFilter()) {
            String keyword = criteria.getKeyword().toLowerCase();
            String searchText = entry.getRawLine() != null ?
                    entry.getRawLine().toLowerCase() : "";
            if (!searchText.contains(keyword)) {
                return false;
            }
        }

        return true;
    }

    public String getLogDirectory() {
        return logDirectory;
    }

    public String getCurrentLogFile() {
        return currentLogFile;
    }
}
