package com.maru.habit.service;

import com.maru.habit.entity.Habit;
import com.maru.habit.entity.HabitRecord;
import com.maru.habit.repository.HabitRecordRepository;
import com.maru.habit.repository.HabitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("HabitService 단위 테스트")
class HabitServiceTest {

    @Mock
    private HabitRepository habitRepository;

    @Mock
    private HabitRecordRepository habitRecordRepository;

    private HabitService habitService;

    @BeforeEach
    void setUp() {
        habitService = new HabitService(habitRepository, habitRecordRepository);
    }

    private Habit createHabit(Long id, String name) {
        Habit habit = new Habit();
        habit.setId(id);
        habit.setName(name);
        habit.setDescription("설명");
        habit.setIcon("💪");
        habit.setColor("#FF5733");
        habit.setDeleted("N");
        return habit;
    }

    // ==================== getAllHabits Tests ====================

    @Nested
    @DisplayName("getAllHabits")
    class GetAllHabitsTests {

        @Test
        @DisplayName("모든 습관 조회 - 성공")
        void getAllHabits_Success() {
            // given
            Habit habit1 = createHabit(1L, "운동하기");
            Habit habit2 = createHabit(2L, "독서하기");
            when(habitRepository.findByDeletedOrderByDisplayOrderAscCreatedAtAsc("N"))
                    .thenReturn(Arrays.asList(habit1, habit2));

            // when
            List<Habit> result = habitService.getAllHabits();

            // then
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("모든 습관 조회 - 빈 목록")
        void getAllHabits_Empty() {
            // given
            when(habitRepository.findByDeletedOrderByDisplayOrderAscCreatedAtAsc("N"))
                    .thenReturn(Collections.emptyList());

            // when
            List<Habit> result = habitService.getAllHabits();

            // then
            assertThat(result).isEmpty();
        }
    }

    // ==================== getHabitById Tests ====================

    @Nested
    @DisplayName("getHabitById")
    class GetHabitByIdTests {

        @Test
        @DisplayName("ID로 습관 조회 - 존재하는 경우")
        void getHabitById_Found() {
            // given
            Habit habit = createHabit(1L, "운동하기");
            when(habitRepository.findById(1L)).thenReturn(Optional.of(habit));

            // when
            Optional<Habit> result = habitService.getHabitById(1L);

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getName()).isEqualTo("운동하기");
        }

        @Test
        @DisplayName("ID로 습관 조회 - 존재하지 않는 경우")
        void getHabitById_NotFound() {
            // given
            when(habitRepository.findById(999L)).thenReturn(Optional.empty());

            // when
            Optional<Habit> result = habitService.getHabitById(999L);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("ID로 습관 조회 - 삭제된 습관은 조회 불가")
        void getHabitById_DeletedHabit() {
            // given
            Habit deletedHabit = createHabit(1L, "삭제된 습관");
            deletedHabit.setDeleted("Y");
            when(habitRepository.findById(1L)).thenReturn(Optional.of(deletedHabit));

            // when
            Optional<Habit> result = habitService.getHabitById(1L);

            // then
            assertThat(result).isEmpty();
        }
    }

    // ==================== createHabit Tests ====================

    @Nested
    @DisplayName("createHabit")
    class CreateHabitTests {

        @Test
        @DisplayName("습관 생성 - 성공")
        void createHabit_Success() {
            // given
            Habit newHabit = new Habit();
            newHabit.setName("새 습관");

            when(habitRepository.save(any(Habit.class))).thenAnswer(inv -> {
                Habit saved = inv.getArgument(0);
                saved.setId(1L);
                return saved;
            });

            // when
            Habit result = habitService.createHabit(newHabit);

            // then
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("새 습관");
            verify(habitRepository).save(newHabit);
        }
    }

    // ==================== updateHabit Tests ====================

    @Nested
    @DisplayName("updateHabit")
    class UpdateHabitTests {

        @Test
        @DisplayName("습관 수정 - 성공")
        void updateHabit_Success() {
            // given
            Habit existingHabit = createHabit(1L, "기존 습관");

            Habit updatedData = new Habit();
            updatedData.setName("수정된 습관");
            updatedData.setDescription("수정된 설명");
            updatedData.setIcon("🏃");
            updatedData.setColor("#00FF00");

            when(habitRepository.findById(1L)).thenReturn(Optional.of(existingHabit));
            when(habitRepository.save(any(Habit.class))).thenAnswer(inv -> inv.getArgument(0));

            // when
            Habit result = habitService.updateHabit(1L, updatedData);

            // then
            assertThat(result.getName()).isEqualTo("수정된 습관");
            assertThat(result.getDescription()).isEqualTo("수정된 설명");
            assertThat(result.getIcon()).isEqualTo("🏃");
            assertThat(result.getColor()).isEqualTo("#00FF00");
        }

