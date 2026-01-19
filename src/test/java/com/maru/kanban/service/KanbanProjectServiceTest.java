package com.maru.kanban.service;

import com.maru.kanban.entity.KanbanProject;
import com.maru.kanban.repository.KanbanProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("KanbanProjectService 단위 테스트")
class KanbanProjectServiceTest {

    @Mock
    private KanbanProjectRepository projectRepository;

    private KanbanProjectService kanbanProjectService;

    @BeforeEach
    void setUp() {
        kanbanProjectService = new KanbanProjectService(projectRepository);
    }

    private KanbanProject createProject(Long id, String name, String directoryPath) {
        KanbanProject project = new KanbanProject();
        project.setId(id);
        project.setName(name);
        project.setDirectoryPath(directoryPath);
        project.setDescription("프로젝트 설명");
        project.setColor("#667eea");
        project.setDisplayOrder(0);
        project.setDeleted("N");
        return project;
    }

    // ==================== findAllProjects Tests ====================

    @Nested
    @DisplayName("findAllProjects")
    class FindAllProjectsTests {

        @Test
        @DisplayName("모든 프로젝트 조회 - 성공")
        void findAllProjects_Success() {
            // given
            KanbanProject project1 = createProject(1L, "프로젝트1", "/projects/p1");
            KanbanProject project2 = createProject(2L, "프로젝트2", "/projects/p2");

            when(projectRepository.findByDeletedOrderByDisplayOrderAsc("N"))
                    .thenReturn(Arrays.asList(project1, project2));

            // when
            List<KanbanProject> result = kanbanProjectService.findAllProjects();

            // then
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("모든 프로젝트 조회 - 빈 결과")
        void findAllProjects_Empty() {
            // given
            when(projectRepository.findByDeletedOrderByDisplayOrderAsc("N"))
                    .thenReturn(Collections.emptyList());

            // when
            List<KanbanProject> result = kanbanProjectService.findAllProjects();

            // then
            assertThat(result).isEmpty();
        }
    }

    // ==================== findAllProjectsWithTasks Tests ====================

    @Nested
    @DisplayName("findAllProjectsWithTasks")
    class FindAllProjectsWithTasksTests {

        @Test
        @DisplayName("태스크와 함께 모든 프로젝트 조회 - 성공")
        void findAllProjectsWithTasks_Success() {
            // given
            KanbanProject project = createProject(1L, "프로젝트", "/projects/p1");
            when(projectRepository.findAllWithTasks("N")).thenReturn(Arrays.asList(project));

            // when
            List<KanbanProject> result = kanbanProjectService.findAllProjectsWithTasks();

            // then
            assertThat(result).hasSize(1);
        }
    }

    // ==================== findProjectById Tests ====================

    @Nested
    @DisplayName("findProjectById")
    class FindProjectByIdTests {

        @Test
        @DisplayName("ID로 프로젝트 조회 - 존재하는 경우")
        void findProjectById_Found() {
            // given
            KanbanProject project = createProject(1L, "프로젝트", "/projects/p1");
            when(projectRepository.findByIdAndDeleted(1L, "N")).thenReturn(Optional.of(project));

            // when
            Optional<KanbanProject> result = kanbanProjectService.findProjectById(1L);

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getName()).isEqualTo("프로젝트");
        }

        @Test
        @DisplayName("ID로 프로젝트 조회 - 존재하지 않음")
        void findProjectById_NotFound() {
            // given
            when(projectRepository.findByIdAndDeleted(999L, "N")).thenReturn(Optional.empty());

            // when
            Optional<KanbanProject> result = kanbanProjectService.findProjectById(999L);

            // then
            assertThat(result).isEmpty();
        }
    }

    // ==================== createProject Tests ====================

    @Nested
    @DisplayName("createProject")
    class CreateProjectTests {

        @TempDir
        Path tempDir;

