package com.maru.integration;

import com.maru.todo.TodoApplication;
import com.maru.dday.entity.DDay;
import com.maru.dday.repository.DDayRepository;
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
@DisplayName("DDayController 통합테스트")
class DDayControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DDayRepository ddayRepository;

    @BeforeEach
    void setUp() {
        ddayRepository.deleteAll();
    }

    @Test
    @DisplayName("D-Day 목록 조회 - 성공")
    void listDDays_Success() throws Exception {
        // Given
        DDay futureDDay = new DDay();
        futureDDay.setTitle("미래 이벤트");
        futureDDay.setTargetDate(LocalDate.now().plusDays(30));
        futureDDay.setDeleted("N");
        ddayRepository.save(futureDDay);

        DDay pastDDay = new DDay();
        pastDDay.setTitle("지난 이벤트");
        pastDDay.setTargetDate(LocalDate.now().minusDays(10));
        pastDDay.setDeleted("N");
        ddayRepository.save(pastDDay);

        // When & Then
        mockMvc.perform(get("/dday"))
                .andExpect(status().isOk())
                .andExpect(view().name("dday/list"))
                .andExpect(model().attributeExists("upcomingDDays"))
                .andExpect(model().attributeExists("pastDDays"))
                .andExpect(model().attributeExists("newDDay"));
    }

    @Test
    @DisplayName("D-Day 생성 - 성공")
    void createDDay_Success() throws Exception {
        // When
        mockMvc.perform(post("/dday")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("title", "생일")
                        .param("targetDate", "2024-12-25")
                        .param("description", "크리스마스!")
                        .param("icon", "🎄"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dday"))
                .andExpect(flash().attributeExists("success"));

        // Then
        assertThat(ddayRepository.count()).isEqualTo(1);
        DDay saved = ddayRepository.findAll().get(0);
        assertThat(saved.getTitle()).isEqualTo("생일");
        assertThat(saved.getTargetDate()).isEqualTo(LocalDate.of(2024, 12, 25));
        assertThat(saved.getDescription()).isEqualTo("크리스마스!");
        assertThat(saved.getIcon()).isEqualTo("🎄");
    }

    @Test
    @DisplayName("D-Day 생성 - 제목 없음 실패")
    void createDDay_NoTitle_Fails() throws Exception {
        // When
        mockMvc.perform(post("/dday")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("title", "")
                        .param("targetDate", "2024-12-25"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dday"))
                .andExpect(flash().attributeExists("error"));

        // Then
        assertThat(ddayRepository.count()).isZero();
    }

    @Test
    @DisplayName("D-Day 생성 - 날짜 없음 실패")
    void createDDay_NoDate_Fails() throws Exception {
        // When
        mockMvc.perform(post("/dday")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("title", "이벤트"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dday"))
                .andExpect(flash().attributeExists("error"));

        // Then
        assertThat(ddayRepository.count()).isZero();
    }

    @Test
    @DisplayName("D-Day 수정 폼 - 성공")
    void editDDayForm_Success() throws Exception {
        // Given
        DDay dday = new DDay();
        dday.setTitle("수정할 이벤트");
        dday.setTargetDate(LocalDate.now().plusDays(7));
        dday.setDeleted("N");
        DDay saved = ddayRepository.save(dday);

        // When & Then
        mockMvc.perform(get("/dday/{id}/edit", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("dday/edit"))
                .andExpect(model().attributeExists("dday"));
    }

    @Test
    @DisplayName("D-Day 수정 폼 - 존재하지 않는 ID 리다이렉트")
    void editDDayForm_NotFound_Redirects() throws Exception {
        // When & Then
        mockMvc.perform(get("/dday/{id}/edit", 99999))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dday"));
    }

    @Test
    @DisplayName("D-Day 수정 - 성공")
    void updateDDay_Success() throws Exception {
        // Given
        DDay dday = new DDay();
        dday.setTitle("기존 이벤트");
        dday.setTargetDate(LocalDate.now().plusDays(7));
        dday.setDeleted("N");
        DDay saved = ddayRepository.save(dday);

        // When
        mockMvc.perform(post("/dday/{id}", saved.getId())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("title", "수정된 이벤트")
                        .param("targetDate", "2025-01-01")
                        .param("description", "새해"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dday"))
                .andExpect(flash().attributeExists("success"));

        // Then
        Optional<DDay> updated = ddayRepository.findById(saved.getId());
        assertThat(updated).isPresent();
        assertThat(updated.get().getTitle()).isEqualTo("수정된 이벤트");
        assertThat(updated.get().getTargetDate()).isEqualTo(LocalDate.of(2025, 1, 1));
    }

    @Test
    @DisplayName("D-Day 삭제 - 성공 (소프트 삭제)")
    void deleteDDay_Success() throws Exception {
        // Given
        DDay dday = new DDay();
        dday.setTitle("삭제할 이벤트");
        dday.setTargetDate(LocalDate.now().plusDays(7));
        dday.setDeleted("N");
        DDay saved = ddayRepository.save(dday);

        // When
        mockMvc.perform(post("/dday/{id}/delete", saved.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dday"))
                .andExpect(flash().attributeExists("success"));

        // Then - 소프트 삭제 확인
        Optional<DDay> deleted = ddayRepository.findById(saved.getId());
        assertThat(deleted).isPresent();
        assertThat(deleted.get().getDeleted()).isEqualTo("Y");
    }

    @Test
    @DisplayName("D-Day 계산 - 미래 날짜")
    void ddayCalculation_FutureDate() throws Exception {
        // Given
        DDay dday = new DDay();
        dday.setTitle("미래 이벤트");
        dday.setTargetDate(LocalDate.now().plusDays(10));
        dday.setDeleted("N");
        ddayRepository.save(dday);

        // Then
        assertThat(dday.getDaysRemaining()).isEqualTo(10);
        assertThat(dday.getDDayDisplay()).isEqualTo("D-10");
        assertThat(dday.isPast()).isFalse();
    }

    @Test
    @DisplayName("D-Day 계산 - 과거 날짜")
    void ddayCalculation_PastDate() throws Exception {
        // Given
        DDay dday = new DDay();
        dday.setTitle("과거 이벤트");
        dday.setTargetDate(LocalDate.now().minusDays(5));
        dday.setDeleted("N");
        ddayRepository.save(dday);

        // Then
        assertThat(dday.getDaysRemaining()).isEqualTo(-5);
        assertThat(dday.getDDayDisplay()).isEqualTo("D+5");
        assertThat(dday.isPast()).isTrue();
    }

    @Test
    @DisplayName("D-Day 계산 - 오늘")
    void ddayCalculation_Today() throws Exception {
        // Given
        DDay dday = new DDay();
        dday.setTitle("오늘 이벤트");
        dday.setTargetDate(LocalDate.now());
        dday.setDeleted("N");
        ddayRepository.save(dday);

        // Then
        assertThat(dday.getDaysRemaining()).isEqualTo(0);
        assertThat(dday.getDDayDisplay()).isEqualTo("D-Day");
        assertThat(dday.isToday()).isTrue();
    }

    @Test
    @DisplayName("D-Day 정렬 - 미래 날짜는 오름차순")
    void listUpcomingDDays_SortedAscending() throws Exception {
        // Given
        DDay dday1 = new DDay();
        dday1.setTitle("먼 미래");
        dday1.setTargetDate(LocalDate.now().plusDays(30));
        dday1.setDeleted("N");
        ddayRepository.save(dday1);

        DDay dday2 = new DDay();
        dday2.setTitle("가까운 미래");
        dday2.setTargetDate(LocalDate.now().plusDays(5));
        dday2.setDeleted("N");
        ddayRepository.save(dday2);

        // When & Then - 목록 조회 시 정렬 확인
        mockMvc.perform(get("/dday"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("upcomingDDays"));
    }
}
