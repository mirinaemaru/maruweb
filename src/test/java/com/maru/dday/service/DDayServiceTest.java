package com.maru.dday.service;

import com.maru.dday.entity.DDay;
import com.maru.dday.repository.DDayRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
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
@DisplayName("DDayService 단위 테스트")
class DDayServiceTest {

    @Mock
    private DDayRepository ddayRepository;

    private DDayService ddayService;

    @BeforeEach
    void setUp() {
        ddayService = new DDayService(ddayRepository);
    }

    private DDay createDDay(Long id, String title, LocalDate targetDate) {
        DDay dday = new DDay();
        dday.setId(id);
        dday.setTitle(title);
        dday.setDescription("설명");
        dday.setTargetDate(targetDate);
        dday.setIcon("🎉");
        dday.setDeleted("N");
        return dday;
    }

    // ==================== getAllDDays Tests ====================

    @Nested
    @DisplayName("getAllDDays")
    class GetAllDDaysTests {

        @Test
        @DisplayName("모든 D-Day 조회 - 성공")
        void getAllDDays_Success() {
            // given
            DDay dday1 = createDDay(1L, "생일", LocalDate.now().plusDays(30));
            DDay dday2 = createDDay(2L, "기념일", LocalDate.now().plusDays(60));
            when(ddayRepository.findByDeletedOrderByTargetDateAsc("N"))
                    .thenReturn(Arrays.asList(dday1, dday2));

            // when
            List<DDay> result = ddayService.getAllDDays();

            // then
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("모든 D-Day 조회 - 빈 목록")
        void getAllDDays_Empty() {
            // given
            when(ddayRepository.findByDeletedOrderByTargetDateAsc("N"))
                    .thenReturn(Collections.emptyList());

            // when
            List<DDay> result = ddayService.getAllDDays();

            // then
            assertThat(result).isEmpty();
        }
    }

    // ==================== getUpcomingDDays Tests ====================

    @Nested
    @DisplayName("getUpcomingDDays")
    class GetUpcomingDDaysTests {

        @Test
        @DisplayName("다가오는 D-Day 조회 - 성공")
        void getUpcomingDDays_Success() {
            // given
            DDay dday1 = createDDay(1L, "다가오는 생일", LocalDate.now().plusDays(10));
            when(ddayRepository.findByDeletedAndTargetDateGreaterThanEqualOrderByTargetDateAsc(
                    eq("N"), any(LocalDate.class)))
                    .thenReturn(Arrays.asList(dday1));

            // when
            List<DDay> result = ddayService.getUpcomingDDays();

            // then
            assertThat(result).hasSize(1);
            verify(ddayRepository).findByDeletedAndTargetDateGreaterThanEqualOrderByTargetDateAsc(
                    eq("N"), any(LocalDate.class));
        }

        @Test
        @DisplayName("다가오는 D-Day 조회 - 빈 목록")
        void getUpcomingDDays_Empty() {
            // given
            when(ddayRepository.findByDeletedAndTargetDateGreaterThanEqualOrderByTargetDateAsc(
                    eq("N"), any(LocalDate.class)))
                    .thenReturn(Collections.emptyList());

            // when
            List<DDay> result = ddayService.getUpcomingDDays();

            // then
            assertThat(result).isEmpty();
        }
    }

    // ==================== getPastDDays Tests ====================

    @Nested
    @DisplayName("getPastDDays")
    class GetPastDDaysTests {

        @Test
        @DisplayName("지난 D-Day 조회 - 성공")
        void getPastDDays_Success() {
            // given
            DDay dday1 = createDDay(1L, "지난 생일", LocalDate.now().minusDays(30));
            when(ddayRepository.findByDeletedAndTargetDateLessThanOrderByTargetDateDesc(
                    eq("N"), any(LocalDate.class)))
                    .thenReturn(Arrays.asList(dday1));

            // when
            List<DDay> result = ddayService.getPastDDays();

            // then
            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("지난 D-Day 조회 - 빈 목록")
        void getPastDDays_Empty() {
            // given
            when(ddayRepository.findByDeletedAndTargetDateLessThanOrderByTargetDateDesc(
                    eq("N"), any(LocalDate.class)))
                    .thenReturn(Collections.emptyList());

            // when
            List<DDay> result = ddayService.getPastDDays();

            // then
            assertThat(result).isEmpty();
        }
    }

    // ==================== getDDayById Tests ====================

    @Nested
    @DisplayName("getDDayById")
    class GetDDayByIdTests {

        @Test
        @DisplayName("ID로 D-Day 조회 - 존재하는 경우")
        void getDDayById_Found() {
            // given
            DDay dday = createDDay(1L, "생일", LocalDate.now().plusDays(30));
            when(ddayRepository.findById(1L)).thenReturn(Optional.of(dday));

            // when
            Optional<DDay> result = ddayService.getDDayById(1L);

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getTitle()).isEqualTo("생일");
        }

