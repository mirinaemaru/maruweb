package com.maru.kanban.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Service for handling file uploads, downloads, and deletions for Kanban tasks
 */
@Service
@Slf4j
public class FileStorageService {

    private final Path fileStorageLocation;
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
            "txt", "md", "pdf", "doc", "docx", "png", "jpg", "jpeg"
    );

    public FileStorageService(@Value("${kanban.file.upload-dir}") String uploadDir) {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
            log.info("File storage directory created at: {}", this.fileStorageLocation);
        } catch (IOException ex) {
            log.error("Could not create file storage directory", ex);
            throw new RuntimeException("Could not create file storage directory", ex);
        }
    }

    /**
     * Store uploaded file with UUID-based naming
     *
     * @param taskId Task ID for subdirectory organization
     * @param file MultipartFile to store
     * @return Stored filename (UUID_originalname)
     * @throws RuntimeException if file storage fails
     */
    public String storeFile(Long taskId, MultipartFile file) {
        // Validate file
        validateFile(file);

        // Clean filename and generate UUID-based name
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String fileExtension = getFileExtension(originalFilename);
        String storedFilename = UUID.randomUUID().toString() + "_" + originalFilename;

        try {
            // Check for invalid characters
            if (originalFilename.contains("..")) {
                throw new RuntimeException("Filename contains invalid path sequence: " + originalFilename);
            }

            // Create task-specific subdirectory
            Path taskDirectory = this.fileStorageLocation.resolve(taskId.toString());
            Files.createDirectories(taskDirectory);

            // Store file
            Path targetLocation = taskDirectory.resolve(storedFilename);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            log.info("File stored successfully: taskId={}, filename={}", taskId, storedFilename);
            return storedFilename;

        } catch (IOException ex) {
            log.error("Failed to store file: taskId={}, filename={}", taskId, originalFilename, ex);
            throw new RuntimeException("Failed to store file: " + originalFilename, ex);
        }
    }

    /**
     * Load file as Resource for download
     *
     * @param taskId Task ID
     * @param filename Stored filename
     * @return Resource for file download
     * @throws RuntimeException if file not found
     */
    public Resource loadFileAsResource(Long taskId, String filename) {
        try {
            Path taskDirectory = this.fileStorageLocation.resolve(taskId.toString());
            Path filePath = taskDirectory.resolve(filename).normalize();

            // Security check: ensure file is within allowed directory
            if (!filePath.startsWith(taskDirectory)) {
                throw new RuntimeException("File path is outside allowed directory");
            }

            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("File not found: " + filename);
            }
        } catch (MalformedURLException ex) {
            log.error("File not found: taskId={}, filename={}", taskId, filename, ex);
            throw new RuntimeException("File not found: " + filename, ex);
        }
    }

    /**
     * Delete file from storage
     *
     * @param taskId Task ID
     * @param filename Stored filename
     */
    public void deleteFile(Long taskId, String filename) {
        try {
            Path taskDirectory = this.fileStorageLocation.resolve(taskId.toString());
            Path filePath = taskDirectory.resolve(filename).normalize();

            // Security check: ensure file is within allowed directory
            if (!filePath.startsWith(taskDirectory)) {
                log.warn("Attempted to delete file outside allowed directory: {}", filePath);
                return;
            }

            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("File deleted successfully: taskId={}, filename={}", taskId, filename);

                // Clean up empty task directory
                if (Files.list(taskDirectory).findAny().isEmpty()) {
                    Files.delete(taskDirectory);
                    log.info("Empty task directory deleted: taskId={}", taskId);
                }
            } else {
                log.warn("File not found for deletion: taskId={}, filename={}", taskId, filename);
            }
        } catch (IOException ex) {
            log.error("Failed to delete file: taskId={}, filename={}", taskId, filename, ex);
            throw new RuntimeException("Failed to delete file: " + filename, ex);
        }
    }

    /**
     * Validate uploaded file (size and extension)
     *
     * @param file MultipartFile to validate
     * @throws RuntimeException if validation fails
     */
    public void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("File is empty");
        }

        // Check file size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new RuntimeException("File size exceeds maximum allowed size (10MB)");
        }

        // Check file extension
        String filename = StringUtils.cleanPath(file.getOriginalFilename());
        String extension = getFileExtension(filename).toLowerCase();

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new RuntimeException("File type not allowed. Allowed types: " + String.join(", ", ALLOWED_EXTENSIONS));
        }
    }

    /**
     * Get file extension from filename
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }

    /**
     * Get full file path for a task's file
     *
     * @param taskId Task ID
     * @param filename Stored filename
     * @return Full file path string
     */
    public String getFilePath(Long taskId, String filename) {
        Path taskDirectory = this.fileStorageLocation.resolve(taskId.toString());
        return taskDirectory.resolve(filename).toString();
    }
}
