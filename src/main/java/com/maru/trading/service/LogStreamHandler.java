package com.maru.trading.service;

import com.maru.trading.dto.LogEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 로그 파일 실시간 스트리밍 핸들러
 * WatchService를 사용하여 로그 파일 변경을 감지하고 WebSocket으로 브로드캐스트
 */
@Service
public class LogStreamHandler {

    private static final Logger logger = LoggerFactory.getLogger(LogStreamHandler.class);

    private static final Pattern LOG_PATTERN = Pattern.compile(
            "^(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3})\\s+\\[([^\\]]+)\\]\\s+(\\w+)\\s+(\\S+)\\s+-\\s+(.*)$"
    );

    @Value("${system.log.directory:/var/logs/trading}")
    private String logDirectory;

    @Value("${system.log.current-file:maruweb.log}")
    private String currentLogFile;

    private final SimpMessagingTemplate messagingTemplate;
    private final Set<String> activeSubscriptions = ConcurrentHashMap.newKeySet();
    private final Map<String, Long> filePositions = new ConcurrentHashMap<>();

    private WatchService watchService;
    private ExecutorService watchExecutor;
    private ScheduledExecutorService pollingExecutor;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public LogStreamHandler(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @PostConstruct
    public void init() {
        try {
            Path logDir = Paths.get(logDirectory);
            if (Files.exists(logDir) && Files.isDirectory(logDir)) {
                watchService = FileSystems.getDefault().newWatchService();
                logDir.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);

                watchExecutor = Executors.newSingleThreadExecutor(r -> {
                    Thread t = new Thread(r, "log-watch-thread");
                    t.setDaemon(true);
                    return t;
                });

                pollingExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
                    Thread t = new Thread(r, "log-poll-thread");
                    t.setDaemon(true);
                    return t;
                });

                running.set(true);
                startWatching();
                startPolling();
                logger.info("LogStreamHandler 초기화 완료: {}", logDirectory);
            } else {
                logger.warn("로그 디렉토리가 존재하지 않습니다: {}", logDirectory);
            }
        } catch (IOException e) {
            logger.error("LogStreamHandler 초기화 실패", e);
        }
    }

    @PreDestroy
    public void destroy() {
        running.set(false);

        if (watchExecutor != null) {
            watchExecutor.shutdownNow();
        }
        if (pollingExecutor != null) {
            pollingExecutor.shutdownNow();
        }
        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException e) {
                logger.error("WatchService 종료 실패", e);
            }
        }
    }

    /**
     * 구독 시작
     */
    public void subscribe(String sessionId) {
        activeSubscriptions.add(sessionId);
        logger.debug("로그 스트림 구독 시작: {}", sessionId);

        // 현재 파일 위치 초기화 (끝에서 시작)
        try {
            Path filePath = Paths.get(logDirectory, currentLogFile);
            if (Files.exists(filePath)) {
                filePositions.put(currentLogFile, Files.size(filePath));
            }
        } catch (IOException e) {
            logger.error("파일 위치 초기화 실패", e);
        }
    }

    /**
     * 구독 해제
     */
    public void unsubscribe(String sessionId) {
        activeSubscriptions.remove(sessionId);
        logger.debug("로그 스트림 구독 해제: {}", sessionId);
    }

    /**
     * 파일 변경 감지 시작
     */
    private void startWatching() {
        watchExecutor.submit(() -> {
            while (running.get()) {
                try {
                    WatchKey key = watchService.poll(1, TimeUnit.SECONDS);
                    if (key != null) {
                        for (WatchEvent<?> event : key.pollEvents()) {
                            if (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
                                Path changed = (Path) event.context();
                                if (changed.toString().equals(currentLogFile)) {
                                    readAndBroadcastNewLines();
                                }
                            }
                        }
                        key.reset();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    logger.error("파일 감시 중 오류", e);
                }
            }
        });
    }

    /**
     * 주기적 폴링 (WatchService 보조)
     */
    private void startPolling() {
        pollingExecutor.scheduleWithFixedDelay(() -> {
            if (!activeSubscriptions.isEmpty()) {
                readAndBroadcastNewLines();
            }
        }, 5, 2, TimeUnit.SECONDS);
    }

    /**
     * 새로운 로그 라인을 읽고 브로드캐스트
     */
    private void readAndBroadcastNewLines() {
        if (activeSubscriptions.isEmpty()) {
            return;
        }

        try {
            Path filePath = Paths.get(logDirectory, currentLogFile);
            if (!Files.exists(filePath)) {
                return;
            }

            long currentSize = Files.size(filePath);
            long lastPosition = filePositions.getOrDefault(currentLogFile, currentSize);

            // 파일이 작아졌으면 (로테이션) 처음부터 읽기
            if (currentSize < lastPosition) {
                lastPosition = 0;
            }

            if (currentSize > lastPosition) {
                List<LogEntry> newEntries = readNewLines(filePath, lastPosition);
                if (!newEntries.isEmpty()) {
                    broadcastLogEntries(newEntries);
                    filePositions.put(currentLogFile, currentSize);
                }
            }
        } catch (Exception e) {
            logger.error("새 로그 라인 읽기 실패", e);
        }
    }

    /**
     * 지정된 위치부터 새로운 라인 읽기
     */
    private List<LogEntry> readNewLines(Path filePath, long startPosition) throws IOException {
        List<LogEntry> entries = new ArrayList<>();

        try (RandomAccessFile raf = new RandomAccessFile(filePath.toFile(), "r")) {
            raf.seek(startPosition);

            String line;
            int lineNumber = 0;
            while ((line = raf.readLine()) != null) {
                if (line.isEmpty()) continue;

                // ISO-8859-1 -> UTF-8 변환 (RandomAccessFile은 ISO-8859-1로 읽음)
                line = new String(line.getBytes("ISO-8859-1"), "UTF-8");

                lineNumber++;
                LogEntry entry = parseLine(line, lineNumber);
                entries.add(entry);

                // 한 번에 너무 많이 보내지 않도록 제한
                if (entries.size() >= 100) {
                    break;
                }
            }
        }

        return entries;
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
            entry.setLevel("TRACE");
            entry.setMessage(line);
        }

        return entry;
    }

    /**
     * WebSocket으로 로그 엔트리 브로드캐스트
     */
    private void broadcastLogEntries(List<LogEntry> entries) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "LOG_ENTRIES");
        message.put("entries", entries);
        message.put("filename", currentLogFile);
        message.put("timestamp", System.currentTimeMillis());

        messagingTemplate.convertAndSend("/topic/logs", message);
        logger.trace("로그 브로드캐스트: {} entries", entries.size());
    }

    public boolean hasActiveSubscriptions() {
        return !activeSubscriptions.isEmpty();
    }

    public int getSubscriptionCount() {
        return activeSubscriptions.size();
    }
}
