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

    // ==================== getStreakCount Tests ====================

    @Nested
    @DisplayName("getStreakCount")
    class GetStreakCountTests {

        @Test
        @DisplayName("연속 기록 일수 조회 - 연속 3일")
        void getStreakCount_ThreeDays() {
            // given
            Habit habit = createHabit(1L, "운동하기");
            LocalDate today = LocalDate.now();

            when(habitRecordRepository.findByHabitAndRecordDate(habit, today))
                    .thenReturn(Optional.of(new HabitRecord(habit, today)));
            when(habitRecordRepository.findByHabitAndRecordDate(habit, today.minusDays(1)))
                    .thenReturn(Optional.of(new HabitRecord(habit, today.minusDays(1))));
            when(habitRecordRepository.findByHabitAndRecordDate(habit, today.minusDays(2)))
                    .thenReturn(Optional.of(new HabitRecord(habit, today.minusDays(2))));
            when(habitRecordRepository.findByHabitAndRecordDate(habit, today.minusDays(3)))
                    .thenReturn(Optional.empty());

            // when
            int result = habitService.getStreakCount(habit);

            // then
            assertThat(result).isEqualTo(3);
        }

        @Test
        @DisplayName("연속 기록 일수 조회 - 오늘 기록 없음")
        void getStreakCount_NoRecordToday() {
            // given
            Habit habit = createHabit(1L, "운동하기");
            LocalDate today = LocalDate.now();

            when(habitRecordRepository.findByHabitAndRecordDate(habit, today))
                    .thenReturn(Optional.empty());

            // when
            int result = habitService.getStreakCount(habit);

            // then
            assertThat(result).isEqualTo(0);
        }
    }

    // ==================== getStreakCounts Tests ====================

    @Nested
    @DisplayName("getStreakCounts")
    class GetStreakCountsTests {

        @Test
        @DisplayName("모든 습관의 연속 기록 일수 조회")
        void getStreakCounts_Success() {
            // given
            Habit habit1 = createHabit(1L, "운동하기");
            Habit habit2 = createHabit(2L, "독서하기");
            LocalDate today = LocalDate.now();

            when(habitRepository.findByDeletedOrderByDisplayOrderAscCreatedAtAsc("N"))
                    .thenReturn(Arrays.asList(habit1, habit2));

            // habit1: 2일 연속
            when(habitRecordRepository.findByHabitAndRecordDate(habit1, today))
                    .thenReturn(Optional.of(new HabitRecord(habit1, today)));
            when(habitRecordRepository.findByHabitAndRecordDate(habit1, today.minusDays(1)))
                    .thenReturn(Optional.of(new HabitRecord(habit1, today.minusDays(1))));
            when(habitRecordRepository.findByHabitAndRecordDate(habit1, today.minusDays(2)))
                    .thenReturn(Optional.empty());

            // habit2: 0일
            when(habitRecordRepository.findByHabitAndRecordDate(habit2, today))
                    .thenReturn(Optional.empty());

            // when
            Map<Long, Integer> result = habitService.getStreakCounts();

            // then
            assertThat(result).hasSize(2);
            assertThat(result.get(1L)).isEqualTo(2);
            assertThat(result.get(2L)).isEqualTo(0);
        }
    }
}
