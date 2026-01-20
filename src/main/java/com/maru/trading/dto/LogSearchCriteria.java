package com.maru.trading.dto;

import java.util.List;

/**
 * 로그 검색 필터 조건 DTO
 */
public class LogSearchCriteria {
    private String filename;
    private List<String> levels;
    private String keyword;
    private Integer lines;
    private String startTime;
    private String endTime;

    public LogSearchCriteria() {
        this.lines = 100;
    }

    // Getters and Setters
    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public List<String> getLevels() {
        return levels;
    }

    public void setLevels(List<String> levels) {
        this.levels = levels;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Integer getLines() {
        return lines;
    }

    public void setLines(Integer lines) {
        this.lines = lines;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public boolean hasLevelFilter() {
        return levels != null && !levels.isEmpty();
    }

    public boolean hasKeywordFilter() {
        return keyword != null && !keyword.trim().isEmpty();
    }
}
