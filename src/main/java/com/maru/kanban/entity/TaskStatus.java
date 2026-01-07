package com.maru.kanban.entity;

import java.util.Arrays;
import java.util.List;

public enum TaskStatus {
    REGISTERED("등록", "Tasks newly created"),
    WAITING_RESPONSE("응답대기중", "Waiting for external response/input"),
    IN_PROGRESS("진행중", "Currently being worked on"),
    COMPLETED("완료", "Task completed");

    private final String koreanName;
    private final String description;

    TaskStatus(String koreanName, String description) {
        this.koreanName = koreanName;
        this.description = description;
    }

    public String getKoreanName() {
        return koreanName;
    }

    public String getDescription() {
        return description;
    }

    public static List<TaskStatus> getAllStatuses() {
        return Arrays.asList(values());
    }
}
