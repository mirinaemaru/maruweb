package com.maru.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maru.kanban.entity.KanbanProject;
import com.maru.kanban.entity.KanbanTask;
import com.maru.kanban.entity.KanbanTaskComment;
import com.maru.kanban.repository.KanbanProjectRepository;
import com.maru.kanban.repository.KanbanTaskCommentRepository;
import com.maru.kanban.repository.KanbanTaskRepository;
import com.maru.todo.TodoApplication;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = {TodoApplication.class, TestConfig.class})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("KanbanController 통합테스트")
class KanbanControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private KanbanProjectRepository projectRepository;

    @Autowired
    private KanbanTaskRepository taskRepository;

    @Autowired
    private KanbanTaskCommentRepository commentRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        commentRepository.deleteAll();
        taskRepository.deleteAll();
        projectRepository.deleteAll();
    }

    // ==================== Helper Methods ====================

    private KanbanProject createAndSaveProject(String name) {
        KanbanProject project = new KanbanProject();
        project.setName(name);
        project.setDirectoryPath("/test/projects/" + name.toLowerCase().replace(" ", "-"));
        project.setDescription("테스트 프로젝트");
        project.setColor("#667eea");
        project.setDeleted("N");
        return projectRepository.save(project);
    }

    private KanbanTask createAndSaveTask(KanbanProject project, String title, String status) {
        KanbanTask task = new KanbanTask();
        task.setProject(project);
        task.setTitle(title);
        task.setDescription("테스트 태스크 설명");
        task.setStatus(status);
        task.setPriority("MEDIUM");
        task.setTaskNumber(1);
        task.setDisplayOrder(0);
        task.setDeleted("N");
        return taskRepository.save(task);
    }

    private KanbanTaskComment createAndSaveComment(KanbanTask task, String text) {
        KanbanTaskComment comment = new KanbanTaskComment();
        comment.setTask(task);
        comment.setCommentText(text);
        comment.setDeleted("N");
        return commentRepository.save(comment);
    }

    // ==================== Board Page Tests ====================

    @Nested
    @DisplayName("board - GET /kanban")
    class BoardPageTests {

        @Test
        @DisplayName("칸반 보드 페이지 조회 - 프로젝트 없음")
        void board_NoProjects() throws Exception {
            mockMvc.perform(get("/kanban"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("kanban/board"))
                    .andExpect(model().attributeExists("projects"));
        }

        @Test
        @DisplayName("칸반 보드 페이지 조회 - 프로젝트 있음")
        void board_WithProjects() throws Exception {
            // Given
            KanbanProject project = createAndSaveProject("테스트 프로젝트");

            // When & Then
            mockMvc.perform(get("/kanban"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("kanban/board"))
                    .andExpect(model().attributeExists("projects"))
                    .andExpect(model().attributeExists("selectedProject"));
        }

        @Test
        @DisplayName("칸반 보드 페이지 조회 - 특정 프로젝트 선택")
        void board_WithSpecificProject() throws Exception {
            // Given
            KanbanProject project1 = createAndSaveProject("프로젝트1");
            KanbanProject project2 = createAndSaveProject("프로젝트2");

            // When & Then
            mockMvc.perform(get("/kanban")
                            .param("projectId", project2.getId().toString()))
                    .andExpect(status().isOk())
                    .andExpect(view().name("kanban/board"))
                    .andExpect(model().attribute("selectedProject",
                            org.hamcrest.Matchers.hasProperty("id", org.hamcrest.Matchers.is(project2.getId()))));
        }

        @Test
        @DisplayName("칸반 보드 페이지 조회 - 태스크 포함")
        void board_WithTasks() throws Exception {
            // Given
            KanbanProject project = createAndSaveProject("테스트 프로젝트");
            createAndSaveTask(project, "등록된 태스크", "REGISTERED");
            createAndSaveTask(project, "진행중 태스크", "IN_PROGRESS");
            createAndSaveTask(project, "완료된 태스크", "COMPLETED");

            // When & Then
            mockMvc.perform(get("/kanban")
                            .param("projectId", project.getId().toString()))
                    .andExpect(status().isOk())
                    .andExpect(model().attributeExists("tasksByStatus"))
                    .andExpect(model().attributeExists("statuses"))
                    .andExpect(model().attributeExists("selectedProject"));
        }
    }

    // ==================== Project CRUD Tests ====================

    @Nested
    @DisplayName("Project CRUD")
    class ProjectCrudTests {

        @Test
        @DisplayName("프로젝트 생성 폼 조회")
        void newProjectForm() throws Exception {
            mockMvc.perform(get("/kanban/projects/new"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("kanban/project-form"))
                    .andExpect(model().attributeExists("project"));
        }

        @Test
        @DisplayName("프로젝트 생성 - 성공")
        void createProject_Success() throws Exception {
            mockMvc.perform(post("/kanban/projects")
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("name", "새 프로젝트")
                            .param("directoryPath", "/test/new-project")
                            .param("description", "새 프로젝트 설명"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrlPattern("/kanban*"))
                    .andExpect(flash().attributeExists("successMessage"));

            // 데이터베이스 확인
            List<KanbanProject> projects = projectRepository.findByDeletedOrderByDisplayOrderAsc("N");
            assertThat(projects).hasSize(1);
            assertThat(projects.get(0).getName()).isEqualTo("새 프로젝트");
        }

        @Test
        @DisplayName("프로젝트 수정 폼 조회")
        void editProjectForm() throws Exception {
            // Given
            KanbanProject project = createAndSaveProject("수정할 프로젝트");

            // When & Then
            mockMvc.perform(get("/kanban/projects/{id}/edit", project.getId()))
                    .andExpect(status().isOk())
                    .andExpect(view().name("kanban/project-form"))
                    .andExpect(model().attributeExists("project"));
        }

        @Test
        @DisplayName("프로젝트 수정 - 성공")
        void updateProject_Success() throws Exception {
            // Given
            KanbanProject project = createAndSaveProject("원래 이름");

            // When
            mockMvc.perform(post("/kanban/projects/{id}", project.getId())
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("name", "수정된 이름")
                            .param("directoryPath", "/test/updated"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrlPattern("/kanban*"));

            // Then
            Optional<KanbanProject> updated = projectRepository.findByIdAndDeleted(project.getId(), "N");
            assertThat(updated).isPresent();
            assertThat(updated.get().getName()).isEqualTo("수정된 이름");
        }

        @Test
        @DisplayName("프로젝트 삭제 - 성공 (소프트 삭제)")
        void deleteProject_Success() throws Exception {
            // Given
            KanbanProject project = createAndSaveProject("삭제할 프로젝트");

            // When
            mockMvc.perform(post("/kanban/projects/{id}/delete", project.getId()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/kanban"))
                    .andExpect(flash().attributeExists("successMessage"));

            // Then - 소프트 삭제 확인
            Optional<KanbanProject> deleted = projectRepository.findById(project.getId());
            assertThat(deleted).isPresent();
            assertThat(deleted.get().getDeleted()).isEqualTo("Y");
        }

        @Test
        @DisplayName("프로젝트 수정 폼 - 존재하지 않는 프로젝트")
        void editProjectForm_NotFound() throws Exception {
            mockMvc.perform(get("/kanban/projects/{id}/edit", 999L))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/kanban"))
                    .andExpect(flash().attributeExists("errorMessage"));
        }
    }

    // ==================== Task CRUD Tests ====================

    @Nested
    @DisplayName("Task CRUD")
    class TaskCrudTests {

        @Test
        @DisplayName("태스크 생성 - 성공")
        void createTask_Success() throws Exception {
            // Given
            KanbanProject project = createAndSaveProject("테스트 프로젝트");

            // When
            mockMvc.perform(post("/kanban/tasks")
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("projectId", project.getId().toString())
                            .param("title", "새 태스크")
                            .param("description", "태스크 설명")
                            .param("priority", "HIGH"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrlPattern("/kanban*"))
                    .andExpect(flash().attributeExists("successMessage"));

            // Then
            List<KanbanTask> tasks = taskRepository.findByProjectIdAndDeletedOrderByUpdatedAtDesc(project.getId(), "N");
            assertThat(tasks).hasSize(1);
            assertThat(tasks.get(0).getTitle()).isEqualTo("새 태스크");
            assertThat(tasks.get(0).getStatus()).isEqualTo("REGISTERED");
        }

        @Test
        @DisplayName("태스크 생성 - 파일 첨부")
        void createTask_WithFile() throws Exception {
            // Given
            KanbanProject project = createAndSaveProject("테스트 프로젝트");
            MockMultipartFile file = new MockMultipartFile(
                    "file", "test.txt", "text/plain", "test content".getBytes());

            // When
            mockMvc.perform(multipart("/kanban/tasks")
                            .file(file)
                            .param("projectId", project.getId().toString())
                            .param("title", "파일 첨부 태스크"))
                    .andExpect(status().is3xxRedirection());

            // Then
            List<KanbanTask> tasks = taskRepository.findByProjectIdAndDeletedOrderByUpdatedAtDesc(project.getId(), "N");
            assertThat(tasks).hasSize(1);
            assertThat(tasks.get(0).getFileOriginalName()).isEqualTo("test.txt");
        }

        @Test
        @DisplayName("태스크 수정 폼 조회")
        void editTaskForm() throws Exception {
            // Given
            KanbanProject project = createAndSaveProject("테스트 프로젝트");
            KanbanTask task = createAndSaveTask(project, "수정할 태스크", "REGISTERED");

            // When & Then
            mockMvc.perform(get("/kanban/tasks/{id}/edit", task.getId()))
                    .andExpect(status().isOk())
                    .andExpect(view().name("kanban/task-form"))
                    .andExpect(model().attributeExists("task"));
        }

        @Test
        @DisplayName("태스크 수정 - 성공")
        void updateTask_Success() throws Exception {
            // Given
            KanbanProject project = createAndSaveProject("테스트 프로젝트");
            KanbanTask task = createAndSaveTask(project, "원래 제목", "REGISTERED");

            // When
            mockMvc.perform(post("/kanban/tasks/{id}", task.getId())
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("title", "수정된 제목")
                            .param("description", "수정된 설명")
                            .param("priority", "HIGH"))
                    .andExpect(status().is3xxRedirection());

            // Then
            Optional<KanbanTask> updated = taskRepository.findByIdAndDeleted(task.getId(), "N");
            assertThat(updated).isPresent();
            assertThat(updated.get().getTitle()).isEqualTo("수정된 제목");
            assertThat(updated.get().getPriority()).isEqualTo("HIGH");
        }

        @Test
        @DisplayName("태스크 상태 변경 - 성공")
        void updateTaskStatus_Success() throws Exception {
            // Given
            KanbanProject project = createAndSaveProject("테스트 프로젝트");
            KanbanTask task = createAndSaveTask(project, "상태 변경 태스크", "REGISTERED");

            // When
            mockMvc.perform(post("/kanban/tasks/{id}/status", task.getId())
                            .param("status", "IN_PROGRESS")
                            .param("projectId", project.getId().toString()))
                    .andExpect(status().is3xxRedirection());

            // Then
            Optional<KanbanTask> updated = taskRepository.findByIdAndDeleted(task.getId(), "N");
            assertThat(updated).isPresent();
            assertThat(updated.get().getStatus()).isEqualTo("IN_PROGRESS");
        }

        @Test
        @DisplayName("태스크 삭제 - 성공 (소프트 삭제)")
        void deleteTask_Success() throws Exception {
            // Given
            KanbanProject project = createAndSaveProject("테스트 프로젝트");
            KanbanTask task = createAndSaveTask(project, "삭제할 태스크", "REGISTERED");

            // When
            mockMvc.perform(post("/kanban/tasks/{id}/delete", task.getId())
                            .param("projectId", project.getId().toString()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(flash().attributeExists("successMessage"));

            // Then
            Optional<KanbanTask> deleted = taskRepository.findById(task.getId());
            assertThat(deleted).isPresent();
            assertThat(deleted.get().getDeleted()).isEqualTo("Y");
        }

        @Test
        @DisplayName("태스크 수정 폼 - 존재하지 않는 태스크")
        void editTaskForm_NotFound() throws Exception {
            mockMvc.perform(get("/kanban/tasks/{id}/edit", 999L))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/kanban"))
                    .andExpect(flash().attributeExists("errorMessage"));
        }
    }

    // ==================== File Management Tests ====================

    @Nested
    @DisplayName("File Management")
    class FileManagementTests {

        @Test
        @DisplayName("태스크 파일 삭제")
        void deleteTaskFile() throws Exception {
            // Given
            KanbanProject project = createAndSaveProject("테스트 프로젝트");
            KanbanTask task = createAndSaveTask(project, "파일 태스크", "REGISTERED");
            task.setFilePath("/uploads/kanban/1/test.txt");
            task.setFileStoredName("stored-test.txt");
            task.setFileOriginalName("test.txt");
            taskRepository.save(task);

            // When
            mockMvc.perform(post("/kanban/tasks/{id}/delete-file", task.getId())
                            .param("projectId", project.getId().toString()))
                    .andExpect(status().is3xxRedirection());

            // Then - 파일 정보가 삭제되었는지 확인
            Optional<KanbanTask> updated = taskRepository.findByIdAndDeleted(task.getId(), "N");
            assertThat(updated).isPresent();
            assertThat(updated.get().getFilePath()).isNull();
        }

        @Test
        @DisplayName("파일 다운로드 - 파일 없음")
        void downloadFile_NoFile() throws Exception {
            // Given
            KanbanProject project = createAndSaveProject("테스트 프로젝트");
            KanbanTask task = createAndSaveTask(project, "파일 없는 태스크", "REGISTERED");

            // When & Then
            mockMvc.perform(get("/kanban/tasks/{id}/download", task.getId()))
                    .andExpect(status().isNotFound());
        }
    }

    // ==================== Comment Tests ====================

    @Nested
    @DisplayName("Comment Management")
    class CommentTests {

        @Test
        @DisplayName("댓글 추가 - 성공")
        void addComment_Success() throws Exception {
            // Given
            KanbanProject project = createAndSaveProject("테스트 프로젝트");
            KanbanTask task = createAndSaveTask(project, "댓글 테스트", "WAITING_RESPONSE");

            // When
            mockMvc.perform(post("/kanban/tasks/{id}/comments", task.getId())
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("commentText", "테스트 댓글입니다")
                            .param("projectId", project.getId().toString()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(flash().attributeExists("successMessage"));

            // Then
            List<KanbanTaskComment> comments = commentRepository.findByTaskIdAndDeletedOrderByCreatedAtDesc(task.getId(), "N");
            assertThat(comments).hasSize(1);
            assertThat(comments.get(0).getCommentText()).isEqualTo("테스트 댓글입니다");
        }

        @Test
        @DisplayName("댓글 삭제 - 성공")
        void deleteComment_Success() throws Exception {
            // Given
            KanbanProject project = createAndSaveProject("테스트 프로젝트");
            KanbanTask task = createAndSaveTask(project, "댓글 테스트", "WAITING_RESPONSE");
            KanbanTaskComment comment = createAndSaveComment(task, "삭제할 댓글");

            // When
            mockMvc.perform(post("/kanban/comments/{id}/delete", comment.getId())
                            .param("projectId", project.getId().toString()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(flash().attributeExists("successMessage"));

            // Then
            Optional<KanbanTaskComment> deleted = commentRepository.findById(comment.getId());
            assertThat(deleted).isPresent();
            assertThat(deleted.get().getDeleted()).isEqualTo("Y");
        }
    }

    // ==================== REST API Tests ====================

    @Nested
    @DisplayName("REST API")
    class RestApiTests {

        @Test
        @DisplayName("태스크 상태 변경 API - 성공")
        void updateTaskStatusApi_Success() throws Exception {
            // Given
            KanbanProject project = createAndSaveProject("테스트 프로젝트");
            KanbanTask task = createAndSaveTask(project, "API 테스트", "REGISTERED");

            Map<String, String> payload = new HashMap<>();
            payload.put("status", "IN_PROGRESS");

            // When & Then
            mockMvc.perform(post("/kanban/api/tasks/{id}/status", task.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(payload)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            // 데이터 확인
            Optional<KanbanTask> updated = taskRepository.findByIdAndDeleted(task.getId(), "N");
            assertThat(updated).isPresent();
            assertThat(updated.get().getStatus()).isEqualTo("IN_PROGRESS");
        }

        @Test
        @DisplayName("태스크 상태 변경 API - 존재하지 않는 태스크")
        void updateTaskStatusApi_NotFound() throws Exception {
            // Given
            Map<String, String> payload = new HashMap<>();
            payload.put("status", "IN_PROGRESS");

            // When & Then - 존재하지 않는 태스크는 500 에러 또는 예외 발생
            mockMvc.perform(post("/kanban/api/tasks/{id}/status", 999L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(payload)))
                    .andExpect(status().is4xxClientError());
        }

        @Test
        @DisplayName("태스크 내용 수정 API - 성공")
        void updateTaskContentApi_Success() throws Exception {
            // Given
            KanbanProject project = createAndSaveProject("테스트 프로젝트");
            KanbanTask task = createAndSaveTask(project, "원래 제목", "REGISTERED");

            Map<String, String> payload = new HashMap<>();
            payload.put("title", "수정된 제목");
            payload.put("description", "수정된 설명");

            // When & Then
            mockMvc.perform(post("/kanban/api/tasks/{id}/content", task.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(payload)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.title").value("수정된 제목"));

            // 데이터 확인
            Optional<KanbanTask> updated = taskRepository.findByIdAndDeleted(task.getId(), "N");
            assertThat(updated).isPresent();
            assertThat(updated.get().getTitle()).isEqualTo("수정된 제목");
        }

        @Test
        @DisplayName("태스크 내용 수정 API - 빈 제목은 무시됨")
        void updateTaskContentApi_EmptyTitle() throws Exception {
            // Given
            KanbanProject project = createAndSaveProject("테스트 프로젝트");
            KanbanTask task = createAndSaveTask(project, "원래 제목", "REGISTERED");

            Map<String, String> payload = new HashMap<>();
            payload.put("title", "");
            payload.put("description", "설명만 변경");

            // When & Then - 빈 제목은 무시되고 설명만 변경됨
            mockMvc.perform(post("/kanban/api/tasks/{id}/content", task.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(payload)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.title").value("원래 제목")); // 제목은 변경되지 않음
        }
    }

    // ==================== Workflow Tests ====================

    @Nested
    @DisplayName("Workflow Integration")
    class WorkflowTests {

        @Test
        @DisplayName("태스크 워크플로우 - REGISTERED → IN_PROGRESS → COMPLETED")
        void taskWorkflow_FullCycle() throws Exception {
            // Given
            KanbanProject project = createAndSaveProject("워크플로우 테스트");
            KanbanTask task = createAndSaveTask(project, "워크플로우 태스크", "REGISTERED");

            // Step 1: REGISTERED → IN_PROGRESS
            mockMvc.perform(post("/kanban/tasks/{id}/status", task.getId())
                            .param("status", "IN_PROGRESS")
                            .param("projectId", project.getId().toString()))
                    .andExpect(status().is3xxRedirection());

            Optional<KanbanTask> inProgress = taskRepository.findByIdAndDeleted(task.getId(), "N");
            assertThat(inProgress.get().getStatus()).isEqualTo("IN_PROGRESS");

            // Step 2: IN_PROGRESS → COMPLETED
            mockMvc.perform(post("/kanban/tasks/{id}/status", task.getId())
                            .param("status", "COMPLETED")
                            .param("projectId", project.getId().toString()))
                    .andExpect(status().is3xxRedirection());

            Optional<KanbanTask> completed = taskRepository.findByIdAndDeleted(task.getId(), "N");
            assertThat(completed.get().getStatus()).isEqualTo("COMPLETED");
            assertThat(completed.get().getCompletedAt()).isNotNull();
        }

        @Test
        @DisplayName("태스크 워크플로우 - WAITING_RESPONSE 댓글 추가")
        void taskWorkflow_WaitingWithComment() throws Exception {
            // Given
            KanbanProject project = createAndSaveProject("대기 테스트");
            KanbanTask task = createAndSaveTask(project, "응답 대기 태스크", "REGISTERED");

            // Step 1: REGISTERED → WAITING_RESPONSE
            mockMvc.perform(post("/kanban/tasks/{id}/status", task.getId())
                            .param("status", "WAITING_RESPONSE")
                            .param("projectId", project.getId().toString()))
                    .andExpect(status().is3xxRedirection());

            // Step 2: 댓글 추가
            mockMvc.perform(post("/kanban/tasks/{id}/comments", task.getId())
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("commentText", "응답 내용입니다")
                            .param("projectId", project.getId().toString()))
                    .andExpect(status().is3xxRedirection());

            // Verify
            List<KanbanTaskComment> comments = commentRepository.findByTaskIdAndDeletedOrderByCreatedAtDesc(task.getId(), "N");
            assertThat(comments).hasSize(1);
        }
    }
}
