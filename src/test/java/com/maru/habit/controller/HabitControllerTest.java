package com.maru.habit.controller;

import com.maru.habit.entity.Habit;
import com.maru.habit.service.HabitService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest
@ContextConfiguration(classes = TestConfig.class)
@Import(HabitController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("HabitController 통합 테스트")
class HabitControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private HabitService habitService;

    private Habit createMockHabit(Long id, String name, String icon) {
        Habit habit = new Habit();
        habit.setId(id);
        habit.setName(name);
        habit.setIcon(icon);
        habit.setDeleted("N");
        return habit;
    }

    // ==================== GET /habits Tests ====================

    @Nested
    @DisplayName("GET /habits")
    class ListHabitsTests {

        @Test
        @DisplayName("습관 목록 조회 - counts와 numericRecords 포함 확인")
        void listHabits_ContainsCountsAndNumericRecords() throws Exception {
            // given
            Habit habit1 = createMockHabit(1L, "러닝하기", "🏃");
            Habit habit2 = createMockHabit(2L, "몸무게", "📊");

            when(habitService.getAllHabits()).thenReturn(Arrays.asList(habit1, habit2));
            when(habitService.getRecordsForMonth(anyInt(), anyInt())).thenReturn(new HashMap<>());
            when(habitService.getNumericRecordsForMonth(anyInt(), anyInt())).thenReturn(new HashMap<>());

            Map<Long, Integer> counts = new HashMap<>();
            counts.put(1L, 17);
            counts.put(2L, 2);
            when(habitService.getMonthlyCounts(anyInt(), anyInt())).thenReturn(counts);

            // when & then
            mockMvc.perform(get("/habits"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("habit/list"))
                    .andExpect(model().attributeExists("habits"))
                    .andExpect(model().attributeExists("records"))
                    .andExpect(model().attributeExists("numericRecords"))
                    .andExpect(model().attributeExists("counts"))
                    .andExpect(model().attributeExists("days"))
                    .andExpect(model().attributeExists("year"))
                    .andExpect(model().attributeExists("month"));
        }

        @Test
        @DisplayName("습관 목록 조회 - 특정 년월 파라미터")
        void listHabits_WithYearMonthParams() throws Exception {
            // given
            when(habitService.getAllHabits()).thenReturn(Collections.emptyList());
            when(habitService.getRecordsForMonth(2025, 12)).thenReturn(new HashMap<>());
            when(habitService.getNumericRecordsForMonth(2025, 12)).thenReturn(new HashMap<>());
            when(habitService.getMonthlyCounts(2025, 12)).thenReturn(new HashMap<>());

            // when & then
            mockMvc.perform(get("/habits")
                            .param("year", "2025")
                            .param("month", "12"))
                    .andExpect(status().isOk())
                    .andExpect(model().attribute("year", 2025))
                    .andExpect(model().attribute("month", 12));

            verify(habitService).getMonthlyCounts(2025, 12);
        }
    }

    // ==================== POST /habits/{id}/numeric Tests ====================

    @Nested
    @DisplayName("POST /habits/{id}/numeric")
    class SaveNumericRecordTests {

        @Test
        @DisplayName("숫자 기록 저장 - 성공")
        void saveNumericRecord_Success() throws Exception {
            // given
            doNothing().when(habitService).saveNumericRecord(anyLong(), any(LocalDate.class), anyDouble());

            // when & then
            mockMvc.perform(post("/habits/1/numeric")
                            .param("date", "2026-01-29")
                            .param("value", "72.5")
                            .param("year", "2026")
                            .param("month", "1"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/habits?year=2026&month=1"));

            verify(habitService).saveNumericRecord(eq(1L), eq(LocalDate.of(2026, 1, 29)), eq(72.5));
        }

        @Test
        @DisplayName("숫자 기록 저장 - 소수점 첫째자리 값")
        void saveNumericRecord_DecimalValue() throws Exception {
            // given
            doNothing().when(habitService).saveNumericRecord(anyLong(), any(LocalDate.class), anyDouble());

            // when & then
            mockMvc.perform(post("/habits/2/numeric")
                            .param("date", "2026-01-28")
                            .param("value", "106.5")
                            .param("year", "2026")
                            .param("month", "1"))
                    .andExpect(status().is3xxRedirection());

            verify(habitService).saveNumericRecord(eq(2L), eq(LocalDate.of(2026, 1, 28)), eq(106.5));
        }

        @Test
        @DisplayName("숫자 기록 저장 - 정수 값")
        void saveNumericRecord_IntegerValue() throws Exception {
            // given
            doNothing().when(habitService).saveNumericRecord(anyLong(), any(LocalDate.class), anyDouble());

            // when & then
            mockMvc.perform(post("/habits/1/numeric")
                            .param("date", "2026-01-29")
                            .param("value", "72")
                            .param("year", "2026")
                            .param("month", "1"))
                    .andExpect(status().is3xxRedirection());

            verify(habitService).saveNumericRecord(eq(1L), eq(LocalDate.of(2026, 1, 29)), eq(72.0));
        }

        @Test
        @DisplayName("숫자 기록 저장 - 값 없이 (삭제)")
        void saveNumericRecord_NoValue() throws Exception {
            // given
            doNothing().when(habitService).saveNumericRecord(anyLong(), any(LocalDate.class), isNull());

            // when & then
            mockMvc.perform(post("/habits/1/numeric")
                            .param("date", "2026-01-29")
                            .param("year", "2026")
                            .param("month", "1"))
                    .andExpect(status().is3xxRedirection());

            verify(habitService).saveNumericRecord(eq(1L), eq(LocalDate.of(2026, 1, 29)), isNull());
        }

        @Test
        @DisplayName("숫자 기록 저장 - 년월 파라미터 없이 리다이렉트")
        void saveNumericRecord_NoYearMonthParams() throws Exception {
            // given
            doNothing().when(habitService).saveNumericRecord(anyLong(), any(LocalDate.class), anyDouble());

            // when & then
            mockMvc.perform(post("/habits/1/numeric")
                            .param("date", "2026-01-29")
                            .param("value", "72.5"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/habits"));
        }
    }

    // ==================== POST /habits/{id}/toggle Tests ====================

    @Nested
    @DisplayName("POST /habits/{id}/toggle")
    class ToggleRecordTests {

        @Test
        @DisplayName("체크박스 토글 - 성공")
        void toggleRecord_Success() throws Exception {
            // given
            doNothing().when(habitService).toggleRecord(anyLong(), any(LocalDate.class));

            // when & then
            mockMvc.perform(post("/habits/1/toggle")
                            .param("date", "2026-01-29")
                            .param("year", "2026")
                            .param("month", "1"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/habits?year=2026&month=1"));

            verify(habitService).toggleRecord(eq(1L), eq(LocalDate.of(2026, 1, 29)));
        }
    }
}
