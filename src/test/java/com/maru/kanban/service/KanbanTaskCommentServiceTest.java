package com.maru.kanban.service;

import com.maru.kanban.entity.KanbanTask;
import com.maru.kanban.entity.KanbanTaskComment;
import com.maru.kanban.repository.KanbanTaskCommentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("KanbanTaskCommentService 테스트")
class KanbanTaskCommentServiceTest {

    @Mock
    private KanbanTaskCommentRepository commentRepository;

    @InjectMocks
    private KanbanTaskCommentService commentService;

    private KanbanTask testTask;
    private KanbanTaskComment testComment;

    @BeforeEach
    void setUp() {
        testTask = new KanbanTask();
        testTask.setId(1L);
        testTask.setTitle("테스트 태스크");

        testComment = new KanbanTaskComment();
        testComment.setId(1L);
        testComment.setTask(testTask);
        testComment.setCommentText("테스트 댓글입니다.");
        testComment.setCreatedAt(LocalDateTime.now());
        testComment.setDeleted("N");
    }

    @Nested
    @DisplayName("getCommentsByTaskId 메서드")
    class GetCommentsByTaskIdTest {

        @Test
        @DisplayName("태스크 ID로 댓글 목록 조회 - 성공")
        void getCommentsByTaskId_Success() {
            // given
            Long taskId = 1L;
            KanbanTaskComment comment1 = createComment(1L, "첫 번째 댓글");
            KanbanTaskComment comment2 = createComment(2L, "두 번째 댓글");
            List<KanbanTaskComment> expectedComments = Arrays.asList(comment1, comment2);

            when(commentRepository.findByTaskIdAndDeletedOrderByCreatedAtDesc(taskId, "N"))
                    .thenReturn(expectedComments);

            // when
            List<KanbanTaskComment> result = commentService.getCommentsByTaskId(taskId);

            // then
            assertThat(result).hasSize(2);
            assertThat(result).containsExactly(comment1, comment2);
            verify(commentRepository).findByTaskIdAndDeletedOrderByCreatedAtDesc(taskId, "N");
        }

        @Test
        @DisplayName("태스크 ID로 댓글 목록 조회 - 댓글 없음")
        void getCommentsByTaskId_EmptyList() {
            // given
            Long taskId = 999L;
            when(commentRepository.findByTaskIdAndDeletedOrderByCreatedAtDesc(taskId, "N"))
                    .thenReturn(Collections.emptyList());

            // when
            List<KanbanTaskComment> result = commentService.getCommentsByTaskId(taskId);

            // then
            assertThat(result).isEmpty();
            verify(commentRepository).findByTaskIdAndDeletedOrderByCreatedAtDesc(taskId, "N");
        }