        @Test
        @DisplayName("습관 수정 - 존재하지 않는 ID")
        void updateHabit_NotFound() {
            // given
            Habit updatedData = new Habit();
            updatedData.setName("수정된 습관");

            when(habitRepository.findById(999L)).thenReturn(Optional.empty());

            // when/then
            assertThatThrownBy(() -> habitService.updateHabit(999L, updatedData))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Habit not found with id: 999");
        }

        @Test
        @DisplayName("습관 수정 - 삭제된 습관은 수정 불가")
        void updateHabit_DeletedHabit() {
            // given
            Habit deletedHabit = createHabit(1L, "삭제된 습관");
            deletedHabit.setDeleted("Y");

            Habit updatedData = new Habit();
            updatedData.setName("수정된 습관");

            when(habitRepository.findById(1L)).thenReturn(Optional.of(deletedHabit));

            // when/then
            assertThatThrownBy(() -> habitService.updateHabit(1L, updatedData))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Habit not found with id: 1");
        }
    }

    // ==================== deleteHabit Tests ====================

    @Nested
    @DisplayName("deleteHabit")
    class DeleteHabitTests {

        @Test
        @DisplayName("습관 삭제 (소프트 삭제) - 성공")
        void deleteHabit_Success() {
            // given
            Habit habit = createHabit(1L, "습관");
            when(habitRepository.findById(1L)).thenReturn(Optional.of(habit));
            when(habitRepository.save(any(Habit.class))).thenAnswer(inv -> inv.getArgument(0));

            // when
            habitService.deleteHabit(1L);

            // then
            ArgumentCaptor<Habit> habitCaptor = ArgumentCaptor.forClass(Habit.class);
            verify(habitRepository).save(habitCaptor.capture());
            assertThat(habitCaptor.getValue().getDeleted()).isEqualTo("Y");
        }

        @Test
        @DisplayName("습관 삭제 - 존재하지 않는 ID")
        void deleteHabit_NotFound() {
            // given
            when(habitRepository.findById(999L)).thenReturn(Optional.empty());

            // when
            habitService.deleteHabit(999L);

            // then
            verify(habitRepository, never()).save(any());
        }

        @Test
        @DisplayName("습관 삭제 - 이미 삭제된 습관")
        void deleteHabit_AlreadyDeleted() {
            // given
            Habit deletedHabit = createHabit(1L, "삭제된 습관");
            deletedHabit.setDeleted("Y");
            when(habitRepository.findById(1L)).thenReturn(Optional.of(deletedHabit));

            // when
            habitService.deleteHabit(1L);

            // then
            verify(habitRepository, never()).save(any());
        }
    }

    // ==================== toggleRecord Tests ====================

    @Nested
    @DisplayName("toggleRecord")
    class ToggleRecordTests {

        @Test
        @DisplayName("기록 토글 - 기록 추가")
        void toggleRecord_AddRecord() {
            // given
            Habit habit = createHabit(1L, "운동하기");
            LocalDate date = LocalDate.now();
            when(habitRepository.findById(1L)).thenReturn(Optional.of(habit));
            when(habitRecordRepository.findByHabitAndRecordDate(habit, date)).thenReturn(Optional.empty());
            when(habitRecordRepository.save(any(HabitRecord.class))).thenAnswer(inv -> inv.getArgument(0));

            // when
            habitService.toggleRecord(1L, date);

            // then
            verify(habitRecordRepository).save(any(HabitRecord.class));
            verify(habitRecordRepository, never()).delete(any());
        }

        @Test
        @DisplayName("기록 토글 - 기록 삭제")
        void toggleRecord_RemoveRecord() {
            // given
            Habit habit = createHabit(1L, "운동하기");
            LocalDate date = LocalDate.now();
            HabitRecord existingRecord = new HabitRecord(habit, date);
            when(habitRepository.findById(1L)).thenReturn(Optional.of(habit));
            when(habitRecordRepository.findByHabitAndRecordDate(habit, date)).thenReturn(Optional.of(existingRecord));

            // when
            habitService.toggleRecord(1L, date);

            // then
            verify(habitRecordRepository).delete(existingRecord);
            verify(habitRecordRepository, never()).save(any());
        }

