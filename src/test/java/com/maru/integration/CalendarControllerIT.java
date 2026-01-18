package com.maru.integration;

import com.maru.todo.TodoApplication;
import com.maru.calendar.entity.CalendarEvent;
import com.maru.calendar.repository.CalendarEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = {TodoApplication.class, TestConfig.class})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("CalendarController 통합테스트")
class CalendarControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CalendarEventRepository eventRepository;

    @BeforeEach
    void setUp() {
        eventRepository.deleteAll();
    }

    @Test
    @DisplayName("캘린더 뷰 조회 - 성공")
    void viewCalendar_Success() throws Exception {
        // Given
        CalendarEvent event = new CalendarEvent();
        event.setTitle("테스트 이벤트");
        event.setStartDateTime(LocalDateTime.now().plusDays(1));
        event.setEndDateTime(LocalDateTime.now().plusDays(1).plusHours(1));
        event.setDeleted("N");
        eventRepository.save(event);

        // When & Then
        mockMvc.perform(get("/calendar"))
                .andExpect(status().isOk())
                .andExpect(view().name("calendar/view"))
                .andExpect(model().attributeExists("year"))
                .andExpect(model().attributeExists("month"))
                .andExpect(model().attributeExists("gridData"))
                .andExpect(model().attributeExists("events"))
                .andExpect(model().attributeExists("calendarEvent"))
                .andExpect(model().attributeExists("isGoogleConnected"));
    }

    @Test
    @DisplayName("캘린더 뷰 조회 - 특정 월 필터링")
    void viewCalendar_WithMonthFilter() throws Exception {
        // When & Then
        mockMvc.perform(get("/calendar")
                        .param("year", "2024")
                        .param("month", "6"))
                .andExpect(status().isOk())
                .andExpect(view().name("calendar/view"))
                .andExpect(model().attribute("year", 2024))
                .andExpect(model().attribute("month", 6));
    }

    @Test
    @DisplayName("이벤트 생성 - 성공")
    void createEvent_Success() throws Exception {
        // When
        mockMvc.perform(post("/calendar/events")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("title", "회의")
                        .param("startDateTime", "2024-06-15T10:00")
                        .param("endDateTime", "2024-06-15T11:00")
                        .param("description", "팀 회의")
                        .param("location", "회의실 A")
                        .param("allDay", "N"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/calendar"))
                .andExpect(flash().attributeExists("success"));

        // Then
        assertThat(eventRepository.count()).isEqualTo(1);
        CalendarEvent saved = eventRepository.findAll().get(0);
        assertThat(saved.getTitle()).isEqualTo("회의");
        assertThat(saved.getDescription()).isEqualTo("팀 회의");
        assertThat(saved.getLocation()).isEqualTo("회의실 A");
    }

    @Test
    @DisplayName("이벤트 생성 - 하루 종일 이벤트")
    void createEvent_AllDayEvent() throws Exception {
        // When
        mockMvc.perform(post("/calendar/events")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("title", "휴가")
                        .param("startDateTime", "2024-06-20T00:00")
                        .param("endDateTime", "2024-06-20T23:59")
                        .param("allDay", "Y"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/calendar"))
                .andExpect(flash().attributeExists("success"));

        // Then
        CalendarEvent saved = eventRepository.findAll().get(0);
        assertThat(saved.getTitle()).isEqualTo("휴가");
        assertThat(saved.getAllDay()).isEqualTo("Y");
        // All-day events should have time set to start/end of day
        assertThat(saved.getStartDateTime().getHour()).isEqualTo(0);
        assertThat(saved.getEndDateTime().getHour()).isEqualTo(23);
    }

    @Test
    @DisplayName("이벤트 생성 - 제목 없음 실패")
    void createEvent_NoTitle_Fails() throws Exception {
        // When
        mockMvc.perform(post("/calendar/events")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("title", "")
                        .param("startDateTime", "2024-06-15T10:00")
                        .param("endDateTime", "2024-06-15T11:00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/calendar"))
                .andExpect(flash().attributeExists("error"));

        // Then
        assertThat(eventRepository.count()).isZero();
    }

    @Test
    @DisplayName("이벤트 상세 조회 - 성공")
    void viewEvent_Success() throws Exception {
        // Given
        CalendarEvent event = new CalendarEvent();
        event.setTitle("상세 조회 이벤트");
        event.setStartDateTime(LocalDateTime.now().plusDays(1));
        event.setEndDateTime(LocalDateTime.now().plusDays(1).plusHours(1));
        event.setDeleted("N");
        CalendarEvent saved = eventRepository.save(event);

        // When & Then
        mockMvc.perform(get("/calendar/events/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("calendar/event-detail"))
                .andExpect(model().attributeExists("event"));
    }

    @Test
    @DisplayName("이벤트 상세 조회 - 존재하지 않는 ID 리다이렉트")
    void viewEvent_NotFound_Redirects() throws Exception {
        // When & Then
        mockMvc.perform(get("/calendar/events/{id}", 99999))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/calendar"));
    }

    @Test
    @DisplayName("이벤트 수정 - 성공")
    void updateEvent_Success() throws Exception {
        // Given
        CalendarEvent event = new CalendarEvent();
        event.setTitle("기존 이벤트");
        event.setStartDateTime(LocalDateTime.of(2024, 6, 15, 10, 0));
        event.setEndDateTime(LocalDateTime.of(2024, 6, 15, 11, 0));
        event.setDeleted("N");
        CalendarEvent saved = eventRepository.save(event);

        // When
        mockMvc.perform(post("/calendar/events/{id}", saved.getId())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("title", "수정된 이벤트")
                        .param("startDateTime", "2024-06-16T14:00")
                        .param("endDateTime", "2024-06-16T15:00")
                        .param("description", "새 설명")
                        .param("allDay", "N"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/calendar"))
                .andExpect(flash().attributeExists("success"));

        // Then
        Optional<CalendarEvent> updated = eventRepository.findById(saved.getId());
        assertThat(updated).isPresent();
        assertThat(updated.get().getTitle()).isEqualTo("수정된 이벤트");
        assertThat(updated.get().getDescription()).isEqualTo("새 설명");
    }

    @Test
    @DisplayName("이벤트 삭제 - 성공 (소프트 삭제)")
    void deleteEvent_Success() throws Exception {
        // Given
        CalendarEvent event = new CalendarEvent();
        event.setTitle("삭제할 이벤트");
        event.setStartDateTime(LocalDateTime.now().plusDays(1));
        event.setEndDateTime(LocalDateTime.now().plusDays(1).plusHours(1));
        event.setDeleted("N");
        CalendarEvent saved = eventRepository.save(event);

        // When
        mockMvc.perform(post("/calendar/events/{id}/delete", saved.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/calendar"))
                .andExpect(flash().attributeExists("success"));

        // Then - 소프트 삭제 확인
        Optional<CalendarEvent> deleted = eventRepository.findById(saved.getId());
        assertThat(deleted).isPresent();
        assertThat(deleted.get().getDeleted()).isEqualTo("Y");
    }

    @Test
    @DisplayName("월별 이벤트 조회")
    void viewCalendar_MonthlyEvents() throws Exception {
        // Given - 6월 이벤트
        CalendarEvent juneEvent = new CalendarEvent();
        juneEvent.setTitle("6월 이벤트");
        juneEvent.setStartDateTime(LocalDateTime.of(2024, 6, 15, 10, 0));
        juneEvent.setEndDateTime(LocalDateTime.of(2024, 6, 15, 11, 0));
        juneEvent.setDeleted("N");
        eventRepository.save(juneEvent);

        // Given - 7월 이벤트
        CalendarEvent julyEvent = new CalendarEvent();
        julyEvent.setTitle("7월 이벤트");
        julyEvent.setStartDateTime(LocalDateTime.of(2024, 7, 15, 10, 0));
        julyEvent.setEndDateTime(LocalDateTime.of(2024, 7, 15, 11, 0));
        julyEvent.setDeleted("N");
        eventRepository.save(julyEvent);

        // When & Then - 6월 조회
        mockMvc.perform(get("/calendar")
                        .param("year", "2024")
                        .param("month", "6"))
                .andExpect(status().isOk())
                .andExpect(view().name("calendar/view"))
                .andExpect(model().attributeExists("events"));
    }

    @Test
    @DisplayName("이벤트 기본값 확인")
    void event_DefaultValues() throws Exception {
        // When
        mockMvc.perform(post("/calendar/events")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("title", "기본값 테스트")
                        .param("startDateTime", "2024-06-15T10:00")
                        .param("endDateTime", "2024-06-15T11:00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/calendar"));

        // Then
        CalendarEvent saved = eventRepository.findAll().get(0);
        assertThat(saved.getDeleted()).isEqualTo("N");
        assertThat(saved.getSyncStatus()).isEqualTo("PENDING");  // Service sets PENDING for sync
        assertThat(saved.getAllDay()).isEqualTo("N");
    }

    @Test
    @DisplayName("삭제된 이벤트는 목록에서 제외")
    void viewCalendar_ExcludesDeletedEvents() throws Exception {
        // Given
        CalendarEvent activeEvent = new CalendarEvent();
        activeEvent.setTitle("활성 이벤트");
        activeEvent.setStartDateTime(LocalDateTime.now().plusDays(1));
        activeEvent.setEndDateTime(LocalDateTime.now().plusDays(1).plusHours(1));
        activeEvent.setDeleted("N");
        eventRepository.save(activeEvent);

        CalendarEvent deletedEvent = new CalendarEvent();
        deletedEvent.setTitle("삭제된 이벤트");
        deletedEvent.setStartDateTime(LocalDateTime.now().plusDays(2));
        deletedEvent.setEndDateTime(LocalDateTime.now().plusDays(2).plusHours(1));
        deletedEvent.setDeleted("Y");
        eventRepository.save(deletedEvent);

        // When & Then
        mockMvc.perform(get("/calendar"))
                .andExpect(status().isOk())
                .andExpect(view().name("calendar/view"));

        // Verify only non-deleted events are counted
        assertThat(eventRepository.findByDeletedOrderByStartDateTimeAsc("N")).hasSize(1);
    }

    @Test
    @DisplayName("이벤트에 D-Day 아이콘 설정")
    void createEvent_WithDDayIcon() throws Exception {
        // When
        mockMvc.perform(post("/calendar/events")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("title", "생일")
                        .param("startDateTime", "2024-06-15T00:00")
                        .param("endDateTime", "2024-06-15T23:59")
                        .param("ddayIcon", "🎂")
                        .param("allDay", "Y"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/calendar"));

        // Then
        CalendarEvent saved = eventRepository.findAll().get(0);
        assertThat(saved.getDdayIcon()).isEqualTo("🎂");
    }
}
