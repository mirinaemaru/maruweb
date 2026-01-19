package com.maru.kanban.service;

import com.maru.kanban.entity.KanbanProject;
import com.maru.kanban.entity.KanbanTask;
import com.maru.kanban.repository.KanbanProjectRepository;
import com.maru.kanban.repository.KanbanTaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("KanbanTaskService 단위 테스트")
class KanbanTaskServiceTest {

    @Mock
    private KanbanTaskRepository taskRepository;

    @Mock
    private KanbanProjectRepository projectRepository;

    @Mock
    private FileStorageService fileStorageService;

    private KanbanTaskService kanbanTaskService;

    @BeforeEach
    void setUp() {
        kanbanTaskService = new KanbanTaskService(taskRepository, projectRepository, fileStorageService);
    }

    private KanbanProject createProject(Long id, String name) {
        KanbanProject project = new KanbanProject();
        project.setId(id);
        project.setName(name);
        project.setDeleted("N");
        return project;
    }

    private KanbanTask createTask(Long id, String title, KanbanProject project) {
        KanbanTask task = new KanbanTask();
        task.setId(id);
        task.setTitle(title);
        task.setDescription("태스크 설명");
        task.setStatus("REGISTERED");
        task.setProject(project);
        task.setDeleted("N");
        task.setTaskNumber(1);
        task.setDisplayOrder(0);
        return task;
    }

    // ==================== findAllTasksByProject Tests ====================

    @Nested
    @DisplayName("findAllTasksByProject")
    class FindAllTasksByProjectTests {

        @Test
        @DisplayName("프로젝트별 모든 태스크 조회 - 성공")
        void findAllTasksByProject_Success() {
            // given
            KanbanProject project = createProject(1L, "프로젝트");
            KanbanTask task1 = createTask(1L, "태스크1", project);
            KanbanTask task2 = createTask(2L, "태스크2", project);

            when(taskRepository.findByProjectIdAndDeletedOrderByUpdatedAtDesc(1L, "N"))
                    .thenReturn(Arrays.asList(task1, task2));

            // when
            List<KanbanTask> result = kanbanTaskService.findAllTasksByProject(1L);

            // then
            assertThat(result).hasSize(2);
        }
    }

    // ==================== findTasksByProjectAndStatus Tests ====================

    @Nested
    @DisplayName("findTasksByProjectAndStatus")
    class FindTasksByProjectAndStatusTests {