        @Test
        @DisplayName("삭제된 댓글은 조회되지 않음")
        void getCommentsByTaskId_ExcludesDeleted() {
            // given
            Long taskId = 1L;
            KanbanTaskComment activeComment = createComment(1L, "활성 댓글");
            // 삭제된 댓글은 Repository에서 필터링되므로 결과에 포함되지 않음
            List<KanbanTaskComment> expectedComments = Collections.singletonList(activeComment);

            when(commentRepository.findByTaskIdAndDeletedOrderByCreatedAtDesc(taskId, "N"))
                    .thenReturn(expectedComments);

            // when
            List<KanbanTaskComment> result = commentService.getCommentsByTaskId(taskId);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getCommentText()).isEqualTo("활성 댓글");
        }
    }

    @Nested
    @DisplayName("createComment 메서드")
    class CreateCommentTest {

        @Test
        @DisplayName("댓글 생성 - 성공")
        void createComment_Success() {
            // given
            Long taskId = 1L;
            String commentText = "새로운 댓글입니다.";

            when(commentRepository.save(any(KanbanTaskComment.class))).thenAnswer(invocation -> {
                KanbanTaskComment saved = invocation.getArgument(0);
                saved.setId(1L);
                return saved;
            });

            // when
            KanbanTaskComment result = commentService.createComment(taskId, commentText);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getCommentText()).isEqualTo(commentText);
            assertThat(result.getTask().getId()).isEqualTo(taskId);
            verify(commentRepository).save(any(KanbanTaskComment.class));
        }

        @Test
        @DisplayName("빈 댓글 텍스트로 생성")
        void createComment_EmptyText() {
            // given
            Long taskId = 1L;
            String commentText = "";

            when(commentRepository.save(any(KanbanTaskComment.class))).thenAnswer(invocation -> {
                KanbanTaskComment saved = invocation.getArgument(0);
                saved.setId(1L);
                return saved;
            });

            // when
            KanbanTaskComment result = commentService.createComment(taskId, commentText);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getCommentText()).isEmpty();
            verify(commentRepository).save(any(KanbanTaskComment.class));
        }

        @Test
        @DisplayName("긴 댓글 텍스트로 생성")
        void createComment_LongText() {
            // given
            Long taskId = 1L;
            String commentText = "A".repeat(1000);

            when(commentRepository.save(any(KanbanTaskComment.class))).thenAnswer(invocation -> {
                KanbanTaskComment saved = invocation.getArgument(0);
                saved.setId(1L);
                return saved;
            });

            // when
            KanbanTaskComment result = commentService.createComment(taskId, commentText);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getCommentText()).hasSize(1000);
            verify(commentRepository).save(any(KanbanTaskComment.class));
        }

        @Test
        @DisplayName("null 댓글 텍스트로 생성")
        void createComment_NullText() {
            // given
            Long taskId = 1L;
            String commentText = null;

            when(commentRepository.save(any(KanbanTaskComment.class))).thenAnswer(invocation -> {
                KanbanTaskComment saved = invocation.getArgument(0);
                saved.setId(1L);
                return saved;
            });

            // when
            KanbanTaskComment result = commentService.createComment(taskId, commentText);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getCommentText()).isNull();
            verify(commentRepository).save(any(KanbanTaskComment.class));
        }
    }

    @Nested
    @DisplayName("deleteComment 메서드")
    class DeleteCommentTest {

        @Test
        @DisplayName("댓글 삭제 - 성공 (소프트 삭제)")
        void deleteComment_Success() {
            // given
            Long commentId = 1L;
            KanbanTaskComment existingComment = createComment(commentId, "삭제할 댓글");
            existingComment.setDeleted("N");

            when(commentRepository.findById(commentId)).thenReturn(Optional.of(existingComment));
            when(commentRepository.save(any(KanbanTaskComment.class))).thenReturn(existingComment);

            // when
            commentService.deleteComment(commentId);

            // then
            assertThat(existingComment.getDeleted()).isEqualTo("Y");
            verify(commentRepository).findById(commentId);
            verify(commentRepository).save(existingComment);
        }

        @Test
        @DisplayName("존재하지 않는 댓글 삭제 시도")
        void deleteComment_NotFound() {
            // given
            Long commentId = 999L;
            when(commentRepository.findById(commentId)).thenReturn(Optional.empty());

            // when
            commentService.deleteComment(commentId);

            // then
            verify(commentRepository).findById(commentId);
            verify(commentRepository, never()).save(any(KanbanTaskComment.class));
        }

        @Test
        @DisplayName("이미 삭제된 댓글 다시 삭제")
        void deleteComment_AlreadyDeleted() {
            // given
            Long commentId = 1L;
            KanbanTaskComment deletedComment = createComment(commentId, "이미 삭제된 댓글");
            deletedComment.setDeleted("Y");

            when(commentRepository.findById(commentId)).thenReturn(Optional.of(deletedComment));
            when(commentRepository.save(any(KanbanTaskComment.class))).thenReturn(deletedComment);

            // when
            commentService.deleteComment(commentId);

            // then
            assertThat(deletedComment.getDeleted()).isEqualTo("Y");
            verify(commentRepository).findById(commentId);
            verify(commentRepository).save(deletedComment);
        }
    }

    // Helper method
    private KanbanTaskComment createComment(Long id, String text) {
        KanbanTaskComment comment = new KanbanTaskComment();
        comment.setId(id);
        comment.setTask(testTask);
        comment.setCommentText(text);
        comment.setCreatedAt(LocalDateTime.now());
        comment.setDeleted("N");
        return comment;
    }
}
