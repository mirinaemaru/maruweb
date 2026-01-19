package com.maru.calendar.service;

import com.maru.calendar.entity.CalendarEvent;
import com.maru.calendar.repository.CalendarEventRepository;
import com.maru.dday.entity.DDay;
import com.maru.dday.service.DDayService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CalendarEventService 단위 테스트")
class CalendarEventServiceTest {

    @Mock
    private CalendarEventRepository calendarEventRepository;

    @Mock
    private GoogleCalendarSyncService syncService;

    @Mock
    private DDayService ddayService;

    private CalendarEventService calendarEventService;

    @BeforeEach
    void setUp() {
        calendarEventService = new CalendarEventService(calendarEventRepository, syncService, ddayService);
    }

    private CalendarEvent createEvent(Long id, String title, LocalDateTime startDateTime) {
        CalendarEvent event = new CalendarEvent();
        event.setId(id);
        event.setTitle(title);
        event.setDescription("이벤트 설명");
        event.setStartDateTime(startDateTime);
        event.setEndDateTime(startDateTime.plusHours(1));
        event.setAllDay("N");
        event.setDeleted("N");
        return event;
    }

    // ==================== getEventsForMonth Tests ====================

    @Nested
    @DisplayName("getEventsForMonth")
    class GetEventsForMonthTests {

        @Test
        @DisplayName("월별 이벤트 조회 - 성공")
        void getEventsForMonth_Success() {
            // given
            CalendarEvent event1 = createEvent(1L, "회의", LocalDateTime.of(2024, 1, 15, 10, 0));
            CalendarEvent event2 = createEvent(2L, "미팅", LocalDateTime.of(2024, 1, 20, 14, 0));

            when(calendarEventRepository.findByDeletedAndStartDateTimeBetweenOrderByStartDateTimeAsc(
                    eq("N"), any(LocalDateTime.class), any(LocalDateTime.class)))
                    .thenReturn(Arrays.asList(event1, event2));

            // when
            List<CalendarEvent> result = calendarEventService.getEventsForMonth(2024, 1);

            // then
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("월별 이벤트 조회 - 빈 결과")
        void getEventsForMonth_Empty() {
            // given
            when(calendarEventRepository.findByDeletedAndStartDateTimeBetweenOrderByStartDateTimeAsc(
                    eq("N"), any(LocalDateTime.class), any(LocalDateTime.class)))
                    .thenReturn(Collections.emptyList());

            // when
            List<CalendarEvent> result = calendarEventService.getEventsForMonth(2024, 1);

            // then
            assertThat(result).isEmpty();
        }
    }

    // ==================== getEventsForDay Tests ====================

    @Nested
    @DisplayName("getEventsForDay")
    class GetEventsForDayTests {

        @Test
        @DisplayName("일별 이벤트 조회 - 성공")
        void getEventsForDay_Success() {
            // given
            LocalDate date = LocalDate.of(2024, 1, 15);
            CalendarEvent event1 = createEvent(1L, "아침 회의", date.atTime(9, 0));
            CalendarEvent event2 = createEvent(2L, "점심 미팅", date.atTime(12, 0));

            when(calendarEventRepository.findByDeletedAndStartDateTimeBetween(
                    eq("N"), any(LocalDateTime.class), any(LocalDateTime.class)))
                    .thenReturn(Arrays.asList(event1, event2));

            // when
            List<CalendarEvent> result = calendarEventService.getEventsForDay(date);

            // then
            assertThat(result).hasSize(2);
        }
    }

    // ==================== getAllEvents Tests ====================

    @Nested
    @DisplayName("getAllEvents")
    class GetAllEventsTests {

        @Test
        @DisplayName("모든 이벤트 조회 - 성공")
        void getAllEvents_Success() {
            // given
            CalendarEvent event1 = createEvent(1L, "이벤트1", LocalDateTime.now());
            CalendarEvent event2 = createEvent(2L, "이벤트2", LocalDateTime.now().plusDays(1));

            when(calendarEventRepository.findByDeletedOrderByStartDateTimeAsc("N"))
                    .thenReturn(Arrays.asList(event1, event2));

            // when
            List<CalendarEvent> result = calendarEventService.getAllEvents();

            // then
            assertThat(result).hasSize(2);
        }
    }

    // ==================== getEventById Tests ====================

    @Nested
    @DisplayName("getEventById")
    class GetEventByIdTests {

        @Test
        @DisplayName("ID로 이벤트 조회 - 존재하는 경우")
        void getEventById_Found() {
            // given
            CalendarEvent event = createEvent(1L, "회의", LocalDateTime.now());
            when(calendarEventRepository.findById(1L)).thenReturn(Optional.of(event));

            // when
            Optional<CalendarEvent> result = calendarEventService.getEventById(1L);

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getTitle()).isEqualTo("회의");
        }

