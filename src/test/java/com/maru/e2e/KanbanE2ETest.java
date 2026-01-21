package com.maru.e2e;

import com.maru.integration.TestConfig;
import com.maru.kanban.entity.KanbanProject;
import com.maru.kanban.entity.KanbanTask;
import com.maru.kanban.entity.KanbanTaskComment;
import com.maru.kanban.repository.KanbanProjectRepository;
import com.maru.kanban.repository.KanbanTaskCommentRepository;
import com.maru.kanban.repository.KanbanTaskRepository;
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

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kanban 시스템 E2E 테스트
 *
 * 시나리오: 프로젝트 생성 → 태스크 생성 → 상태 변경 → 댓글 추가 → 삭제
 */
@SpringBootTest(
    classes = {TodoApplication.class, TestConfig.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
@DisplayName("Kanban E2E 테스트")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KanbanE2ETest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private KanbanProjectRepository projectRepository;

    @Autowired
    private KanbanTaskRepository taskRepository;

    @Autowired
    private KanbanTaskCommentRepository commentRepository;

    private String baseUrl;
    private static Long createdProjectId;
    private static Long createdTaskId;
    private static Long createdCommentId;

    @BeforeAll
    void setUp() {
        // 테스트 데이터 정리
        commentRepository.deleteAll();
        taskRepository.deleteAll();
        projectRepository.deleteAll();
    }

    @BeforeEach
    void setUpEach() {
        baseUrl = "http://localhost:" + port;
    }

    // ==================== Project E2E Tests ====================

    @Test
    @Order(1)
    @DisplayName("칸반 보드 페이지 조회 - 빈 상태")
    void viewEmptyBoard() {
        // When
        ResponseEntity<String> response = testRestTemplate.getForEntity(
            baseUrl + "/kanban", String.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("칸반");
    }

    @Test
    @Order(2)
    @DisplayName("프로젝트 생성 폼 조회")
    void viewNewProjectForm() {
        // When
        ResponseEntity<String> response = testRestTemplate.getForEntity(
            baseUrl + "/kanban/projects/new", String.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("프로젝트");
    }

    @Test
    @Order(3)
    @DisplayName("프로젝트 생성")
    void createProject() {
        // Given
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("name", "E2E 테스트 프로젝트");
        formData.add("directoryPath", "/test/e2e-project");
        formData.add("description", "E2E 테스트를 위한 프로젝트");
        formData.add("color", "#667eea");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(formData, headers);

        // When
        ResponseEntity<String> response = testRestTemplate.postForEntity(
            baseUrl + "/kanban/projects", request, String.class);

        // Then
        assertThat(response.getStatusCode()).isIn(HttpStatus.OK, HttpStatus.FOUND, HttpStatus.SEE_OTHER);

        // 생성된 프로젝트 확인
        List<KanbanProject> projects = projectRepository.findByDeletedOrderByDisplayOrderAsc("N");
        assertThat(projects).isNotEmpty();

        KanbanProject created = projects.stream()
            .filter(p -> "E2E 테스트 프로젝트".equals(p.getName()))
            .findFirst()
            .orElse(null);
        assertThat(created).isNotNull();
        createdProjectId = created.getId();
        System.out.println("Created project ID: " + createdProjectId);
    }

    @Test
    @Order(4)
    @DisplayName("프로젝트 포함 칸반 보드 조회")
    void viewBoardWithProject() {
        // Skip if project was not created
        Assumptions.assumeTrue(createdProjectId != null, "Project was not created");

        // When
        ResponseEntity<String> response = testRestTemplate.getForEntity(
            baseUrl + "/kanban?projectId=" + createdProjectId, String.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("E2E 테스트 프로젝트");
    }

    // ==================== Task E2E Tests ====================

    @Test
    @Order(5)
    @DisplayName("태스크 생성")
    void createTask() {
        // Skip if project was not created
        Assumptions.assumeTrue(createdProjectId != null, "Project was not created");

        // Given
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("projectId", createdProjectId.toString());
        formData.add("title", "E2E 테스트 태스크");
        formData.add("description", "E2E 테스트를 위한 태스크입니다");
        formData.add("priority", "HIGH");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(formData, headers);

        // When
        ResponseEntity<String> response = testRestTemplate.postForEntity(
            baseUrl + "/kanban/tasks", request, String.class);

        // Then
        assertThat(response.getStatusCode()).isIn(HttpStatus.OK, HttpStatus.FOUND, HttpStatus.SEE_OTHER);

        // 생성된 태스크 확인
        List<KanbanTask> tasks = taskRepository.findByProjectIdAndDeletedOrderByUpdatedAtDesc(createdProjectId, "N");
        assertThat(tasks).isNotEmpty();

        KanbanTask created = tasks.stream()
            .filter(t -> "E2E 테스트 태스크".equals(t.getTitle()))
            .findFirst()
            .orElse(null);
        assertThat(created).isNotNull();
        assertThat(created.getStatus()).isEqualTo("REGISTERED");
        assertThat(created.getPriority()).isEqualTo("HIGH");
        createdTaskId = created.getId();
        System.out.println("Created task ID: " + createdTaskId);
    }

    @Test
    @Order(6)
    @DisplayName("태스크 포함 보드 조회")
    void viewBoardWithTask() {
        // Skip if task was not created
        Assumptions.assumeTrue(createdTaskId != null, "Task was not created");

        // When
        ResponseEntity<String> response = testRestTemplate.getForEntity(
            baseUrl + "/kanban?projectId=" + createdProjectId, String.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("E2E 테스트 태스크");
    }

    @Test
    @Order(7)
    @DisplayName("태스크 상태 변경 - REGISTERED → IN_PROGRESS")
    void updateTaskStatus_ToInProgress() {
        // Skip if task was not created
        Assumptions.assumeTrue(createdTaskId != null, "Task was not created");

        // Given
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("status", "IN_PROGRESS");
        formData.add("projectId", createdProjectId.toString());

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(formData, headers);

        // When
        ResponseEntity<String> response = testRestTemplate.postForEntity(
            baseUrl + "/kanban/tasks/" + createdTaskId + "/status", request, String.class);

        // Then
        assertThat(response.getStatusCode()).isIn(HttpStatus.OK, HttpStatus.FOUND, HttpStatus.SEE_OTHER);

        // 상태 변경 확인
        Optional<KanbanTask> updated = taskRepository.findByIdAndDeleted(createdTaskId, "N");
        assertThat(updated).isPresent();
        assertThat(updated.get().getStatus()).isEqualTo("IN_PROGRESS");
    }

    @Test
    @Order(8)
    @DisplayName("태스크 상태 변경 - IN_PROGRESS → WAITING_RESPONSE")
    void updateTaskStatus_ToWaiting() {
        // Skip if task was not created
        Assumptions.assumeTrue(createdTaskId != null, "Task was not created");

        // Given
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("status", "WAITING_RESPONSE");
        formData.add("projectId", createdProjectId.toString());

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(formData, headers);

        // When
        ResponseEntity<String> response = testRestTemplate.postForEntity(
            baseUrl + "/kanban/tasks/" + createdTaskId + "/status", request, String.class);

        // Then
        assertThat(response.getStatusCode()).isIn(HttpStatus.OK, HttpStatus.FOUND, HttpStatus.SEE_OTHER);

        // 상태 변경 확인
        Optional<KanbanTask> updated = taskRepository.findByIdAndDeleted(createdTaskId, "N");
        assertThat(updated).isPresent();
        assertThat(updated.get().getStatus()).isEqualTo("WAITING_RESPONSE");
    }

    // ==================== Comment E2E Tests ====================

    @Test
    @Order(9)
    @DisplayName("댓글 추가")
    void addComment() {
        // Skip if task was not created
        Assumptions.assumeTrue(createdTaskId != null, "Task was not created");

        // Given
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("commentText", "E2E 테스트 댓글입니다");
        formData.add("projectId", createdProjectId.toString());

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(formData, headers);

        // When
        ResponseEntity<String> response = testRestTemplate.postForEntity(
            baseUrl + "/kanban/tasks/" + createdTaskId + "/comments", request, String.class);

        // Then
        assertThat(response.getStatusCode()).isIn(HttpStatus.OK, HttpStatus.FOUND, HttpStatus.SEE_OTHER);

        // 댓글 생성 확인
        List<KanbanTaskComment> comments = commentRepository.findByTaskIdAndDeletedOrderByCreatedAtDesc(createdTaskId, "N");
        assertThat(comments).isNotEmpty();

        KanbanTaskComment created = comments.stream()
            .filter(c -> "E2E 테스트 댓글입니다".equals(c.getCommentText()))
            .findFirst()
            .orElse(null);
        assertThat(created).isNotNull();
        createdCommentId = created.getId();
        System.out.println("Created comment ID: " + createdCommentId);
    }

    // ==================== REST API E2E Tests ====================

    @Test
    @Order(10)
    @DisplayName("태스크 상태 변경 API (JSON)")
    void updateTaskStatusApi() {
        // Skip if task was not created
        Assumptions.assumeTrue(createdTaskId != null, "Task was not created");

        // Given
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> payload = Map.of("status", "IN_PROGRESS");
        HttpEntity<Map<String, String>> request = new HttpEntity<>(payload, headers);

        // When
        ResponseEntity<Map> response = testRestTemplate.postForEntity(
            baseUrl + "/kanban/api/tasks/" + createdTaskId + "/status",
            request,
            Map.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("success")).isEqualTo(true);

        // 상태 변경 확인
        Optional<KanbanTask> updated = taskRepository.findByIdAndDeleted(createdTaskId, "N");
        assertThat(updated).isPresent();
        assertThat(updated.get().getStatus()).isEqualTo("IN_PROGRESS");
    }

    @Test
    @Order(11)
    @DisplayName("태스크 내용 수정 API (JSON)")
    void updateTaskContentApi() {
        // Skip if task was not created
        Assumptions.assumeTrue(createdTaskId != null, "Task was not created");

        // Given
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> payload = Map.of(
            "title", "E2E 수정된 태스크",
            "description", "API로 수정된 설명"
        );
        HttpEntity<Map<String, String>> request = new HttpEntity<>(payload, headers);

        // When
        ResponseEntity<Map> response = testRestTemplate.postForEntity(
            baseUrl + "/kanban/api/tasks/" + createdTaskId + "/content",
            request,
            Map.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("success")).isEqualTo(true);

        // 내용 변경 확인
        Optional<KanbanTask> updated = taskRepository.findByIdAndDeleted(createdTaskId, "N");
        assertThat(updated).isPresent();
        assertThat(updated.get().getTitle()).isEqualTo("E2E 수정된 태스크");
    }

    // ==================== Workflow Completion E2E Tests ====================

    @Test
    @Order(12)
    @DisplayName("태스크 완료 처리")
    void completeTask() {
        // Skip if task was not created
        Assumptions.assumeTrue(createdTaskId != null, "Task was not created");

        // Given
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("status", "COMPLETED");
        formData.add("projectId", createdProjectId.toString());

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(formData, headers);

        // When
        ResponseEntity<String> response = testRestTemplate.postForEntity(
            baseUrl + "/kanban/tasks/" + createdTaskId + "/status", request, String.class);

        // Then
        assertThat(response.getStatusCode()).isIn(HttpStatus.OK, HttpStatus.FOUND, HttpStatus.SEE_OTHER);

        // 완료 상태 확인
        Optional<KanbanTask> completed = taskRepository.findByIdAndDeleted(createdTaskId, "N");
        assertThat(completed).isPresent();
        assertThat(completed.get().getStatus()).isEqualTo("COMPLETED");
        assertThat(completed.get().getCompletedAt()).isNotNull();
    }

    // ==================== Cleanup E2E Tests ====================

    @Test
    @Order(13)
    @DisplayName("댓글 삭제")
    void deleteComment() {
        // Skip if comment was not created
        Assumptions.assumeTrue(createdCommentId != null, "Comment was not created");

        // Given
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("projectId", createdProjectId.toString());

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(formData, headers);

        // When
        ResponseEntity<String> response = testRestTemplate.postForEntity(
            baseUrl + "/kanban/comments/" + createdCommentId + "/delete", request, String.class);

        // Then
        assertThat(response.getStatusCode()).isIn(HttpStatus.OK, HttpStatus.FOUND, HttpStatus.SEE_OTHER);

        // 소프트 삭제 확인
        Optional<KanbanTaskComment> deleted = commentRepository.findById(createdCommentId);
        assertThat(deleted).isPresent();
        assertThat(deleted.get().getDeleted()).isEqualTo("Y");
    }

    @Test
    @Order(14)
    @DisplayName("태스크 삭제")
    void deleteTask() {
        // Skip if task was not created
        Assumptions.assumeTrue(createdTaskId != null, "Task was not created");

        // Given
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("projectId", createdProjectId.toString());

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(formData, headers);

        // When
        ResponseEntity<String> response = testRestTemplate.postForEntity(
            baseUrl + "/kanban/tasks/" + createdTaskId + "/delete", request, String.class);

        // Then
        assertThat(response.getStatusCode()).isIn(HttpStatus.OK, HttpStatus.FOUND, HttpStatus.SEE_OTHER);

        // 소프트 삭제 확인
        Optional<KanbanTask> deleted = taskRepository.findById(createdTaskId);
        assertThat(deleted).isPresent();
        assertThat(deleted.get().getDeleted()).isEqualTo("Y");
    }

    @Test
    @Order(15)
    @DisplayName("프로젝트 삭제")
    void deleteProject() {
        // Skip if project was not created
        Assumptions.assumeTrue(createdProjectId != null, "Project was not created");

        // When
        ResponseEntity<String> response = testRestTemplate.postForEntity(
            baseUrl + "/kanban/projects/" + createdProjectId + "/delete",
            null,
            String.class);

        // Then
        assertThat(response.getStatusCode()).isIn(HttpStatus.OK, HttpStatus.FOUND, HttpStatus.SEE_OTHER);

        // 소프트 삭제 확인
        Optional<KanbanProject> deleted = projectRepository.findById(createdProjectId);
        assertThat(deleted).isPresent();
        assertThat(deleted.get().getDeleted()).isEqualTo("Y");
    }

    @Test
    @Order(16)
    @DisplayName("삭제된 프로젝트 제외 보드 조회")
    void viewBoardAfterDeletion() {
        // When
        ResponseEntity<String> response = testRestTemplate.getForEntity(
            baseUrl + "/kanban", String.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        // 삭제된 프로젝트는 표시되지 않음
        assertThat(response.getBody()).doesNotContain("E2E 테스트 프로젝트");
    }
}
