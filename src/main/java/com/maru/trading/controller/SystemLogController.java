package com.maru.trading.controller;

import com.maru.trading.dto.LogFileInfo;
import com.maru.trading.dto.LogSearchCriteria;
import com.maru.trading.service.SystemLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/trading")
public class SystemLogController {

    private static final Logger logger = LoggerFactory.getLogger(SystemLogController.class);

    private final SystemLogService systemLogService;

    public SystemLogController(SystemLogService systemLogService) {
        this.systemLogService = systemLogService;
    }

    /**
     * 시스템 로그 뷰어 페이지
     */
    @GetMapping("/system-logs")
    public String systemLogsPage(Model model) {
        List<LogFileInfo> files = systemLogService.listLogFiles();
        model.addAttribute("logFiles", files);
        model.addAttribute("currentFile", systemLogService.getCurrentLogFile());
        model.addAttribute("logDirectory", systemLogService.getLogDirectory());
        return "trading/system-logs";
    }

    /**
     * 로그 파일 목록 API
     */
    @GetMapping("/api/logs/files")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> listLogFiles() {
        Map<String, Object> response = new HashMap<>();
        try {
            List<LogFileInfo> files = systemLogService.listLogFiles();
            response.put("success", true);
            response.put("files", files);
            response.put("currentFile", systemLogService.getCurrentLogFile());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("로그 파일 목록 조회 실패", e);
            response.put("success", false);
            response.put("error", "로그 파일 목록 조회 실패: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 로그 Tail API - 마지막 N줄 조회
     */
    @GetMapping("/api/logs/tail")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> tailLog(
            @RequestParam(required = false) String filename,
            @RequestParam(defaultValue = "100") Integer lines,
            @RequestParam(required = false) List<String> levels,
            @RequestParam(required = false) String keyword) {

        LogSearchCriteria criteria = new LogSearchCriteria();
        criteria.setFilename(filename);
        criteria.setLines(lines);
        criteria.setLevels(levels);
        criteria.setKeyword(keyword);

        Map<String, Object> result = systemLogService.tailLog(criteria);

        if ((Boolean) result.getOrDefault("success", false)) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
        }
    }

    /**
     * 로그 검색 API
     */
    @GetMapping("/api/logs/search")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> searchLogs(
            @RequestParam(required = false) String filename,
            @RequestParam(required = false) List<String> levels,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {

        LogSearchCriteria criteria = new LogSearchCriteria();
        criteria.setFilename(filename);
        criteria.setLevels(levels);
        criteria.setKeyword(keyword);
        criteria.setStartTime(startTime);
        criteria.setEndTime(endTime);

        Map<String, Object> result = systemLogService.searchLogs(criteria);

        if ((Boolean) result.getOrDefault("success", false)) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
        }
    }

    /**
     * 로그 파일 다운로드
     */
    @GetMapping("/api/logs/download")
    public ResponseEntity<?> downloadLog(@RequestParam String filename) {
        try {
            // 보안 검증은 서비스에서 수행
            InputStreamResource resource = systemLogService.getLogFileStream(filename);
            long fileSize = systemLogService.getFileSize(filename);

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(fileSize)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);

        } catch (SecurityException e) {
            logger.warn("로그 다운로드 보안 위반: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "접근이 거부되었습니다");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);

        } catch (IOException e) {
            logger.error("로그 다운로드 실패: {}", filename, e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "파일 다운로드 실패: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }
}