        @Test
        @DisplayName("ID로 이벤트 조회 - 존재하지 않음")
        void getEventById_NotFound() {
            // given
            when(calendarEventRepository.findById(999L)).thenReturn(Optional.empty());

            // when
            Optional<CalendarEvent> result = calendarEventService.getEventById(999L);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("ID로 이벤트 조회 - 삭제된 이벤트")
        void getEventById_Deleted() {
            // given
            CalendarEvent deletedEvent = createEvent(1L, "삭제된 이벤트", LocalDateTime.now());
            deletedEvent.setDeleted("Y");
            when(calendarEventRepository.findById(1L)).thenReturn(Optional.of(deletedEvent));

            // when
            Optional<CalendarEvent> result = calendarEventService.getEventById(1L);

            // then
            assertThat(result).isEmpty();
        }
    }

    // ==================== createEvent Tests ====================

    @Nested
    @DisplayName("createEvent")
    class CreateEventTests {

        @Test
        @DisplayName("이벤트 생성 - D-Day 없음")
        void createEvent_WithoutDDay() {
            // given
            CalendarEvent newEvent = createEvent(null, "새 이벤트", LocalDateTime.now());

            when(calendarEventRepository.save(any(CalendarEvent.class))).thenAnswer(inv -> {
                CalendarEvent saved = inv.getArgument(0);
                saved.setId(1L);
                return saved;
            });

            // when
            CalendarEvent result = calendarEventService.createEvent(newEvent);

            // then
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getSyncStatus()).isEqualTo("PENDING");
            verify(syncService).markEventForSync(any(CalendarEvent.class));
            verify(ddayService, never()).createDDay(any(DDay.class));
        }

        @Test
        @DisplayName("이벤트 생성 - D-Day 연동")
        void createEvent_WithDDay() {
            // given
            CalendarEvent newEvent = createEvent(null, "생일 파티", LocalDateTime.now());
            newEvent.setDdayIcon("🎂");

            DDay savedDDay = new DDay();
            savedDDay.setId(10L);

            when(calendarEventRepository.save(any(CalendarEvent.class))).thenAnswer(inv -> {
                CalendarEvent saved = inv.getArgument(0);
                saved.setId(1L);
                return saved;
            });
            when(ddayService.createDDay(any(DDay.class))).thenReturn(savedDDay);

            // when
            CalendarEvent result = calendarEventService.createEvent(newEvent);

            // then
            assertThat(result.getDdayId()).isEqualTo(10L);
            verify(ddayService).createDDay(any(DDay.class));
            verify(calendarEventRepository, times(2)).save(any(CalendarEvent.class)); // 2번 저장 (초기 + D-Day ID 업데이트)
        }
    }

    // ==================== updateEvent Tests ====================

    @Nested
    @DisplayName("updateEvent")
    class UpdateEventTests {

