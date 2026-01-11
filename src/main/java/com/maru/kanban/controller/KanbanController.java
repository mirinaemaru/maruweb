package com.maru.kanban.controller;

import com.maru.kanban.entity.KanbanProject;
import com.maru.kanban.entity.KanbanTask;
import com.maru.kanban.entity.TaskStatus;
import com.maru.kanban.service.FileStorageService;
import com.maru.kanban.service.KanbanProjectService;
import com.maru.kanban.service.KanbanTaskCommentService;
import com.maru.kanban.service.KanbanTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import java.util.HashMap;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller for Kanban board management
 */
@Controller
@RequestMapping("/kanban")
@Slf4j
@RequiredArgsConstructor
public class KanbanController {

    private final KanbanProjectService projectService;
    private final KanbanTaskService taskService;
    private final FileStorageService fileStorageService;
    private final KanbanTaskCommentService commentService;

    /**
     * Main Kanban board page
     */
    @GetMapping
    public String board(@RequestParam(required = false) Long projectId, Model model) {
        List<KanbanProject> projects = projectService.findAllProjects();
        model.addAttribute("projects", projects);

        // Select first project if no projectId specified
        if (projectId == null && !projects.isEmpty()) {
            projectId = projects.get(0).getId();
        }

        if (projectId != null) {
            final Long finalProjectId = projectId;  // Make effectively final for lambda
            KanbanProject selectedProject = projectService.findProjectById(finalProjectId).orElse(null);
            model.addAttribute("selectedProject", selectedProject);

            // Get tasks grouped by status
            Map<String, List<KanbanTask>> tasksByStatus = TaskStatus.getAllStatuses().stream()
                    .collect(Collectors.toMap(
                            TaskStatus::name,
                            status -> taskService.findTasksByProjectAndStatus(finalProjectId, status.name())
                    ));
            model.addAttribute("tasksByStatus", tasksByStatus);
            model.addAttribute("statuses", TaskStatus.values());
        }

        model.addAttribute("newTask", new KanbanTask());
        return "kanban/board";
    }

    // ===== Project Management =====

    /**
     * Show project creation form
     */
    @GetMapping("/projects/new")
    public String newProjectForm(Model model) {
        model.addAttribute("project", new KanbanProject());
        model.addAttribute("isEdit", false);
        return "kanban/project-form";
    }

    /**
     * Create new project
     */
    @PostMapping("/projects")
    public String createProject(@ModelAttribute KanbanProject project, RedirectAttributes redirectAttributes) {
        try {
            KanbanProject created = projectService.createProject(project);
            redirectAttributes.addFlashAttribute("successMessage", "프로젝트가 생성되었습니다.");
            return "redirect:/kanban?projectId=" + created.getId();
        } catch (Exception e) {
            log.error("Failed to create project", e);
            redirectAttributes.addFlashAttribute("errorMessage", "프로젝트 생성 실패: " + e.getMessage());
            return "redirect:/kanban/projects/new";
        }
    }