        @Test
        @DisplayName("ID로 D-Day 조회 - 존재하지 않는 경우")
        void getDDayById_NotFound() {
            // given
            when(ddayRepository.findById(999L)).thenReturn(Optional.empty());

            // when
            Optional<DDay> result = ddayService.getDDayById(999L);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("ID로 D-Day 조회 - 삭제된 D-Day는 조회 불가")
        void getDDayById_DeletedDDay() {
            // given
            DDay deletedDDay = createDDay(1L, "삭제된 D-Day", LocalDate.now());
            deletedDDay.setDeleted("Y");
            when(ddayRepository.findById(1L)).thenReturn(Optional.of(deletedDDay));

            // when
            Optional<DDay> result = ddayService.getDDayById(1L);

            // then
            assertThat(result).isEmpty();
        }
    }

    // ==================== createDDay Tests ====================

    @Nested
    @DisplayName("createDDay")
    class CreateDDayTests {

        @Test
        @DisplayName("D-Day 생성 - 성공")
        void createDDay_Success() {
            // given
            DDay newDDay = new DDay();
            newDDay.setTitle("새로운 D-Day");
            newDDay.setTargetDate(LocalDate.now().plusDays(100));

            when(ddayRepository.save(any(DDay.class))).thenAnswer(inv -> {
                DDay saved = inv.getArgument(0);
                saved.setId(1L);
                return saved;
            });

            // when
            DDay result = ddayService.createDDay(newDDay);

            // then
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getTitle()).isEqualTo("새로운 D-Day");
            verify(ddayRepository).save(newDDay);
        }
    }

    // ==================== updateDDay Tests ====================

    @Nested
    @DisplayName("updateDDay")
    class UpdateDDayTests {

        @Test
        @DisplayName("D-Day 수정 - 성공")
        void updateDDay_Success() {
            // given
            DDay existingDDay = createDDay(1L, "기존 D-Day", LocalDate.now().plusDays(30));

            DDay updatedData = new DDay();
            updatedData.setTitle("수정된 D-Day");
            updatedData.setDescription("수정된 설명");
            updatedData.setTargetDate(LocalDate.now().plusDays(60));
            updatedData.setIcon("🎂");

            when(ddayRepository.findById(1L)).thenReturn(Optional.of(existingDDay));
            when(ddayRepository.save(any(DDay.class))).thenAnswer(inv -> inv.getArgument(0));

            // when
            DDay result = ddayService.updateDDay(1L, updatedData);

            // then
            assertThat(result.getTitle()).isEqualTo("수정된 D-Day");
            assertThat(result.getDescription()).isEqualTo("수정된 설명");
            assertThat(result.getIcon()).isEqualTo("🎂");
        }

        @Test
        @DisplayName("D-Day 수정 - 존재하지 않는 ID")
        void updateDDay_NotFound() {
            // given
            DDay updatedData = new DDay();
            updatedData.setTitle("수정된 D-Day");

            when(ddayRepository.findById(999L)).thenReturn(Optional.empty());

            // when/then
            assertThatThrownBy(() -> ddayService.updateDDay(999L, updatedData))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("D-Day not found with id: 999");
        }

        @Test
        @DisplayName("D-Day 수정 - 삭제된 D-Day는 수정 불가")
        void updateDDay_DeletedDDay() {
            // given
            DDay deletedDDay = createDDay(1L, "삭제된 D-Day", LocalDate.now());
            deletedDDay.setDeleted("Y");

            DDay updatedData = new DDay();
            updatedData.setTitle("수정된 D-Day");

            when(ddayRepository.findById(1L)).thenReturn(Optional.of(deletedDDay));

            // when/then
            assertThatThrownBy(() -> ddayService.updateDDay(1L, updatedData))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("D-Day not found with id: 1");
        }
    }

    // ==================== deleteDDay Tests ====================

    @Nested
    @DisplayName("deleteDDay")
    class DeleteDDayTests {

        @Test
        @DisplayName("D-Day 삭제 (소프트 삭제) - 성공")
        void deleteDDay_Success() {
            // given
            DDay dday = createDDay(1L, "D-Day", LocalDate.now().plusDays(30));
            when(ddayRepository.findById(1L)).thenReturn(Optional.of(dday));
            when(ddayRepository.save(any(DDay.class))).thenAnswer(inv -> inv.getArgument(0));

            // when
            ddayService.deleteDDay(1L);

            // then
            ArgumentCaptor<DDay> ddayCaptor = ArgumentCaptor.forClass(DDay.class);
            verify(ddayRepository).save(ddayCaptor.capture());
            assertThat(ddayCaptor.getValue().getDeleted()).isEqualTo("Y");
        }

        @Test
        @DisplayName("D-Day 삭제 - 존재하지 않는 ID")
        void deleteDDay_NotFound() {
            // given
            when(ddayRepository.findById(999L)).thenReturn(Optional.empty());

            // when
            ddayService.deleteDDay(999L);

            // then
            verify(ddayRepository, never()).save(any());
        }

        @Test
        @DisplayName("D-Day 삭제 - 이미 삭제된 D-Day")
        void deleteDDay_AlreadyDeleted() {
            // given
            DDay deletedDDay = createDDay(1L, "삭제된 D-Day", LocalDate.now());
            deletedDDay.setDeleted("Y");
            when(ddayRepository.findById(1L)).thenReturn(Optional.of(deletedDDay));

            // when
            ddayService.deleteDDay(1L);

            // then
            verify(ddayRepository, never()).save(any());
        }
    }
}