        @Test
        @DisplayName("프로젝트 생성 - 성공")
        void createProject_Success() {
            // given
            KanbanProject newProject = new KanbanProject();
            newProject.setName("새 프로젝트");
            newProject.setDirectoryPath(tempDir.toString());
            // KanbanProject 엔티티에서 displayOrder=0, color="#667eea"가 기본값
            // 따라서 findByDeletedOrderByDisplayOrderAsc는 호출되지 않음

            when(projectRepository.findByDirectoryPathAndDeleted(tempDir.toString(), "N"))
                    .thenReturn(Optional.empty());
            when(projectRepository.save(any(KanbanProject.class))).thenAnswer(inv -> {
                KanbanProject saved = inv.getArgument(0);
                saved.setId(1L);
                return saved;
            });

            // when
            KanbanProject result = kanbanProjectService.createProject(newProject);

            // then
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getDisplayOrder()).isEqualTo(0); // 기본값
            assertThat(result.getColor()).isEqualTo("#667eea"); // 기본값
        }

        @Test
        @DisplayName("프로젝트 생성 - 중복 경로")
        void createProject_DuplicatePath() {
            // given
            KanbanProject existing = createProject(1L, "기존 프로젝트", tempDir.toString());
            KanbanProject newProject = new KanbanProject();
            newProject.setName("새 프로젝트");
            newProject.setDirectoryPath(tempDir.toString());

            when(projectRepository.findByDirectoryPathAndDeleted(tempDir.toString(), "N"))
                    .thenReturn(Optional.of(existing));

            // when/then
            assertThatThrownBy(() -> kanbanProjectService.createProject(newProject))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Project with directory path already exists");
        }

        @Test
        @DisplayName("프로젝트 생성 - 빈 경로")
        void createProject_EmptyPath() {
            // given
            KanbanProject newProject = new KanbanProject();
            newProject.setName("새 프로젝트");
            newProject.setDirectoryPath("");

            // when/then
            assertThatThrownBy(() -> kanbanProjectService.createProject(newProject))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Directory path cannot be empty");
        }

        @Test
        @DisplayName("프로젝트 생성 - 사용자 지정 색상")
        void createProject_WithCustomColor() {
            // given
            KanbanProject newProject = new KanbanProject();
            newProject.setName("새 프로젝트");
            newProject.setDirectoryPath(tempDir.toString());
            newProject.setColor("#ff0000"); // 기본값 대신 사용자 지정 색상

            when(projectRepository.findByDirectoryPathAndDeleted(tempDir.toString(), "N"))
                    .thenReturn(Optional.empty());
            when(projectRepository.save(any(KanbanProject.class))).thenAnswer(inv -> inv.getArgument(0));

            // when
            KanbanProject result = kanbanProjectService.createProject(newProject);

            // then
            assertThat(result.getColor()).isEqualTo("#ff0000");
        }