        @Test
        @DisplayName("기록 토글 - 존재하지 않는 습관")
        void toggleRecord_HabitNotFound() {
            // given
            when(habitRepository.findById(999L)).thenReturn(Optional.empty());

            // when
            habitService.toggleRecord(999L, LocalDate.now());

            // then
            verify(habitRecordRepository, never()).save(any());
            verify(habitRecordRepository, never()).delete(any());
        }

        @Test
        @DisplayName("기록 토글 - 삭제된 습관")
        void toggleRecord_DeletedHabit() {
            // given
            Habit deletedHabit = createHabit(1L, "삭제된 습관");
            deletedHabit.setDeleted("Y");
            when(habitRepository.findById(1L)).thenReturn(Optional.of(deletedHabit));

            // when
            habitService.toggleRecord(1L, LocalDate.now());

            // then
            verify(habitRecordRepository, never()).save(any());
            verify(habitRecordRepository, never()).delete(any());
        }
    }

    // ==================== getRecordsForMonth Tests ====================

    @Nested
    @DisplayName("getRecordsForMonth")
    class GetRecordsForMonthTests {

        @Test
        @DisplayName("월별 기록 조회 - 성공")
        void getRecordsForMonth_Success() {
            // given
            Habit habit1 = createHabit(1L, "운동하기");
            Habit habit2 = createHabit(2L, "독서하기");

            LocalDate date1 = LocalDate.of(2024, 1, 5);
            LocalDate date2 = LocalDate.of(2024, 1, 10);
            HabitRecord record1 = new HabitRecord(habit1, date1);
            HabitRecord record2 = new HabitRecord(habit1, date2);

            when(habitRepository.findByDeletedOrderByDisplayOrderAscCreatedAtAsc("N"))
                    .thenReturn(Arrays.asList(habit1, habit2));
            when(habitRecordRepository.findByHabitAndRecordDateBetween(
                    eq(habit1), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(Arrays.asList(record1, record2));
            when(habitRecordRepository.findByHabitAndRecordDateBetween(
                    eq(habit2), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(Collections.emptyList());

            // when
            Map<Long, Set<LocalDate>> result = habitService.getRecordsForMonth(2024, 1);

            // then
            assertThat(result).hasSize(2);
            assertThat(result.get(1L)).hasSize(2);
            assertThat(result.get(2L)).isEmpty();
        }

        @Test
        @DisplayName("월별 기록 조회 - 빈 결과")
        void getRecordsForMonth_Empty() {
            // given
            when(habitRepository.findByDeletedOrderByDisplayOrderAscCreatedAtAsc("N"))
                    .thenReturn(Collections.emptyList());

            // when
            Map<Long, Set<LocalDate>> result = habitService.getRecordsForMonth(2024, 1);

            // then
            assertThat(result).isEmpty();
        }
    }

    // ==================== saveNumericRecord Tests ====================

    @Nested
    @DisplayName("saveNumericRecord")
    class SaveNumericRecordTests {

        @Test
        @DisplayName("숫자 기록 저장 - 새 기록 생성")
        void saveNumericRecord_CreateNew() {
            // given
            Habit habit = createHabit(1L, "몸무게");
            habit.setIcon("📊");
            LocalDate date = LocalDate.now();
            Double value = 72.5;

            when(habitRepository.findById(1L)).thenReturn(Optional.of(habit));
            when(habitRecordRepository.findByHabitAndRecordDate(habit, date)).thenReturn(Optional.empty());
            when(habitRecordRepository.save(any(HabitRecord.class))).thenAnswer(inv -> inv.getArgument(0));

            // when
            habitService.saveNumericRecord(1L, date, value);

            // then
            ArgumentCaptor<HabitRecord> captor = ArgumentCaptor.forClass(HabitRecord.class);
            verify(habitRecordRepository).save(captor.capture());
            assertThat(captor.getValue().getNumericValue()).isEqualTo(72.5);
        }

        @Test
        @DisplayName("숫자 기록 저장 - 기존 기록 업데이트")
        void saveNumericRecord_UpdateExisting() {
            // given
            Habit habit = createHabit(1L, "몸무게");
            habit.setIcon("📊");
            LocalDate date = LocalDate.now();
            HabitRecord existingRecord = new HabitRecord(habit, date, 70.0);

            when(habitRepository.findById(1L)).thenReturn(Optional.of(habit));
            when(habitRecordRepository.findByHabitAndRecordDate(habit, date)).thenReturn(Optional.of(existingRecord));
            when(habitRecordRepository.save(any(HabitRecord.class))).thenAnswer(inv -> inv.getArgument(0));

            // when
            habitService.saveNumericRecord(1L, date, 72.5);

            // then
            ArgumentCaptor<HabitRecord> captor = ArgumentCaptor.forClass(HabitRecord.class);
            verify(habitRecordRepository).save(captor.capture());
            assertThat(captor.getValue().getNumericValue()).isEqualTo(72.5);
        }

        @Test
        @DisplayName("숫자 기록 저장 - null 값으로 기록 삭제")
        void saveNumericRecord_DeleteWithNull() {
            // given
            Habit habit = createHabit(1L, "몸무게");
            habit.setIcon("📊");
            LocalDate date = LocalDate.now();
            HabitRecord existingRecord = new HabitRecord(habit, date, 70.0);

            when(habitRepository.findById(1L)).thenReturn(Optional.of(habit));
            when(habitRecordRepository.findByHabitAndRecordDate(habit, date)).thenReturn(Optional.of(existingRecord));

            // when
            habitService.saveNumericRecord(1L, date, null);

            // then
            verify(habitRecordRepository).delete(existingRecord);
            verify(habitRecordRepository, never()).save(any());
        }

        @Test
        @DisplayName("숫자 기록 저장 - 존재하지 않는 습관")
        void saveNumericRecord_HabitNotFound() {
            // given
            when(habitRepository.findById(999L)).thenReturn(Optional.empty());

            // when
            habitService.saveNumericRecord(999L, LocalDate.now(), 72.5);

            // then
            verify(habitRecordRepository, never()).save(any());
            verify(habitRecordRepository, never()).delete(any());
        }

        @Test
        @DisplayName("숫자 기록 저장 - 삭제된 습관")
        void saveNumericRecord_DeletedHabit() {
            // given
            Habit deletedHabit = createHabit(1L, "삭제된 습관");
            deletedHabit.setDeleted("Y");
            when(habitRepository.findById(1L)).thenReturn(Optional.of(deletedHabit));

            // when
            habitService.saveNumericRecord(1L, LocalDate.now(), 72.5);

            // then
            verify(habitRecordRepository, never()).save(any());
            verify(habitRecordRepository, never()).delete(any());
        }
    }

    // ==================== getNumericRecordsForMonth Tests ====================

    @Nested
    @DisplayName("getNumericRecordsForMonth")
    class GetNumericRecordsForMonthTests {

        @Test
        @DisplayName("월별 숫자 기록 조회 - 성공")
        void getNumericRecordsForMonth_Success() {
            // given
            Habit habit1 = createHabit(1L, "몸무게");
            Habit habit2 = createHabit(2L, "혈당");
            int year = 2026;
            int month = 1;
            LocalDate startDate = LocalDate.of(year, month, 1);
            LocalDate endDate = LocalDate.of(year, month, 31);

            when(habitRepository.findByDeletedOrderByDisplayOrderAscCreatedAtAsc("N"))
                    .thenReturn(Arrays.asList(habit1, habit2));

            // habit1: 숫자 기록 2개
            HabitRecord record1 = new HabitRecord(habit1, LocalDate.of(2026, 1, 28), 72.0);
            HabitRecord record2 = new HabitRecord(habit1, LocalDate.of(2026, 1, 29), 72.5);
            when(habitRecordRepository.findByHabitAndRecordDateBetween(habit1, startDate, endDate))
                    .thenReturn(Arrays.asList(record1, record2));

            // habit2: 숫자 기록 1개
            HabitRecord record3 = new HabitRecord(habit2, LocalDate.of(2026, 1, 29), 106.0);
            when(habitRecordRepository.findByHabitAndRecordDateBetween(habit2, startDate, endDate))
                    .thenReturn(Arrays.asList(record3));

            // when
            Map<Long, Map<LocalDate, Double>> result = habitService.getNumericRecordsForMonth(year, month);

            // then
            assertThat(result).hasSize(2);
            assertThat(result.get(1L)).hasSize(2);
            assertThat(result.get(1L).get(LocalDate.of(2026, 1, 28))).isEqualTo(72.0);
            assertThat(result.get(1L).get(LocalDate.of(2026, 1, 29))).isEqualTo(72.5);
            assertThat(result.get(2L)).hasSize(1);
            assertThat(result.get(2L).get(LocalDate.of(2026, 1, 29))).isEqualTo(106.0);
        }

        @Test
        @DisplayName("월별 숫자 기록 조회 - numericValue가 null인 기록 제외")
        void getNumericRecordsForMonth_ExcludeNullValues() {
            // given
            Habit habit = createHabit(1L, "몸무게");
            int year = 2026;
            int month = 1;
            LocalDate startDate = LocalDate.of(year, month, 1);
            LocalDate endDate = LocalDate.of(year, month, 31);

            when(habitRepository.findByDeletedOrderByDisplayOrderAscCreatedAtAsc("N"))
                    .thenReturn(Arrays.asList(habit));

            // 숫자 값 있는 기록과 없는 기록 혼합
            HabitRecord recordWithValue = new HabitRecord(habit, LocalDate.of(2026, 1, 28), 72.0);
            HabitRecord recordWithoutValue = new HabitRecord(habit, LocalDate.of(2026, 1, 29));  // numericValue = null
            when(habitRecordRepository.findByHabitAndRecordDateBetween(habit, startDate, endDate))
                    .thenReturn(Arrays.asList(recordWithValue, recordWithoutValue));

            // when
            Map<Long, Map<LocalDate, Double>> result = habitService.getNumericRecordsForMonth(year, month);

            // then
            assertThat(result.get(1L)).hasSize(1);
            assertThat(result.get(1L).get(LocalDate.of(2026, 1, 28))).isEqualTo(72.0);
            assertThat(result.get(1L).containsKey(LocalDate.of(2026, 1, 29))).isFalse();
        }

        @Test
        @DisplayName("월별 숫자 기록 조회 - 빈 결과")
        void getNumericRecordsForMonth_Empty() {
            // given
            when(habitRepository.findByDeletedOrderByDisplayOrderAscCreatedAtAsc("N"))
                    .thenReturn(Collections.emptyList());

            // when
            Map<Long, Map<LocalDate, Double>> result = habitService.getNumericRecordsForMonth(2026, 1);

            // then
            assertThat(result).isEmpty();
        }
    }

    // ==================== getMonthlyCounts Tests ====================

    @Nested
    @DisplayName("getMonthlyCounts")
    class GetMonthlyCountsTests {

        @Test
        @DisplayName("월별 수행 횟수 조회")
        void getMonthlyCounts_Success() {
            // given
            Habit habit1 = createHabit(1L, "운동하기");
            Habit habit2 = createHabit(2L, "독서하기");
            int year = 2026;
            int month = 1;
            LocalDate startDate = LocalDate.of(year, month, 1);
            LocalDate endDate = LocalDate.of(year, month, 31);

            when(habitRepository.findByDeletedOrderByDisplayOrderAscCreatedAtAsc("N"))
                    .thenReturn(Arrays.asList(habit1, habit2));

            // habit1: 5회
            when(habitRecordRepository.findByHabitAndRecordDateBetween(habit1, startDate, endDate))
                    .thenReturn(Arrays.asList(
                            new HabitRecord(habit1, LocalDate.of(2026, 1, 1)),
                            new HabitRecord(habit1, LocalDate.of(2026, 1, 2)),
                            new HabitRecord(habit1, LocalDate.of(2026, 1, 3)),
                            new HabitRecord(habit1, LocalDate.of(2026, 1, 4)),
                            new HabitRecord(habit1, LocalDate.of(2026, 1, 5))
                    ));

            // habit2: 2회
            when(habitRecordRepository.findByHabitAndRecordDateBetween(habit2, startDate, endDate))
                    .thenReturn(Arrays.asList(
                            new HabitRecord(habit2, LocalDate.of(2026, 1, 10)),
                            new HabitRecord(habit2, LocalDate.of(2026, 1, 15))
                    ));

            // when
            Map<Long, Integer> result = habitService.getMonthlyCounts(year, month);

            // then
            assertThat(result).hasSize(2);
            assertThat(result.get(1L)).isEqualTo(5);
            assertThat(result.get(2L)).isEqualTo(2);
        }

        @Test
        @DisplayName("월별 수행 횟수 조회 - 빈 결과")
        void getMonthlyCounts_Empty() {
            // given
            when(habitRepository.findByDeletedOrderByDisplayOrderAscCreatedAtAsc("N"))
                    .thenReturn(Collections.emptyList());

            // when
            Map<Long, Integer> result = habitService.getMonthlyCounts(2026, 1);

            // then
            assertThat(result).isEmpty();
        }
    }
}
