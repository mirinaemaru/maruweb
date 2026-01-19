package com.maru.kanban.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("FileStorageService 단위 테스트")
class FileStorageServiceTest {

    @TempDir
    Path tempDir;

    private FileStorageService fileStorageService;

    @BeforeEach
    void setUp() {
        fileStorageService = new FileStorageService(tempDir.toString());
    }

    // ==================== storeFile Tests ====================

    @Nested
    @DisplayName("storeFile")
    class StoreFileTests {

        @Test
        @DisplayName("파일 저장 - 성공")
        void storeFile_Success() {
            // given
            MockMultipartFile file = new MockMultipartFile(
                    "file", "test.txt", "text/plain", "test content".getBytes());

            // when
            String storedFilename = fileStorageService.storeFile(1L, file);

            // then
            assertThat(storedFilename).contains("test.txt");
            assertThat(storedFilename).matches("[a-f0-9-]+_test\\.txt");

            // Verify file exists
            Path storedPath = tempDir.resolve("1").resolve(storedFilename);
            assertThat(Files.exists(storedPath)).isTrue();
        }

        @Test
        @DisplayName("파일 저장 - 이미지 파일")
        void storeFile_ImageFile() {
            // given
            byte[] imageContent = new byte[]{(byte) 0x89, 'P', 'N', 'G'};
            MockMultipartFile file = new MockMultipartFile(
                    "file", "image.png", "image/png", imageContent);

            // when
            String storedFilename = fileStorageService.storeFile(1L, file);

            // then
            assertThat(storedFilename).contains("image.png");
        }

        @Test
        @DisplayName("파일 저장 - PDF 파일")
        void storeFile_PdfFile() {
            // given
            MockMultipartFile file = new MockMultipartFile(
                    "file", "document.pdf", "application/pdf", "pdf content".getBytes());

            // when
            String storedFilename = fileStorageService.storeFile(1L, file);

            // then
            assertThat(storedFilename).contains("document.pdf");
        }

        @Test
        @DisplayName("파일 저장 - 빈 파일")
        void storeFile_EmptyFile() {
            // given
            MockMultipartFile file = new MockMultipartFile(
                    "file", "empty.txt", "text/plain", new byte[0]);

            // when/then
            assertThatThrownBy(() -> fileStorageService.storeFile(1L, file))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("File is empty");
        }

        @Test
        @DisplayName("파일 저장 - 경로 이탈 시도")
        void storeFile_PathTraversal() {
            // given
            MockMultipartFile file = new MockMultipartFile(
                    "file", "../test.txt", "text/plain", "test content".getBytes());

            // when/then
            assertThatThrownBy(() -> fileStorageService.storeFile(1L, file))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("invalid path sequence");
        }

        @Test
        @DisplayName("파일 저장 - 허용되지 않는 확장자")
        void storeFile_NotAllowedExtension() {
            // given
            MockMultipartFile file = new MockMultipartFile(
                    "file", "malware.exe", "application/octet-stream", "dangerous content".getBytes());

            // when/then
            assertThatThrownBy(() -> fileStorageService.storeFile(1L, file))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("File type not allowed");
        }
    }

    // ==================== loadFileAsResource Tests ====================

    @Nested
    @DisplayName("loadFileAsResource")
    class LoadFileAsResourceTests {

        @Test
        @DisplayName("파일 로드 - 성공")
        void loadFileAsResource_Success() throws IOException {
            // given
            MockMultipartFile file = new MockMultipartFile(
                    "file", "test.txt", "text/plain", "test content".getBytes());
            String storedFilename = fileStorageService.storeFile(1L, file);

            // when
            Resource resource = fileStorageService.loadFileAsResource(1L, storedFilename);

            // then
            assertThat(resource).isNotNull();
            assertThat(resource.exists()).isTrue();
            assertThat(resource.isReadable()).isTrue();
        }

