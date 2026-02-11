package com.maru.e2e;

import com.maru.integration.TestConfig;
import com.maru.habit.entity.Habit;
import com.maru.habit.entity.HabitRecord;
import com.maru.habit.repository.HabitRepository;
import com.maru.habit.repository.HabitRecordRepository;
import com.maru.todo.TodoApplication;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Tag;

/**
 * Habit Tracker E2E 테스트
 *
 * 시나리오: 습관 생성 → 체크박스 토글 → Body Log 숫자 입력 → 월별 Count 확인
 */
@SpringBootTest(
    classes = {TodoApplication.class, TestConfig.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
@DisplayName("Habit Tracker E2E 테스트")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("e2e")
class HabitE2ETest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private HabitRepository habitRepository;

    @Autowired
    private HabitRecordRepository habitRecordRepository;

    private String baseUrl;
    private static Long checkboxHabitId;
    private static Long bodyLogHabitId;

    @BeforeAll
    void setUp() {
        // 테스트 데이터 정리
        habitRecordRepository.deleteAll();
        habitRepository.deleteAll();
    }

    @BeforeEach
    void setUpEach() {
        baseUrl = "http://localhost:" + port;
    }

    // ==================== 체크박스 타입 습관 테스트 ====================

    @Test
    @Order(1)
    @DisplayName("1. 체크박스 타입 습관 생성")
    void createCheckboxHabit() {
        // given
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("name", "러닝하기");
        body.add("icon", "🏃");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        // when
        ResponseEntity<String> response = testRestTemplate.postForEntity(
                baseUrl + "/habits",
                request,
                String.class
        );

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);

        List<Habit> habits = habitRepository.findByDeletedOrderByDisplayOrderAscCreatedAtAsc("N");
        assertThat(habits).hasSize(1);
        assertThat(habits.get(0).getName()).isEqualTo("러닝하기");
        assertThat(habits.get(0).getIcon()).isEqualTo("🏃");
        checkboxHabitId = habits.get(0).getId();
    }

    @Test
    @Order(2)
    @DisplayName("2. 체크박스 토글 - 체크 추가")
    void toggleCheckbox_AddCheck() {
        // given
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        LocalDate today = LocalDate.now();
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("date", today.toString());
        body.add("year", String.valueOf(today.getYear()));
        body.add("month", String.valueOf(today.getMonthValue()));

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        // when
        ResponseEntity<String> response = testRestTemplate.postForEntity(
                baseUrl + "/habits/" + checkboxHabitId + "/toggle",
                request,
                String.class
        );

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);

        Habit habit = habitRepository.findById(checkboxHabitId).orElseThrow();
        Optional<HabitRecord> record = habitRecordRepository.findByHabitAndRecordDate(habit, today);
        assertThat(record).isPresent();
        assertThat(record.get().getCompleted()).isEqualTo("Y");
    }

    @Test
    @Order(3)
    @DisplayName("3. 체크박스 토글 - 체크 해제")
    void toggleCheckbox_RemoveCheck() {
        // given
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        LocalDate today = LocalDate.now();
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("date", today.toString());
        body.add("year", String.valueOf(today.getYear()));
        body.add("month", String.valueOf(today.getMonthValue()));

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        // when - 다시 토글하면 체크 해제
        ResponseEntity<String> response = testRestTemplate.postForEntity(
                baseUrl + "/habits/" + checkboxHabitId + "/toggle",
                request,
                String.class
        );

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);

        Habit habit = habitRepository.findById(checkboxHabitId).orElseThrow();
        Optional<HabitRecord> record = habitRecordRepository.findByHabitAndRecordDate(habit, today);
        assertThat(record).isEmpty();
    }

    // ==================== Body Log 타입 습관 테스트 ====================

    @Test
    @Order(4)
    @DisplayName("4. Body Log 타입 습관 생성")
    void createBodyLogHabit() {
        // given
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("name", "몸무게");
        body.add("icon", "📊");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        // when
        ResponseEntity<String> response = testRestTemplate.postForEntity(
                baseUrl + "/habits",
                request,
                String.class
        );

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);

        List<Habit> habits = habitRepository.findByDeletedOrderByDisplayOrderAscCreatedAtAsc("N");
        Habit bodyLogHabit = habits.stream()
                .filter(h -> "📊".equals(h.getIcon()))
                .findFirst()
                .orElseThrow();
        assertThat(bodyLogHabit.getName()).isEqualTo("몸무게");
        bodyLogHabitId = bodyLogHabit.getId();
    }

    @Test
    @Order(5)
    @DisplayName("5. Body Log 숫자 입력 - 소수점 값")
    void saveNumericRecord_DecimalValue() {
        // given
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        LocalDate today = LocalDate.now();
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("date", today.toString());
        body.add("value", "72.5");
        body.add("year", String.valueOf(today.getYear()));
        body.add("month", String.valueOf(today.getMonthValue()));

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        // when
        ResponseEntity<String> response = testRestTemplate.postForEntity(
                baseUrl + "/habits/" + bodyLogHabitId + "/numeric",
                request,
                String.class
        );

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);

        Habit habit = habitRepository.findById(bodyLogHabitId).orElseThrow();
        Optional<HabitRecord> record = habitRecordRepository.findByHabitAndRecordDate(habit, today);
        assertThat(record).isPresent();
        assertThat(record.get().getNumericValue()).isEqualTo(72.5);
    }

    @Test
    @Order(6)
    @DisplayName("6. Body Log 숫자 입력 - 정수 값")
    void saveNumericRecord_IntegerValue() {
        // given
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        LocalDate yesterday = LocalDate.now().minusDays(1);
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("date", yesterday.toString());
        body.add("value", "72");
        body.add("year", String.valueOf(yesterday.getYear()));
        body.add("month", String.valueOf(yesterday.getMonthValue()));

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        // when
        ResponseEntity<String> response = testRestTemplate.postForEntity(
                baseUrl + "/habits/" + bodyLogHabitId + "/numeric",
                request,
                String.class
        );

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);

        Habit habit = habitRepository.findById(bodyLogHabitId).orElseThrow();
        Optional<HabitRecord> record = habitRecordRepository.findByHabitAndRecordDate(habit, yesterday);
        assertThat(record).isPresent();
        assertThat(record.get().getNumericValue()).isEqualTo(72.0);
    }

    @Test
    @Order(7)
    @DisplayName("7. Body Log 숫자 입력 - 값 업데이트")
    void saveNumericRecord_UpdateValue() {
        // given
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        LocalDate today = LocalDate.now();
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("date", today.toString());
        body.add("value", "73.0");
        body.add("year", String.valueOf(today.getYear()));
        body.add("month", String.valueOf(today.getMonthValue()));

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        // when
        ResponseEntity<String> response = testRestTemplate.postForEntity(
                baseUrl + "/habits/" + bodyLogHabitId + "/numeric",
                request,
                String.class
        );

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);

        Habit habit = habitRepository.findById(bodyLogHabitId).orElseThrow();
        Optional<HabitRecord> record = habitRecordRepository.findByHabitAndRecordDate(habit, today);
        assertThat(record).isPresent();
        assertThat(record.get().getNumericValue()).isEqualTo(73.0);
    }

    // ==================== 페이지 렌더링 테스트 ====================

    @Test
    @Order(8)
    @DisplayName("8. 습관 목록 페이지 조회 - Count 표시 확인")
    void viewHabitsPage_ShowsCount() {
        // given
        // 체크박스 습관에 레코드 추가
        Habit checkboxHabit = habitRepository.findById(checkboxHabitId).orElseThrow();
        LocalDate today = LocalDate.now();
        habitRecordRepository.save(new HabitRecord(checkboxHabit, today));
        habitRecordRepository.save(new HabitRecord(checkboxHabit, today.minusDays(1)));
        habitRecordRepository.save(new HabitRecord(checkboxHabit, today.minusDays(2)));

        // when
        ResponseEntity<String> response = testRestTemplate.getForEntity(
                baseUrl + "/habits?year=" + today.getYear() + "&month=" + today.getMonthValue(),
                String.class
        );

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Count");
        assertThat(response.getBody()).contains("count-badge");
    }

    @Test
    @Order(9)
    @DisplayName("9. 습관 목록 페이지 - Body Log 숫자 입력 필드 확인")
    void viewHabitsPage_ShowsNumericInput() {
        // when
        ResponseEntity<String> response = testRestTemplate.getForEntity(
                baseUrl + "/habits",
                String.class
        );

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("numeric-input");
        assertThat(response.getBody()).contains("numeric-form");
    }

    // ==================== 정리 ====================

    @Test
    @Order(10)
    @DisplayName("10. 습관 삭제 (소프트 삭제)")
    void deleteHabit() {
        // given
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(new LinkedMultiValueMap<>(), headers);

        // when
        ResponseEntity<String> response = testRestTemplate.postForEntity(
                baseUrl + "/habits/" + checkboxHabitId + "/delete",
                request,
                String.class
        );

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);

        Habit deletedHabit = habitRepository.findById(checkboxHabitId).orElseThrow();
        assertThat(deletedHabit.getDeleted()).isEqualTo("Y");
    }
}
