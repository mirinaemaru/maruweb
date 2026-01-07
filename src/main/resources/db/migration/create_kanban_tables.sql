-- Kanban Module Tables
-- Created: 2026-01-07
-- Description: Project-based task management with file upload support

-- Projects table (categories/directories)
CREATE TABLE IF NOT EXISTS kanban_projects (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL COMMENT 'Project name',
    directory_path VARCHAR(500) NOT NULL COMMENT 'Absolute path to ~/projects/{projectName}',
    description TEXT COMMENT 'Project description',
    color VARCHAR(7) DEFAULT '#667eea' COMMENT 'Hex color for visual distinction',
    display_order INT DEFAULT 0 COMMENT 'For custom ordering',
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    deleted VARCHAR(1) DEFAULT 'N' NOT NULL COMMENT 'Soft delete flag',

    INDEX idx_deleted (deleted),
    INDEX idx_display_order (display_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Kanban projects';

-- Tasks table
CREATE TABLE IF NOT EXISTS kanban_tasks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL COMMENT 'Foreign key to kanban_projects',
    title VARCHAR(500) NOT NULL COMMENT 'Task title',
    description TEXT COMMENT 'Task description',
    status VARCHAR(20) DEFAULT 'REGISTERED' NOT NULL
        COMMENT 'REGISTERED, WAITING_RESPONSE, IN_PROGRESS, COMPLETED',
    priority VARCHAR(10) DEFAULT 'MEDIUM' COMMENT 'LOW, MEDIUM, HIGH',
    display_order INT DEFAULT 0 COMMENT 'Order within status column',

    -- File Management
    file_original_name VARCHAR(255) COMMENT 'Original filename uploaded by user',
    file_stored_name VARCHAR(255) COMMENT 'UUID-based stored filename',
    file_path VARCHAR(1000) COMMENT 'Full path to stored file',
    file_size BIGINT COMMENT 'File size in bytes',
    file_content_type VARCHAR(100) COMMENT 'MIME type',

    -- Automation Context
    automation_notes TEXT COMMENT 'Notes for Claude/developer to execute',
    last_executed_at DATETIME COMMENT 'When task was last worked on',

    -- Timestamps
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    completed_at DATETIME COMMENT 'When moved to COMPLETED status',
    deleted VARCHAR(1) DEFAULT 'N' NOT NULL COMMENT 'Soft delete flag',

    FOREIGN KEY (project_id) REFERENCES kanban_projects(id) ON DELETE CASCADE,
    INDEX idx_project_status (project_id, status, deleted),
    INDEX idx_status_order (status, display_order),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Kanban tasks';
