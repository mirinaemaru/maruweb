package com.maru.trading.dto;

import java.time.LocalDateTime;

/**
 * 파싱된 로그 엔트리를 나타내는 DTO
 */
public class LogEntry {
    private String timestamp;
    private String thread;
    private String level;
    private String logger;
    private String message;
    private int lineNumber;
    private String rawLine;

    public LogEntry() {}

    public LogEntry(String rawLine, int lineNumber) {
        this.rawLine = rawLine;
        this.lineNumber = lineNumber;
    }

    // Getters and Setters
    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getThread() {
        return thread;
    }

    public void setThread(String thread) {
        this.thread = thread;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getLogger() {
        return logger;
    }

    public void setLogger(String logger) {
        this.logger = logger;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public String getRawLine() {
        return rawLine;
    }

    public void setRawLine(String rawLine) {
        this.rawLine = rawLine;
    }
}