    /**
     * Show project edit form
     */
    @GetMapping("/projects/{id}/edit")
    public String editProjectForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        return projectService.findProjectById(id)
                .map(project -> {
                    model.addAttribute("project", project);
                    model.addAttribute("isEdit", true);
                    return "kanban/project-form";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "프로젝트를 찾을 수 없습니다.");
                    return "redirect:/kanban";
                });
    }

    /**
     * Update project
     */
    @PostMapping("/projects/{id}")
    public String updateProject(@PathVariable Long id,
                                @ModelAttribute KanbanProject project,
                                RedirectAttributes redirectAttributes) {
        try {
            projectService.updateProject(id, project);
            redirectAttributes.addFlashAttribute("successMessage", "프로젝트가 수정되었습니다.");
            return "redirect:/kanban?projectId=" + id;
        } catch (Exception e) {
            log.error("Failed to update project: id={}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "프로젝트 수정 실패: " + e.getMessage());
            return "redirect:/kanban/projects/" + id + "/edit";
        }
    }

    /**
     * Delete project
     */
    @PostMapping("/projects/{id}/delete")
    public String deleteProject(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            projectService.deleteProject(id);
            redirectAttributes.addFlashAttribute("successMessage", "프로젝트가 삭제되었습니다.");
        } catch (Exception e) {
            log.error("Failed to delete project: id={}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "프로젝트 삭제 실패: " + e.getMessage());
        }
        return "redirect:/kanban";
    }

    // ===== Task Management =====

    /**
     * Create new task
     */
    @PostMapping("/tasks")
    public String createTask(@RequestParam Long projectId,
                             @ModelAttribute KanbanTask task,
                             @RequestParam(required = false) MultipartFile file,
                             RedirectAttributes redirectAttributes) {
        try {
            // Set project
            KanbanProject project = new KanbanProject();
            project.setId(projectId);
            task.setProject(project);

            taskService.createTask(task, file);
            redirectAttributes.addFlashAttribute("successMessage", "태스크가 생성되었습니다.");
        } catch (Exception e) {
            log.error("Failed to create task", e);
            redirectAttributes.addFlashAttribute("errorMessage", "태스크 생성 실패: " + e.getMessage());
        }
        return "redirect:/kanban?projectId=" + projectId;
    }

    /**
     * Show task edit form
     */
    @GetMapping("/tasks/{id}/edit")
    public String editTaskForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        return taskService.findTaskById(id)
                .map(task -> {
                    model.addAttribute("task", task);
                    model.addAttribute("statuses", TaskStatus.values());
                    model.addAttribute("isEdit", true);
                    model.addAttribute("comments", commentService.getCommentsByTaskId(id));
                    return "kanban/task-form";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "태스크를 찾을 수 없습니다.");
                    return "redirect:/kanban";
                });
    }

    /**
     * Update task
     */
    @PostMapping("/tasks/{id}")
    public String updateTask(@PathVariable Long id,
                             @ModelAttribute KanbanTask task,
                             @RequestParam(required = false) MultipartFile file,
                             RedirectAttributes redirectAttributes) {
        try {
            KanbanTask updated = taskService.updateTask(id, task, file);
            redirectAttributes.addFlashAttribute("successMessage", "태스크가 수정되었습니다.");
            return "redirect:/kanban?projectId=" + updated.getProject().getId();
        } catch (Exception e) {
            log.error("Failed to update task: id={}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "태스크 수정 실패: " + e.getMessage());
            return "redirect:/kanban/tasks/" + id + "/edit";
        }
    }

    /**
     * Update task status (move to next column)
     */
    @PostMapping("/tasks/{id}/status")
    public String updateTaskStatus(@PathVariable Long id,
                                    @RequestParam String status,
                                    @RequestParam Long projectId,
                                    RedirectAttributes redirectAttributes) {
        try {
            taskService.updateTaskStatus(id, status);
            redirectAttributes.addFlashAttribute("successMessage", "태스크 상태가 변경되었습니다.");
        } catch (Exception e) {
            log.error("Failed to update task status: id={}, status={}", id, status, e);
            redirectAttributes.addFlashAttribute("errorMessage", "상태 변경 실패: " + e.getMessage());
        }
        return "redirect:/kanban?projectId=" + projectId;
    }

    /**
     * Delete task
     */
    @PostMapping("/tasks/{id}/delete")
    public String deleteTask(@PathVariable Long id,
                             @RequestParam Long projectId,
                             RedirectAttributes redirectAttributes) {
        try {
            taskService.deleteTask(id);
            redirectAttributes.addFlashAttribute("successMessage", "태스크가 삭제되었습니다.");
        } catch (Exception e) {
            log.error("Failed to delete task: id={}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "태스크 삭제 실패: " + e.getMessage());
        }
        return "redirect:/kanban?projectId=" + projectId;
    }

    // ===== File Management =====

    /**
     * Download task file
     */
    @GetMapping("/tasks/{id}/download")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long id) {
        try {
            KanbanTask task = taskService.findTaskById(id)
                    .orElseThrow(() -> new RuntimeException("Task not found: " + id));

            if (!task.hasFile()) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = fileStorageService.loadFileAsResource(task.getId(), task.getFileStoredName());

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(task.getFileContentType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + task.getFileOriginalName() + "\"")
                    .body(resource);
        } catch (Exception e) {
            log.error("Failed to download file: taskId={}", id, e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Preview task image file (inline display)
     */
    @GetMapping("/tasks/{id}/preview")
    public ResponseEntity<Resource> previewFile(@PathVariable Long id) {
        try {
            KanbanTask task = taskService.findTaskById(id)
                    .orElseThrow(() -> new RuntimeException("Task not found: " + id));

            if (!task.hasFile() || !task.isImageFile()) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = fileStorageService.loadFileAsResource(task.getId(), task.getFileStoredName());

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(task.getFileContentType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + task.getFileOriginalName() + "\"")
                    .body(resource);
        } catch (Exception e) {
            log.error("Failed to preview file: taskId={}", id, e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Delete task file
     */
    @PostMapping("/tasks/{id}/delete-file")
    public String deleteTaskFile(@PathVariable Long id,
                                  @RequestParam Long projectId,
                                  RedirectAttributes redirectAttributes) {
        try {
            taskService.deleteTaskFile(id);
            redirectAttributes.addFlashAttribute("successMessage", "파일이 삭제되었습니다.");
        } catch (Exception e) {
            log.error("Failed to delete task file: id={}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "파일 삭제 실패: " + e.getMessage());
        }
        return "redirect:/kanban?projectId=" + projectId;
    }

    // ===== Comment Management =====

    /**
     * Add comment to a task
     */
    @PostMapping("/tasks/{id}/comments")
    public String addComment(@PathVariable Long id,
                             @RequestParam String commentText,
                             @RequestParam Long projectId,
                             RedirectAttributes redirectAttributes) {
        try {
            commentService.createComment(id, commentText);
            redirectAttributes.addFlashAttribute("successMessage", "댓글이 추가되었습니다.");
        } catch (Exception e) {
            log.error("Failed to add comment: taskId={}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "댓글 추가 실패: " + e.getMessage());
        }
        return "redirect:/kanban?projectId=" + projectId;
    }

    /**
     * Delete comment
     */
    @PostMapping("/comments/{id}/delete")
    public String deleteComment(@PathVariable Long id,
                                @RequestParam Long projectId,
                                RedirectAttributes redirectAttributes) {
        try {
            commentService.deleteComment(id);
            redirectAttributes.addFlashAttribute("successMessage", "댓글이 삭제되었습니다.");
        } catch (Exception e) {
            log.error("Failed to delete comment: id={}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "댓글 삭제 실패: " + e.getMessage());
        }
        return "redirect:/kanban?projectId=" + projectId;
    }

    // ===== REST API for Drag and Drop =====

    /**
     * Update task status via AJAX (for drag and drop)
     */
    @PostMapping("/api/tasks/{id}/status")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateTaskStatusApi(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload) {
        Map<String, Object> response = new HashMap<>();
        try {
            String status = payload.get("status");
            if (status == null || status.isEmpty()) {
                response.put("success", false);
                response.put("message", "Status is required");
                return ResponseEntity.badRequest().body(response);
            }

            taskService.updateTaskStatus(id, status);
            response.put("success", true);
            response.put("message", "Task status updated successfully");
            response.put("newStatus", status);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to update task status via API: id={}", id, e);
            response.put("success", false);
            response.put("message", "Failed to update status: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