        @Test
        @DisplayName("파일 로드 - 존재하지 않는 파일")
        void loadFileAsResource_NotFound() {
            // when/then
            assertThatThrownBy(() -> fileStorageService.loadFileAsResource(1L, "nonexistent.txt"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("File not found");
        }

        @Test
        @DisplayName("파일 로드 - 경로 이탈 시도")
        void loadFileAsResource_PathTraversal() throws IOException {
            // given
            // 먼저 정상적인 파일 생성
            Path taskDir = tempDir.resolve("1");
            Files.createDirectories(taskDir);
            Path normalFile = taskDir.resolve("normal.txt");
            Files.write(normalFile, "content".getBytes());

            // when/then - 경로 이탈 시도
            assertThatThrownBy(() -> fileStorageService.loadFileAsResource(1L, "../2/secret.txt"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("outside allowed directory");
        }
    }

    // ==================== deleteFile Tests ====================

    @Nested
    @DisplayName("deleteFile")
    class DeleteFileTests {

        @Test
        @DisplayName("파일 삭제 - 성공")
        void deleteFile_Success() {
            // given
            MockMultipartFile file = new MockMultipartFile(
                    "file", "test.txt", "text/plain", "test content".getBytes());
            String storedFilename = fileStorageService.storeFile(1L, file);

            Path storedPath = tempDir.resolve("1").resolve(storedFilename);
            assertThat(Files.exists(storedPath)).isTrue();

            // when
            fileStorageService.deleteFile(1L, storedFilename);

            // then
            assertThat(Files.exists(storedPath)).isFalse();
            // 빈 디렉토리도 삭제되었는지 확인
            assertThat(Files.exists(tempDir.resolve("1"))).isFalse();
        }

        @Test
        @DisplayName("파일 삭제 - 존재하지 않는 파일")
        void deleteFile_NotFound() {
            // when - 예외가 발생하지 않고 로그만 출력됨
            fileStorageService.deleteFile(1L, "nonexistent.txt");

            // then - 예외 없이 완료
        }

        @Test
        @DisplayName("파일 삭제 - 다른 파일이 남아있는 경우 디렉토리 유지")
        void deleteFile_DirectoryNotDeletedIfNotEmpty() {
            // given
            MockMultipartFile file1 = new MockMultipartFile(
                    "file", "test1.txt", "text/plain", "content1".getBytes());
            MockMultipartFile file2 = new MockMultipartFile(
                    "file", "test2.txt", "text/plain", "content2".getBytes());

            String storedFilename1 = fileStorageService.storeFile(1L, file1);
            String storedFilename2 = fileStorageService.storeFile(1L, file2);

            // when
            fileStorageService.deleteFile(1L, storedFilename1);

            // then - 디렉토리는 남아있어야 함
            assertThat(Files.exists(tempDir.resolve("1"))).isTrue();
            assertThat(Files.exists(tempDir.resolve("1").resolve(storedFilename2))).isTrue();
        }
    }

    // ==================== validateFile Tests ====================

    @Nested
    @DisplayName("validateFile")
    class ValidateFileTests {

        @Test
        @DisplayName("파일 검증 - 유효한 파일")
        void validateFile_Valid() {
            // given
            MockMultipartFile file = new MockMultipartFile(
                    "file", "test.txt", "text/plain", "test content".getBytes());

            // when/then (예외 없이 통과해야 함)
            fileStorageService.validateFile(file);
        }

        @Test
        @DisplayName("파일 검증 - null 파일")
        void validateFile_Null() {
            // when/then
            assertThatThrownBy(() -> fileStorageService.validateFile(null))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("File is empty");
        }

        @Test
        @DisplayName("파일 검증 - 빈 파일")
        void validateFile_Empty() {
            // given
            MockMultipartFile file = new MockMultipartFile(
                    "file", "empty.txt", "text/plain", new byte[0]);

            // when/then
            assertThatThrownBy(() -> fileStorageService.validateFile(file))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("File is empty");
        }

        @Test
        @DisplayName("파일 검증 - 파일 크기 초과")
        void validateFile_SizeExceeded() {
            // given - 10MB 초과 파일
            byte[] largeContent = new byte[11 * 1024 * 1024];
            MockMultipartFile file = new MockMultipartFile(
                    "file", "large.txt", "text/plain", largeContent);

            // when/then
            assertThatThrownBy(() -> fileStorageService.validateFile(file))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("File size exceeds maximum allowed size");
        }

        @Test
        @DisplayName("파일 검증 - 허용된 확장자 (txt)")
        void validateFile_AllowedExtension_Txt() {
            // given
            MockMultipartFile file = new MockMultipartFile(
                    "file", "test.txt", "text/plain", "content".getBytes());

            // when/then (예외 없이 통과해야 함)
            fileStorageService.validateFile(file);
        }

        @Test
        @DisplayName("파일 검증 - 허용된 확장자 (md)")
        void validateFile_AllowedExtension_Md() {
            // given
            MockMultipartFile file = new MockMultipartFile(
                    "file", "README.md", "text/markdown", "# Title".getBytes());

            // when/then (예외 없이 통과해야 함)
            fileStorageService.validateFile(file);
        }

        @Test
        @DisplayName("파일 검증 - 허용된 확장자 (png)")
        void validateFile_AllowedExtension_Png() {
            // given
            MockMultipartFile file = new MockMultipartFile(
                    "file", "image.png", "image/png", new byte[]{(byte) 0x89, 'P', 'N', 'G'});

            // when/then (예외 없이 통과해야 함)
            fileStorageService.validateFile(file);
        }

        @Test
        @DisplayName("파일 검증 - 허용된 확장자 (jpg)")
        void validateFile_AllowedExtension_Jpg() {
            // given
            MockMultipartFile file = new MockMultipartFile(
                    "file", "image.jpg", "image/jpeg", "jpeg content".getBytes());

            // when/then (예외 없이 통과해야 함)
            fileStorageService.validateFile(file);
        }

        @Test
        @DisplayName("파일 검증 - 허용된 확장자 (jpeg)")
        void validateFile_AllowedExtension_Jpeg() {
            // given
            MockMultipartFile file = new MockMultipartFile(
                    "file", "image.jpeg", "image/jpeg", "jpeg content".getBytes());

            // when/then (예외 없이 통과해야 함)
            fileStorageService.validateFile(file);
        }

        @Test
        @DisplayName("파일 검증 - 허용된 확장자 (pdf)")
        void validateFile_AllowedExtension_Pdf() {
            // given
            MockMultipartFile file = new MockMultipartFile(
                    "file", "document.pdf", "application/pdf", "pdf content".getBytes());

            // when/then (예외 없이 통과해야 함)
            fileStorageService.validateFile(file);
        }

        @Test
        @DisplayName("파일 검증 - 허용된 확장자 (doc)")
        void validateFile_AllowedExtension_Doc() {
            // given
            MockMultipartFile file = new MockMultipartFile(
                    "file", "document.doc", "application/msword", "doc content".getBytes());

            // when/then (예외 없이 통과해야 함)
            fileStorageService.validateFile(file);
        }

        @Test
        @DisplayName("파일 검증 - 허용된 확장자 (docx)")
        void validateFile_AllowedExtension_Docx() {
            // given
            MockMultipartFile file = new MockMultipartFile(
                    "file", "document.docx",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    "docx content".getBytes());

            // when/then (예외 없이 통과해야 함)
            fileStorageService.validateFile(file);
        }

        @Test
        @DisplayName("파일 검증 - 허용되지 않은 확장자 (exe)")
        void validateFile_NotAllowedExtension_Exe() {
            // given
            MockMultipartFile file = new MockMultipartFile(
                    "file", "program.exe", "application/octet-stream", "exe content".getBytes());

            // when/then
            assertThatThrownBy(() -> fileStorageService.validateFile(file))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("File type not allowed");
        }

        @Test
        @DisplayName("파일 검증 - 허용되지 않은 확장자 (js)")
        void validateFile_NotAllowedExtension_Js() {
            // given
            MockMultipartFile file = new MockMultipartFile(
                    "file", "script.js", "text/javascript", "console.log('hi');".getBytes());

            // when/then
            assertThatThrownBy(() -> fileStorageService.validateFile(file))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("File type not allowed");
        }

        @Test
        @DisplayName("파일 검증 - 허용되지 않은 확장자 (html)")
        void validateFile_NotAllowedExtension_Html() {
            // given
            MockMultipartFile file = new MockMultipartFile(
                    "file", "page.html", "text/html", "<html></html>".getBytes());

            // when/then
            assertThatThrownBy(() -> fileStorageService.validateFile(file))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("File type not allowed");
        }

        @Test
        @DisplayName("파일 검증 - 대소문자 확장자 (PNG)")
        void validateFile_UppercaseExtension() {
            // given
            MockMultipartFile file = new MockMultipartFile(
                    "file", "IMAGE.PNG", "image/png", new byte[]{(byte) 0x89, 'P', 'N', 'G'});

            // when/then (예외 없이 통과해야 함 - 대문자도 허용)
            fileStorageService.validateFile(file);
        }
    }

    // ==================== getFilePath Tests ====================

    @Nested
    @DisplayName("getFilePath")
    class GetFilePathTests {

        @Test
        @DisplayName("파일 경로 반환 - 성공")
        void getFilePath_Success() {
            // when
            String filePath = fileStorageService.getFilePath(1L, "test.txt");

            // then
            assertThat(filePath).contains("1");
            assertThat(filePath).contains("test.txt");
            assertThat(filePath).startsWith(tempDir.toString());
        }

        @Test
        @DisplayName("파일 경로 반환 - 다른 태스크 ID")
        void getFilePath_DifferentTaskId() {
            // when
            String filePath1 = fileStorageService.getFilePath(1L, "test.txt");
            String filePath2 = fileStorageService.getFilePath(2L, "test.txt");

            // then
            assertThat(filePath1).isNotEqualTo(filePath2);
            assertThat(filePath1).contains("/1/");
            assertThat(filePath2).contains("/2/");
        }
    }

    // ==================== Constructor Tests ====================

    @Nested
    @DisplayName("constructor")
    class ConstructorTests {

        @Test
        @DisplayName("디렉토리 생성 - 성공")
        void constructor_CreatesDirectory() {
            // given
            Path newDir = tempDir.resolve("newUploadDir");
            assertThat(Files.exists(newDir)).isFalse();

            // when
            new FileStorageService(newDir.toString());

            // then
            assertThat(Files.exists(newDir)).isTrue();
        }
    }
}