        @Test
        @DisplayName("이벤트 수정 - 성공")
        void updateEvent_Success() {
            // given
            CalendarEvent existing = createEvent(1L, "기존 이벤트", LocalDateTime.now());

            CalendarEvent updated = new CalendarEvent();
            updated.setTitle("수정된 이벤트");
            updated.setDescription("수정된 설명");
            updated.setStartDateTime(LocalDateTime.now().plusDays(1));
            updated.setEndDateTime(LocalDateTime.now().plusDays(1).plusHours(2));
            updated.setAllDay("N");
            updated.setLocation("회의실 A");

            when(calendarEventRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(calendarEventRepository.save(any(CalendarEvent.class))).thenAnswer(inv -> inv.getArgument(0));

            // when
            CalendarEvent result = calendarEventService.updateEvent(1L, updated);

            // then
            assertThat(result.getTitle()).isEqualTo("수정된 이벤트");
            assertThat(result.getDescription()).isEqualTo("수정된 설명");
            assertThat(result.getLocation()).isEqualTo("회의실 A");
            verify(syncService).markEventForSync(any(CalendarEvent.class));
        }

        @Test
        @DisplayName("이벤트 수정 - D-Day 새로 추가")
        void updateEvent_AddDDay() {
            // given
            CalendarEvent existing = createEvent(1L, "기존 이벤트", LocalDateTime.now());

            CalendarEvent updated = new CalendarEvent();
            updated.setTitle("생일");
            updated.setStartDateTime(LocalDateTime.now());
            updated.setDdayIcon("🎂");

            DDay newDDay = new DDay();
            newDDay.setId(10L);

            when(calendarEventRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(calendarEventRepository.save(any(CalendarEvent.class))).thenAnswer(inv -> inv.getArgument(0));
            when(ddayService.createDDay(any(DDay.class))).thenReturn(newDDay);

            // when
            CalendarEvent result = calendarEventService.updateEvent(1L, updated);

            // then
            assertThat(result.getDdayId()).isEqualTo(10L);
            assertThat(result.getDdayIcon()).isEqualTo("🎂");
            verify(ddayService).createDDay(any(DDay.class));
        }

        @Test
        @DisplayName("이벤트 수정 - D-Day 삭제")
        void updateEvent_RemoveDDay() {
            // given
            CalendarEvent existing = createEvent(1L, "기존 이벤트", LocalDateTime.now());
            existing.setDdayId(10L);
            existing.setDdayIcon("🎂");

            CalendarEvent updated = new CalendarEvent();
            updated.setTitle("일반 이벤트");
            updated.setStartDateTime(LocalDateTime.now());
            updated.setDdayIcon(null);

            when(calendarEventRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(calendarEventRepository.save(any(CalendarEvent.class))).thenAnswer(inv -> inv.getArgument(0));

            // when
            CalendarEvent result = calendarEventService.updateEvent(1L, updated);

            // then
            assertThat(result.getDdayId()).isNull();
            assertThat(result.getDdayIcon()).isNull();
            verify(ddayService).deleteDDay(10L);
        }

        @Test
        @DisplayName("이벤트 수정 - 존재하지 않음")
        void updateEvent_NotFound() {
            // given
            CalendarEvent updated = new CalendarEvent();
            updated.setTitle("수정된 이벤트");

            when(calendarEventRepository.findById(999L)).thenReturn(Optional.empty());

            // when/then
            assertThatThrownBy(() -> calendarEventService.updateEvent(999L, updated))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Event not found with id: 999");
        }

        @Test
        @DisplayName("이벤트 수정 - 삭제된 이벤트")
        void updateEvent_DeletedEvent() {
            // given
            CalendarEvent deletedEvent = createEvent(1L, "삭제된 이벤트", LocalDateTime.now());
            deletedEvent.setDeleted("Y");

            CalendarEvent updated = new CalendarEvent();
            updated.setTitle("수정된 이벤트");

            when(calendarEventRepository.findById(1L)).thenReturn(Optional.of(deletedEvent));

            // when/then
            assertThatThrownBy(() -> calendarEventService.updateEvent(1L, updated))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Event not found with id: 1");
        }
    }

    // ==================== deleteEvent Tests ====================

    @Nested
    @DisplayName("deleteEvent")
    class DeleteEventTests {

        @Test
        @DisplayName("이벤트 삭제 - D-Day 없음")
        void deleteEvent_WithoutDDay() {
            // given
            CalendarEvent event = createEvent(1L, "이벤트", LocalDateTime.now());
            when(calendarEventRepository.findById(1L)).thenReturn(Optional.of(event));
            when(calendarEventRepository.save(any(CalendarEvent.class))).thenAnswer(inv -> inv.getArgument(0));

            // when
            calendarEventService.deleteEvent(1L);

            // then
            ArgumentCaptor<CalendarEvent> captor = ArgumentCaptor.forClass(CalendarEvent.class);
            verify(calendarEventRepository).save(captor.capture());
            assertThat(captor.getValue().getDeleted()).isEqualTo("Y");
            verify(syncService).markEventForSync(any(CalendarEvent.class));
            verify(ddayService, never()).deleteDDay(anyLong());
        }

        @Test
        @DisplayName("이벤트 삭제 - D-Day도 함께 삭제")
        void deleteEvent_WithDDay() {
            // given
            CalendarEvent event = createEvent(1L, "이벤트", LocalDateTime.now());
            event.setDdayId(10L);
            when(calendarEventRepository.findById(1L)).thenReturn(Optional.of(event));
            when(calendarEventRepository.save(any(CalendarEvent.class))).thenAnswer(inv -> inv.getArgument(0));

            // when
            calendarEventService.deleteEvent(1L);

            // then
            verify(ddayService).deleteDDay(10L);
        }

        @Test
        @DisplayName("이벤트 삭제 - 존재하지 않음")
        void deleteEvent_NotFound() {
            // given
            when(calendarEventRepository.findById(999L)).thenReturn(Optional.empty());

            // when
            calendarEventService.deleteEvent(999L);

            // then
            verify(calendarEventRepository, never()).save(any());
            verify(ddayService, never()).deleteDDay(anyLong());
        }

        @Test
        @DisplayName("이벤트 삭제 - 이미 삭제된 이벤트")
        void deleteEvent_AlreadyDeleted() {
            // given
            CalendarEvent deletedEvent = createEvent(1L, "삭제된 이벤트", LocalDateTime.now());
            deletedEvent.setDeleted("Y");
            when(calendarEventRepository.findById(1L)).thenReturn(Optional.of(deletedEvent));

            // when
            calendarEventService.deleteEvent(1L);

            // then
            verify(calendarEventRepository, never()).save(any());
        }
    }
}
