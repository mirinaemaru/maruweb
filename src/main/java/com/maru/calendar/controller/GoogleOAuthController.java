package com.maru.calendar.controller;

import com.maru.calendar.service.GoogleCalendarSyncService;
import com.maru.calendar.service.GoogleOAuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@RequestMapping("/calendar/oauth2")
@Slf4j
public class GoogleOAuthController {

    private final GoogleOAuthService oauthService;
    private final GoogleCalendarSyncService syncService;

    public GoogleOAuthController(GoogleOAuthService oauthService,
                                GoogleCalendarSyncService syncService) {
        this.oauthService = oauthService;
        this.syncService = syncService;
    }

    @GetMapping("/connect")
    public String connect(RedirectAttributes redirectAttributes) {
        try {
            String authUrl = oauthService.getAuthorizationUrl();
            return "redirect:" + authUrl;
        } catch (Exception e) {
            log.error("Failed to generate OAuth URL: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Failed to connect to Google Calendar: " + e.getMessage());
            return "redirect:/calendar";
        }
    }

    @GetMapping("/callback")
    public String handleCallback(@RequestParam(required = false) String code,
                                 @RequestParam(required = false) String error,
                                 RedirectAttributes redirectAttributes) {
        if (error != null) {
            log.error("OAuth error: {}", error);
            redirectAttributes.addFlashAttribute("error", "Failed to connect to Google Calendar: " + error);
            return "redirect:/calendar";
        }

        if (code == null) {
            log.error("No authorization code received");
            redirectAttributes.addFlashAttribute("error", "No authorization code received");
            return "redirect:/calendar";
        }

        try {
            // Exchange code for tokens
            oauthService.exchangeCodeForTokens(code);

            // Perform initial sync
            log.info("Performing initial sync after OAuth connection...");
            Map<String, Integer> syncResult = syncService.performBidirectionalSync();

            redirectAttributes.addFlashAttribute("success",
                    String.format("Successfully connected to Google Calendar! " +
                            "Synced %d events from Google, created %d events in Google.",
                            syncResult.getOrDefault("pull_created", 0),
                            syncResult.getOrDefault("push_created", 0)));
        } catch (Exception e) {
            log.error("Failed to complete OAuth flow: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Failed to complete OAuth flow: " + e.getMessage());
        }

        return "redirect:/calendar";
    }

    @PostMapping("/disconnect")
    public String disconnect(RedirectAttributes redirectAttributes) {
        try {
            oauthService.disconnect();
            redirectAttributes.addFlashAttribute("success", "Disconnected from Google Calendar");
        } catch (Exception e) {
            log.error("Failed to disconnect: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Failed to disconnect: " + e.getMessage());
        }

        return "redirect:/calendar";
    }

    @PostMapping("/sync")
    public String manualSync(RedirectAttributes redirectAttributes) {
        try {
            if (!oauthService.isAuthenticated()) {
                redirectAttributes.addFlashAttribute("error", "Not connected to Google Calendar");
                return "redirect:/calendar";
            }

            Map<String, Integer> result = syncService.performBidirectionalSync();

            String message = String.format("Sync completed! " +
                    "Push: %d created, %d updated, %d deleted. " +
                    "Pull: %d created, %d updated.",
                    result.getOrDefault("push_created", 0),
                    result.getOrDefault("push_updated", 0),
                    result.getOrDefault("push_deleted", 0),
                    result.getOrDefault("pull_created", 0),
                    result.getOrDefault("pull_updated", 0));

            redirectAttributes.addFlashAttribute("success", message);
        } catch (Exception e) {
            log.error("Manual sync failed: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Sync failed: " + e.getMessage());
        }

        return "redirect:/calendar";
    }
}