        @Test
        @DisplayName("프로젝트와 상태별 태스크 조회 - 성공")
        void findTasksByProjectAndStatus_Success() {
            // given
            KanbanProject project = createProject(1L, "프로젝트");
            KanbanTask task1 = createTask(1L, "태스크1", project);

            when(taskRepository.findByProjectIdAndStatusAndDeletedOrderByUpdatedAtDesc(1L, "REGISTERED", "N"))
                    .thenReturn(Arrays.asList(task1));

            // when
            List<KanbanTask> result = kanbanTaskService.findTasksByProjectAndStatus(1L, "REGISTERED");

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStatus()).isEqualTo("REGISTERED");
        }
    }

    // ==================== findTaskById Tests ====================

    @Nested
    @DisplayName("findTaskById")
    class FindTaskByIdTests {

        @Test
        @DisplayName("ID로 태스크 조회 - 존재하는 경우")
        void findTaskById_Found() {
            // given
            KanbanProject project = createProject(1L, "프로젝트");
            KanbanTask task = createTask(1L, "태스크", project);
            when(taskRepository.findByIdAndDeleted(1L, "N")).thenReturn(Optional.of(task));

            // when
            Optional<KanbanTask> result = kanbanTaskService.findTaskById(1L);

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getTitle()).isEqualTo("태스크");
        }

        @Test
        @DisplayName("ID로 태스크 조회 - 존재하지 않음")
        void findTaskById_NotFound() {
            // given
            when(taskRepository.findByIdAndDeleted(999L, "N")).thenReturn(Optional.empty());

            // when
            Optional<KanbanTask> result = kanbanTaskService.findTaskById(999L);

            // then
            assertThat(result).isEmpty();
        }
    }

    // ==================== findTasksWithFiles Tests ====================

    @Nested
    @DisplayName("findTasksWithFiles")
    class FindTasksWithFilesTests {

        @Test
        @DisplayName("파일이 첨부된 태스크 조회 - 성공")
        void findTasksWithFiles_Success() {
            // given
            KanbanProject project = createProject(1L, "프로젝트");
            KanbanTask task = createTask(1L, "파일 있는 태스크", project);
            task.setFilePath("/uploads/test.txt");

            when(taskRepository.findTasksWithFilesByProject(1L, "N"))
                    .thenReturn(Arrays.asList(task));

            // when
            List<KanbanTask> result = kanbanTaskService.findTasksWithFiles(1L);

            // then
            assertThat(result).hasSize(1);
        }
    }

    // ==================== countTasksByStatus Tests ====================

    @Nested
    @DisplayName("countTasksByStatus")
    class CountTasksByStatusTests {

        @Test
        @DisplayName("상태별 태스크 수 조회")
        void countTasksByStatus_Success() {
            // given
            when(taskRepository.countByProjectIdAndStatusAndDeleted(1L, "REGISTERED", "N"))
                    .thenReturn(5L);

            // when
            long count = kanbanTaskService.countTasksByStatus(1L, "REGISTERED");

            // then
            assertThat(count).isEqualTo(5L);
        }
    }

    // ==================== createTask Tests ====================

    @Nested
    @DisplayName("createTask")
    class CreateTaskTests {

        @Test
        @DisplayName("태스크 생성 - 파일 없음")
        void createTask_WithoutFile() {
            // given
            KanbanProject project = createProject(1L, "프로젝트");
            KanbanTask newTask = new KanbanTask();
            newTask.setTitle("새 태스크");
            newTask.setProject(project);
            // KanbanTask 엔티티에서 displayOrder=0, status="REGISTERED"가 기본값
            // 따라서 getMaxDisplayOrderByProjectAndStatus는 호출되지 않음

            when(projectRepository.findByIdAndDeleted(1L, "N")).thenReturn(Optional.of(project));
            when(taskRepository.getMaxTaskNumberByProject(1L)).thenReturn(0);
            when(taskRepository.save(any(KanbanTask.class))).thenAnswer(inv -> {
                KanbanTask saved = inv.getArgument(0);
                saved.setId(1L);
                return saved;
            });

            // when
            KanbanTask result = kanbanTaskService.createTask(newTask, null);

            // then
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getStatus()).isEqualTo("REGISTERED");
            assertThat(result.getDisplayOrder()).isEqualTo(0); // 기본값 유지
            assertThat(result.getTaskNumber()).isEqualTo(1);
        }

        @Test
        @DisplayName("태스크 생성 - 파일 첨부")
        void createTask_WithFile() {
            // given
            KanbanProject project = createProject(1L, "프로젝트");
            KanbanTask newTask = new KanbanTask();
            newTask.setTitle("새 태스크");
            newTask.setProject(project);
            // KanbanTask 엔티티에서 displayOrder=0, status="REGISTERED"가 기본값

            MockMultipartFile file = new MockMultipartFile(
                    "file", "test.txt", "text/plain", "test content".getBytes());

            when(projectRepository.findByIdAndDeleted(1L, "N")).thenReturn(Optional.of(project));
            when(taskRepository.getMaxTaskNumberByProject(1L)).thenReturn(0);
            when(taskRepository.save(any(KanbanTask.class))).thenAnswer(inv -> {
                KanbanTask saved = inv.getArgument(0);
                if (saved.getId() == null) saved.setId(1L);
                return saved;
            });
            when(fileStorageService.storeFile(anyLong(), any(MultipartFile.class))).thenReturn("stored_test.txt");
            when(fileStorageService.getFilePath(anyLong(), anyString())).thenReturn("/uploads/kanban/1/stored_test.txt");

            // when
            KanbanTask result = kanbanTaskService.createTask(newTask, file);

            // then
            assertThat(result.getFileOriginalName()).isEqualTo("test.txt");
            verify(fileStorageService).validateFile(file);
            verify(fileStorageService).storeFile(anyLong(), any(MultipartFile.class));
        }

        @Test
        @DisplayName("태스크 생성 - 프로젝트 없음")
        void createTask_ProjectNotFound() {
            // given
            KanbanProject project = createProject(999L, "없는 프로젝트");
            KanbanTask newTask = new KanbanTask();
            newTask.setTitle("새 태스크");
            newTask.setProject(project);

            when(projectRepository.findByIdAndDeleted(999L, "N")).thenReturn(Optional.empty());

            // when/then
            assertThatThrownBy(() -> kanbanTaskService.createTask(newTask, null))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Project not found: 999");
        }
    }

    // ==================== updateTask Tests ====================

    @Nested
    @DisplayName("updateTask")
    class UpdateTaskTests {

        @Test
        @DisplayName("태스크 수정 - 성공")
        void updateTask_Success() {
            // given
            KanbanProject project = createProject(1L, "프로젝트");
            KanbanTask existing = createTask(1L, "기존 태스크", project);

            KanbanTask updated = new KanbanTask();
            updated.setTitle("수정된 태스크");
            updated.setDescription("수정된 설명");
            updated.setPriority("HIGH");
            updated.setAutomationNotes("자동화 노트");

            when(taskRepository.findByIdAndDeleted(1L, "N")).thenReturn(Optional.of(existing));
            when(taskRepository.save(any(KanbanTask.class))).thenAnswer(inv -> inv.getArgument(0));

            // when
            KanbanTask result = kanbanTaskService.updateTask(1L, updated, null);

            // then
            assertThat(result.getTitle()).isEqualTo("수정된 태스크");
            assertThat(result.getDescription()).isEqualTo("수정된 설명");
            assertThat(result.getPriority()).isEqualTo("HIGH");
        }

        @Test
        @DisplayName("태스크 수정 - 존재하지 않음")
        void updateTask_NotFound() {
            // given
            KanbanTask updated = new KanbanTask();
            updated.setTitle("수정된 태스크");

            when(taskRepository.findByIdAndDeleted(999L, "N")).thenReturn(Optional.empty());

            // when/then
            assertThatThrownBy(() -> kanbanTaskService.updateTask(999L, updated, null))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Task not found: 999");
        }
    }

    // ==================== updateTaskStatus Tests ====================

    @Nested
    @DisplayName("updateTaskStatus")
    class UpdateTaskStatusTests {

        @Test
        @DisplayName("태스크 상태 변경 - 성공")
        void updateTaskStatus_Success() {
            // given
            KanbanProject project = createProject(1L, "프로젝트");
            KanbanTask task = createTask(1L, "태스크", project);

            when(taskRepository.findByIdAndDeleted(1L, "N")).thenReturn(Optional.of(task));
            when(taskRepository.getMaxDisplayOrderByProjectAndStatus(1L, "IN_PROGRESS")).thenReturn(2);
            when(taskRepository.save(any(KanbanTask.class))).thenAnswer(inv -> inv.getArgument(0));

            // when
            KanbanTask result = kanbanTaskService.updateTaskStatus(1L, "IN_PROGRESS");

            // then
            assertThat(result.getStatus()).isEqualTo("IN_PROGRESS");
            assertThat(result.getDisplayOrder()).isEqualTo(3);
        }

        @Test
        @DisplayName("태스크 상태 변경 - 존재하지 않음")
        void updateTaskStatus_NotFound() {
            // given
            when(taskRepository.findByIdAndDeleted(999L, "N")).thenReturn(Optional.empty());

            // when/then
            assertThatThrownBy(() -> kanbanTaskService.updateTaskStatus(999L, "IN_PROGRESS"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Task not found: 999");
        }
    }

    // ==================== moveTask Tests ====================

    @Nested
    @DisplayName("moveTask")
    class MoveTaskTests {

        @Test
        @DisplayName("태스크 이동 - 성공")
        void moveTask_Success() {
            // given
            KanbanProject project = createProject(1L, "프로젝트");
            KanbanTask task = createTask(1L, "태스크", project);

            when(taskRepository.findByIdAndDeleted(1L, "N")).thenReturn(Optional.of(task));
            when(taskRepository.save(any(KanbanTask.class))).thenAnswer(inv -> inv.getArgument(0));

            // when
            KanbanTask result = kanbanTaskService.moveTask(1L, "COMPLETED", 5);

            // then
            assertThat(result.getStatus()).isEqualTo("COMPLETED");
            assertThat(result.getDisplayOrder()).isEqualTo(5);
        }
    }

    // ==================== deleteTask Tests ====================

    @Nested
    @DisplayName("deleteTask")
    class DeleteTaskTests {

        @Test
        @DisplayName("태스크 삭제 - 파일 없음")
        void deleteTask_WithoutFile() {
            // given
            KanbanProject project = createProject(1L, "프로젝트");
            KanbanTask task = createTask(1L, "태스크", project);

            when(taskRepository.findByIdAndDeleted(1L, "N")).thenReturn(Optional.of(task));
            when(taskRepository.save(any(KanbanTask.class))).thenAnswer(inv -> inv.getArgument(0));

            // when
            kanbanTaskService.deleteTask(1L);

            // then
            ArgumentCaptor<KanbanTask> captor = ArgumentCaptor.forClass(KanbanTask.class);
            verify(taskRepository).save(captor.capture());
            assertThat(captor.getValue().getDeleted()).isEqualTo("Y");
        }

        @Test
        @DisplayName("태스크 삭제 - 파일도 함께 삭제")
        void deleteTask_WithFile() {
            // given
            KanbanProject project = createProject(1L, "프로젝트");
            KanbanTask task = createTask(1L, "태스크", project);
            task.setFilePath("/uploads/kanban/1/test.txt");
            task.setFileStoredName("test.txt");

            when(taskRepository.findByIdAndDeleted(1L, "N")).thenReturn(Optional.of(task));
            when(taskRepository.save(any(KanbanTask.class))).thenAnswer(inv -> inv.getArgument(0));

            // when
            kanbanTaskService.deleteTask(1L);

            // then
            verify(fileStorageService).deleteFile(1L, "test.txt");
        }

        @Test
        @DisplayName("태스크 삭제 - 존재하지 않음")
        void deleteTask_NotFound() {
            // given
            when(taskRepository.findByIdAndDeleted(999L, "N")).thenReturn(Optional.empty());

            // when/then
            assertThatThrownBy(() -> kanbanTaskService.deleteTask(999L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Task not found: 999");
        }
    }

    // ==================== deleteTaskFile Tests ====================

    @Nested
    @DisplayName("deleteTaskFile")
    class DeleteTaskFileTests {

        @Test
        @DisplayName("태스크 파일 삭제 - 성공")
        void deleteTaskFile_Success() {
            // given
            KanbanProject project = createProject(1L, "프로젝트");
            KanbanTask task = createTask(1L, "태스크", project);
            task.setFilePath("/uploads/kanban/1/test.txt");
            task.setFileStoredName("test.txt");

            when(taskRepository.findByIdAndDeleted(1L, "N")).thenReturn(Optional.of(task));
            when(taskRepository.save(any(KanbanTask.class))).thenAnswer(inv -> inv.getArgument(0));

            // when
            kanbanTaskService.deleteTaskFile(1L);

            // then
            verify(fileStorageService).deleteFile(1L, "test.txt");
        }

        @Test
        @DisplayName("태스크 파일 삭제 - 파일 없음")
        void deleteTaskFile_NoFile() {
            // given
            KanbanProject project = createProject(1L, "프로젝트");
            KanbanTask task = createTask(1L, "태스크", project);

            when(taskRepository.findByIdAndDeleted(1L, "N")).thenReturn(Optional.of(task));

            // when
            kanbanTaskService.deleteTaskFile(1L);

            // then
            verify(fileStorageService, never()).deleteFile(anyLong(), anyString());
            verify(taskRepository, never()).save(any());
        }
    }

    // ==================== updateDisplayOrder Tests ====================

    @Nested
    @DisplayName("updateDisplayOrder")
    class UpdateDisplayOrderTests {

        @Test
        @DisplayName("표시 순서 변경 - 성공")
        void updateDisplayOrder_Success() {
            // given
            KanbanProject project = createProject(1L, "프로젝트");
            KanbanTask task = createTask(1L, "태스크", project);

            when(taskRepository.findByIdAndDeleted(1L, "N")).thenReturn(Optional.of(task));
            when(taskRepository.save(any(KanbanTask.class))).thenAnswer(inv -> inv.getArgument(0));

            // when
            kanbanTaskService.updateDisplayOrder(1L, 10);

            // then
            ArgumentCaptor<KanbanTask> captor = ArgumentCaptor.forClass(KanbanTask.class);
            verify(taskRepository).save(captor.capture());
            assertThat(captor.getValue().getDisplayOrder()).isEqualTo(10);
        }
    }

    // ==================== updateTaskContent Tests ====================

    @Nested
    @DisplayName("updateTaskContent")
    class UpdateTaskContentTests {

        @Test
        @DisplayName("태스크 내용 수정 - 성공")
        void updateTaskContent_Success() {
            // given
            KanbanProject project = createProject(1L, "프로젝트");
            KanbanTask task = createTask(1L, "태스크", project);

            when(taskRepository.findByIdAndDeleted(1L, "N")).thenReturn(Optional.of(task));
            when(taskRepository.save(any(KanbanTask.class))).thenAnswer(inv -> inv.getArgument(0));

            // when
            KanbanTask result = kanbanTaskService.updateTaskContent(1L, "수정된 제목", "수정된 설명");

            // then
            assertThat(result.getTitle()).isEqualTo("수정된 제목");
            assertThat(result.getDescription()).isEqualTo("수정된 설명");
        }

        @Test
        @DisplayName("태스크 내용 수정 - 빈 설명은 null로")
        void updateTaskContent_EmptyDescription() {
            // given
            KanbanProject project = createProject(1L, "프로젝트");
            KanbanTask task = createTask(1L, "태스크", project);

            when(taskRepository.findByIdAndDeleted(1L, "N")).thenReturn(Optional.of(task));
            when(taskRepository.save(any(KanbanTask.class))).thenAnswer(inv -> inv.getArgument(0));

            // when
            KanbanTask result = kanbanTaskService.updateTaskContent(1L, "제목", "   ");

            // then
            assertThat(result.getDescription()).isNull();
        }
    }
}
