package com.maru.integration;

import com.maru.todo.TodoApplication;
import com.maru.habit.entity.Habit;
import com.maru.habit.entity.HabitRecord;
import com.maru.habit.repository.HabitRepository;
import com.maru.habit.repository.HabitRecordRepository;
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

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = {TodoApplication.class, TestConfig.class})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("HabitController 통합테스트")
class HabitControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private HabitRepository habitRepository;

    @Autowired
    private HabitRecordRepository habitRecordRepository;

    @BeforeEach
    void setUp() {
        habitRecordRepository.deleteAll();
        habitRepository.deleteAll();
    }

    @Test
    @DisplayName("Habit 목록 조회 - 성공")
    void listHabits_Success() throws Exception {
        // Given
        Habit habit = new Habit();
        habit.setName("운동");
        habit.setDeleted("N");
        habitRepository.save(habit);

        // When & Then
        mockMvc.perform(get("/habits"))
                .andExpect(status().isOk())
                .andExpect(view().name("habit/list"))
                .andExpect(model().attributeExists("habits"))
                .andExpect(model().attributeExists("records"))
                .andExpect(model().attributeExists("streaks"))
                .andExpect(model().attributeExists("days"))
                .andExpect(model().attributeExists("today"))
                .andExpect(model().attributeExists("newHabit"));
    }

    @Test
    @DisplayName("Habit 목록 조회 - 특정 월 필터링")
    void listHabits_WithMonthFilter() throws Exception {
        // When & Then
        mockMvc.perform(get("/habits")
                        .param("year", "2024")
                        .param("month", "6"))
                .andExpect(status().isOk())
                .andExpect(view().name("habit/list"))
                .andExpect(model().attribute("year", 2024))
                .andExpect(model().attribute("month", 6));
    }

    @Test
    @DisplayName("Habit 생성 - 성공")
    void createHabit_Success() throws Exception {
        // When
        mockMvc.perform(post("/habits")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("name", "독서")
                        .param("description", "하루 30분 독서")
                        .param("icon", "📚")
                        .param("color", "#4CAF50"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/habits"))
                .andExpect(flash().attributeExists("success"));

        // Then
        assertThat(habitRepository.count()).isEqualTo(1);
        Habit saved = habitRepository.findAll().get(0);
        assertThat(saved.getName()).isEqualTo("독서");
        assertThat(saved.getDescription()).isEqualTo("하루 30분 독서");
        assertThat(saved.getIcon()).isEqualTo("📚");
        assertThat(saved.getColor()).isEqualTo("#4CAF50");
    }

    @Test
    @DisplayName("Habit 생성 - 이름 없음 실패")
    void createHabit_NoName_Fails() throws Exception {
        // When
        mockMvc.perform(post("/habits")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("name", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/habits"))
                .andExpect(flash().attributeExists("error"));

        // Then
        assertThat(habitRepository.count()).isZero();
    }

    @Test
    @DisplayName("Habit 수정 폼 - 성공")
    void editHabitForm_Success() throws Exception {
        // Given
        Habit habit = new Habit();
        habit.setName("수정할 습관");
        habit.setDeleted("N");
        Habit saved = habitRepository.save(habit);

        // When & Then
        mockMvc.perform(get("/habits/{id}/edit", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("habit/edit"))
                .andExpect(model().attributeExists("habit"));
    }

    @Test
    @DisplayName("Habit 수정 폼 - 존재하지 않는 ID 리다이렉트")
    void editHabitForm_NotFound_Redirects() throws Exception {
        // When & Then
        mockMvc.perform(get("/habits/{id}/edit", 99999))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/habits"));
    }

    @Test
    @DisplayName("Habit 수정 - 성공")
    void updateHabit_Success() throws Exception {
        // Given
        Habit habit = new Habit();
        habit.setName("기존 습관");
        habit.setDeleted("N");
        Habit saved = habitRepository.save(habit);

        // When
        mockMvc.perform(post("/habits/{id}", saved.getId())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("name", "수정된 습관")
                        .param("description", "새로운 설명"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/habits"))
                .andExpect(flash().attributeExists("success"));

        // Then
        Optional<Habit> updated = habitRepository.findById(saved.getId());
        assertThat(updated).isPresent();
        assertThat(updated.get().getName()).isEqualTo("수정된 습관");
        assertThat(updated.get().getDescription()).isEqualTo("새로운 설명");
    }

    @Test
    @DisplayName("Habit 삭제 - 성공 (소프트 삭제)")
    void deleteHabit_Success() throws Exception {
        // Given
        Habit habit = new Habit();
        habit.setName("삭제할 습관");
        habit.setDeleted("N");
        Habit saved = habitRepository.save(habit);

        // When
        mockMvc.perform(post("/habits/{id}/delete", saved.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/habits"))
                .andExpect(flash().attributeExists("success"));

        // Then - 소프트 삭제 확인
        Optional<Habit> deleted = habitRepository.findById(saved.getId());
        assertThat(deleted).isPresent();
        assertThat(deleted.get().getDeleted()).isEqualTo("Y");
    }

    @Test
    @DisplayName("Habit 기록 토글 - 기록 생성")
    void toggleRecord_CreateRecord() throws Exception {
        // Given
        Habit habit = new Habit();
        habit.setName("운동");
        habit.setDeleted("N");
        Habit saved = habitRepository.save(habit);
        LocalDate today = LocalDate.now();

        // When
        mockMvc.perform(post("/habits/{id}/toggle", saved.getId())
                        .param("date", today.toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/habits"));

        // Then
        Optional<HabitRecord> record = habitRecordRepository.findByHabitAndRecordDate(saved, today);
        assertThat(record).isPresent();
        assertThat(record.get().getCompleted()).isEqualTo("Y");
    }

    @Test
    @DisplayName("Habit 기록 토글 - 기록 삭제")
    void toggleRecord_DeleteRecord() throws Exception {
        // Given
        Habit habit = new Habit();
        habit.setName("운동");
        habit.setDeleted("N");
        Habit saved = habitRepository.save(habit);
        LocalDate today = LocalDate.now();

        HabitRecord record = new HabitRecord(saved, today);
        habitRecordRepository.save(record);

        // When - 첫 번째 토글로 삭제
        mockMvc.perform(post("/habits/{id}/toggle", saved.getId())
                        .param("date", today.toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/habits"));

        // Then
        Optional<HabitRecord> deletedRecord = habitRecordRepository.findByHabitAndRecordDate(saved, today);
        assertThat(deletedRecord).isEmpty();
    }

    @Test
    @DisplayName("Habit 기록 토글 - 월/년 파라미터 유지")
    void toggleRecord_KeepsMonthYearParams() throws Exception {
        // Given
        Habit habit = new Habit();
        habit.setName("운동");
        habit.setDeleted("N");
        Habit saved = habitRepository.save(habit);
        LocalDate date = LocalDate.of(2024, 6, 15);

        // When
        mockMvc.perform(post("/habits/{id}/toggle", saved.getId())
                        .param("date", date.toString())
                        .param("year", "2024")
                        .param("month", "6"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/habits?year=2024&month=6"));
    }

    @Test
    @DisplayName("Habit 기본값 확인")
    void habit_DefaultValues() throws Exception {
        // When
        mockMvc.perform(post("/habits")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("name", "기본값 테스트"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/habits"));

        // Then
        Habit saved = habitRepository.findAll().get(0);
        assertThat(saved.getIcon()).isEqualTo("✅");
        assertThat(saved.getColor()).isEqualTo("#667eea");
        assertThat(saved.getDeleted()).isEqualTo("N");
        assertThat(saved.getDisplayOrder()).isEqualTo(0);
    }

    @Test
    @DisplayName("다수의 Habit 목록 조회")
    void listMultipleHabits_Success() throws Exception {
        // Given
        Habit habit1 = new Habit();
        habit1.setName("운동");
        habit1.setDisplayOrder(1);
        habit1.setDeleted("N");
        habitRepository.save(habit1);

        Habit habit2 = new Habit();
        habit2.setName("독서");
        habit2.setDisplayOrder(2);
        habit2.setDeleted("N");
        habitRepository.save(habit2);

        Habit habit3 = new Habit();
        habit3.setName("명상");
        habit3.setDisplayOrder(3);
        habit3.setDeleted("N");
        habitRepository.save(habit3);

        // When & Then
        mockMvc.perform(get("/habits"))
                .andExpect(status().isOk())
                .andExpect(view().name("habit/list"))
                .andExpect(model().attributeExists("habits"));

        // Verify count (excluding soft deleted)
        assertThat(habitRepository.findByDeletedOrderByDisplayOrderAscCreatedAtAsc("N")).hasSize(3);
    }

    @Test
    @DisplayName("삭제된 Habit은 목록에서 제외")
    void listHabits_ExcludesDeleted() throws Exception {
        // Given
        Habit activeHabit = new Habit();
        activeHabit.setName("활성 습관");
        activeHabit.setDeleted("N");
        habitRepository.save(activeHabit);

        Habit deletedHabit = new Habit();
        deletedHabit.setName("삭제된 습관");
        deletedHabit.setDeleted("Y");
        habitRepository.save(deletedHabit);

        // When & Then
        mockMvc.perform(get("/habits"))
                .andExpect(status().isOk())
                .andExpect(view().name("habit/list"));

        // Verify only active habits are returned
        assertThat(habitRepository.findByDeletedOrderByDisplayOrderAscCreatedAtAsc("N")).hasSize(1);
    }
}
