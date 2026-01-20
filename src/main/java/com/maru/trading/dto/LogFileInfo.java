package com.maru.trading.dto;

import java.time.LocalDateTime;

/**
 * 로그 파일 메타데이터 DTO
 */
public class LogFileInfo {
    private String filename;
    private long size;
    private String sizeFormatted;
    private LocalDateTime lastModified;
    private boolean isArchived;
    private boolean isCurrent;

    public LogFileInfo() {}

    public LogFileInfo(String filename, long size, LocalDateTime lastModified) {
        this.filename = filename;
        this.size = size;
        this.lastModified = lastModified;
        this.sizeFormatted = formatSize(size);
        this.isArchived = filename.endsWith(".gz") || filename.endsWith(".zip");
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    // Getters and Setters
    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
        this.sizeFormatted = formatSize(size);
    }

    public String getSizeFormatted() {
        return sizeFormatted;
    }

    public void setSizeFormatted(String sizeFormatted) {
        this.sizeFormatted = sizeFormatted;
    }

    public LocalDateTime getLastModified() {
        return lastModified;
    }

    public void setLastModified(LocalDateTime lastModified) {
        this.lastModified = lastModified;
    }

    public boolean isArchived() {
        return isArchived;
    }

    public void setArchived(boolean archived) {
        isArchived = archived;
    }

    public boolean isCurrent() {
        return isCurrent;
    }

    public void setCurrent(boolean current) {
        isCurrent = current;
    }
}