        @Test
        @DisplayName("프로젝트 생성 - displayOrder 자동 설정")
        void createProject_AutoDisplayOrder() {
            // given
            KanbanProject existing1 = createProject(1L, "기존 프로젝트1", "/projects/p1");
            KanbanProject existing2 = createProject(2L, "기존 프로젝트2", "/projects/p2");

            KanbanProject newProject = new KanbanProject();
            newProject.setName("새 프로젝트");
            newProject.setDirectoryPath(tempDir.toString());
            newProject.setDisplayOrder(null); // 명시적으로 null 설정하여 자동 설정 트리거

            when(projectRepository.findByDirectoryPathAndDeleted(tempDir.toString(), "N"))
                    .thenReturn(Optional.empty());
            when(projectRepository.findByDeletedOrderByDisplayOrderAsc("N"))
                    .thenReturn(Arrays.asList(existing1, existing2));
            when(projectRepository.save(any(KanbanProject.class))).thenAnswer(inv -> inv.getArgument(0));

            // when
            KanbanProject result = kanbanProjectService.createProject(newProject);

            // then
            assertThat(result.getDisplayOrder()).isEqualTo(2); // 기존 2개의 크기
        }
    }

    // ==================== updateProject Tests ====================

    @Nested
    @DisplayName("updateProject")
    class UpdateProjectTests {

        @TempDir
        Path tempDir;

        @Test
        @DisplayName("프로젝트 수정 - 성공")
        void updateProject_Success() {
            // given
            KanbanProject existing = createProject(1L, "기존 프로젝트", tempDir.toString());

            KanbanProject updated = new KanbanProject();
            updated.setName("수정된 프로젝트");
            updated.setDirectoryPath(tempDir.toString());
            updated.setDescription("수정된 설명");
            updated.setColor("#00ff00");
            updated.setDisplayOrder(5);

            when(projectRepository.findByIdAndDeleted(1L, "N")).thenReturn(Optional.of(existing));
            when(projectRepository.save(any(KanbanProject.class))).thenAnswer(inv -> inv.getArgument(0));

            // when
            KanbanProject result = kanbanProjectService.updateProject(1L, updated);

            // then
            assertThat(result.getName()).isEqualTo("수정된 프로젝트");
            assertThat(result.getDescription()).isEqualTo("수정된 설명");
            assertThat(result.getColor()).isEqualTo("#00ff00");
            assertThat(result.getDisplayOrder()).isEqualTo(5);
        }

        @Test
        @DisplayName("프로젝트 수정 - 경로 변경 성공")
        void updateProject_PathChange() {
            // given
            Path newDir = tempDir.resolve("newdir");
            KanbanProject existing = createProject(1L, "기존 프로젝트", tempDir.toString());

            KanbanProject updated = new KanbanProject();
            updated.setName("수정된 프로젝트");
            updated.setDirectoryPath(newDir.toString());
            updated.setDescription("설명");
            updated.setColor("#667eea");
            updated.setDisplayOrder(0);

            when(projectRepository.findByIdAndDeleted(1L, "N")).thenReturn(Optional.of(existing));
            when(projectRepository.findByDirectoryPathAndDeleted(newDir.toString(), "N"))
                    .thenReturn(Optional.empty());
            when(projectRepository.save(any(KanbanProject.class))).thenAnswer(inv -> inv.getArgument(0));

            // when
            KanbanProject result = kanbanProjectService.updateProject(1L, updated);

            // then
            assertThat(result.getDirectoryPath()).isEqualTo(newDir.toString());
        }

        @Test
        @DisplayName("프로젝트 수정 - 경로 중복")
        void updateProject_DuplicatePath() {
            // given
            KanbanProject existing = createProject(1L, "기존 프로젝트", "/projects/p1");
            KanbanProject otherProject = createProject(2L, "다른 프로젝트", "/projects/p2");

            KanbanProject updated = new KanbanProject();
            updated.setName("수정된 프로젝트");
            updated.setDirectoryPath("/projects/p2");

            when(projectRepository.findByIdAndDeleted(1L, "N")).thenReturn(Optional.of(existing));
            when(projectRepository.findByDirectoryPathAndDeleted("/projects/p2", "N"))
                    .thenReturn(Optional.of(otherProject));

            // when/then
            assertThatThrownBy(() -> kanbanProjectService.updateProject(1L, updated))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Project with directory path already exists");
        }

        @Test
        @DisplayName("프로젝트 수정 - 존재하지 않음")
        void updateProject_NotFound() {
            // given
            KanbanProject updated = new KanbanProject();
            updated.setName("수정된 프로젝트");

            when(projectRepository.findByIdAndDeleted(999L, "N")).thenReturn(Optional.empty());

            // when/then
            assertThatThrownBy(() -> kanbanProjectService.updateProject(999L, updated))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Project not found: 999");
        }
    }

    // ==================== deleteProject Tests ====================

    @Nested
    @DisplayName("deleteProject")
    class DeleteProjectTests {

        @Test
        @DisplayName("프로젝트 삭제 - 성공")
        void deleteProject_Success() {
            // given
            KanbanProject project = createProject(1L, "프로젝트", "/projects/p1");
            when(projectRepository.findByIdAndDeleted(1L, "N")).thenReturn(Optional.of(project));
            when(projectRepository.save(any(KanbanProject.class))).thenAnswer(inv -> inv.getArgument(0));

            // when
            kanbanProjectService.deleteProject(1L);

            // then
            ArgumentCaptor<KanbanProject> captor = ArgumentCaptor.forClass(KanbanProject.class);
            verify(projectRepository).save(captor.capture());
            assertThat(captor.getValue().getDeleted()).isEqualTo("Y");
        }

        @Test
        @DisplayName("프로젝트 삭제 - 존재하지 않음")
        void deleteProject_NotFound() {
            // given
            when(projectRepository.findByIdAndDeleted(999L, "N")).thenReturn(Optional.empty());

            // when/then
            assertThatThrownBy(() -> kanbanProjectService.deleteProject(999L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Project not found: 999");
        }
    }

    // ==================== validateDirectoryPath Tests ====================

    @Nested
    @DisplayName("validateDirectoryPath")
    class ValidateDirectoryPathTests {

        @TempDir
        Path tempDir;

        @Test
        @DisplayName("경로 검증 - null 경로")
        void validateDirectoryPath_Null() {
            // when/then
            assertThatThrownBy(() -> kanbanProjectService.validateDirectoryPath(null))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Directory path cannot be empty");
        }

        @Test
        @DisplayName("경로 검증 - 빈 경로")
        void validateDirectoryPath_Empty() {
            // when/then
            assertThatThrownBy(() -> kanbanProjectService.validateDirectoryPath(""))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Directory path cannot be empty");
        }

        @Test
        @DisplayName("경로 검증 - 공백만 있는 경로")
        void validateDirectoryPath_Whitespace() {
            // when/then
            assertThatThrownBy(() -> kanbanProjectService.validateDirectoryPath("   "))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Directory path cannot be empty");
        }

        @Test
        @DisplayName("경로 검증 - 유효한 디렉토리 경로")
        void validateDirectoryPath_ValidDirectory() {
            // when/then (예외 없이 통과해야 함)
            kanbanProjectService.validateDirectoryPath(tempDir.toString());
        }

        @Test
        @DisplayName("경로 검증 - 파일 경로 (디렉토리 아님)")
        void validateDirectoryPath_FileNotDirectory() throws Exception {
            // given
            Path file = tempDir.resolve("test.txt");
            java.nio.file.Files.createFile(file);

            // when/then
            assertThatThrownBy(() -> kanbanProjectService.validateDirectoryPath(file.toString()))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Path is not a directory");
        }
    }

    // ==================== isNameDuplicate Tests ====================

    @Nested
    @DisplayName("isNameDuplicate")
    class IsNameDuplicateTests {

        @Test
        @DisplayName("이름 중복 체크 - 중복인 경우")
        void isNameDuplicate_True() {
            // given
            when(projectRepository.existsByNameAndDeletedAndIdNot("테스트", "N", -1L))
                    .thenReturn(true);

            // when
            boolean result = kanbanProjectService.isNameDuplicate("테스트", null);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("이름 중복 체크 - 중복 아닌 경우")
        void isNameDuplicate_False() {
            // given
            when(projectRepository.existsByNameAndDeletedAndIdNot("새이름", "N", -1L))
                    .thenReturn(false);

            // when
            boolean result = kanbanProjectService.isNameDuplicate("새이름", null);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("이름 중복 체크 - 수정 시 자기 자신 제외")
        void isNameDuplicate_ExcludeSelf() {
            // given
            when(projectRepository.existsByNameAndDeletedAndIdNot("테스트", "N", 1L))
                    .thenReturn(false);

            // when
            boolean result = kanbanProjectService.isNameDuplicate("테스트", 1L);

            // then
            assertThat(result).isFalse();
            verify(projectRepository).existsByNameAndDeletedAndIdNot("테스트", "N", 1L);
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
            KanbanProject project = createProject(1L, "프로젝트", "/projects/p1");
            when(projectRepository.findByIdAndDeleted(1L, "N")).thenReturn(Optional.of(project));
            when(projectRepository.save(any(KanbanProject.class))).thenAnswer(inv -> inv.getArgument(0));

            // when
            kanbanProjectService.updateDisplayOrder(1L, 10);

            // then
            ArgumentCaptor<KanbanProject> captor = ArgumentCaptor.forClass(KanbanProject.class);
            verify(projectRepository).save(captor.capture());
            assertThat(captor.getValue().getDisplayOrder()).isEqualTo(10);
        }

        @Test
        @DisplayName("표시 순서 변경 - 존재하지 않음")
        void updateDisplayOrder_NotFound() {
            // given
            when(projectRepository.findByIdAndDeleted(999L, "N")).thenReturn(Optional.empty());

            // when/then
            assertThatThrownBy(() -> kanbanProjectService.updateDisplayOrder(999L, 10))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Project not found: 999");
        }
    }
}
