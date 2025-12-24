package com.maru.calendar.service;

import com.maru.calendar.entity.CalendarEvent;
import com.maru.calendar.repository.CalendarEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class GoogleCalendarSyncService {

    @Value("${calendar.sync.enabled:false}")
    private boolean syncEnabled;

    private final CalendarEventRepository eventRepository;
    private final GoogleCalendarApiService googleApiService;
    private final GoogleOAuthService oauthService;

    public GoogleCalendarSyncService(CalendarEventRepository eventRepository,
                                    GoogleCalendarApiService googleApiService,
                                    GoogleOAuthService oauthService) {
        this.eventRepository = eventRepository;
        this.googleApiService = googleApiService;
        this.oauthService = oauthService;
    }

    @Transactional
    public Map<String, Integer> performBidirectionalSync() {
        Map<String, Integer> result = new HashMap<>();
        result.put("created", 0);
        result.put("updated", 0);
        result.put("deleted", 0);
        result.put("failed", 0);

        if (!syncEnabled) {
            log.info("Calendar sync is disabled");
            return result;
        }

        if (!oauthService.isAuthenticated()) {
            log.warn("Not authenticated with Google Calendar. Skipping sync.");
            return result;
        }

        try {
            // PUSH: Send local changes to Google
            Map<String, Integer> pushResult = pushLocalChangesToGoogle();
            result.put("push_created", pushResult.get("created"));
            result.put("push_updated", pushResult.get("updated"));
            result.put("push_deleted", pushResult.get("deleted"));
            result.put("push_failed", pushResult.get("failed"));

            // PULL: Fetch Google changes and update local
            Map<String, Integer> pullResult = pullGoogleChangesToLocal();
            result.put("pull_created", pullResult.get("created"));
            result.put("pull_updated", pullResult.get("updated"));

            log.info("Sync completed: {}", result);
        } catch (Exception e) {
            log.error("Sync failed: {}", e.getMessage(), e);
            result.put("failed", 1);
        }

        return result;
    }

    @Transactional
    public Map<String, Integer> pushLocalChangesToGoogle() {
        Map<String, Integer> result = new HashMap<>();
        result.put("created", 0);
        result.put("updated", 0);
        result.put("deleted", 0);
        result.put("failed", 0);

        // Find all events that need syncing
        List<CalendarEvent> pendingEvents = eventRepository.findBySyncStatusIn(
                Arrays.asList("PENDING", "FAILED", "LOCAL_ONLY"));

        log.info("Found {} events pending sync to Google", pendingEvents.size());

        for (CalendarEvent event : pendingEvents) {
            try {
                if ("Y".equals(event.getDeleted())) {
                    // Delete from Google
                    if (event.getGoogleEventId() != null) {
                        googleApiService.deleteEvent(event.getGoogleEventId());
                        result.put("deleted", result.get("deleted") + 1);
                        log.info("Deleted event from Google: {}", event.getTitle());
                    }
                    // Don't update the event as it's deleted locally
                } else if (event.getGoogleEventId() == null) {
                    // Create in Google
                    String googleEventId = googleApiService.createEvent(event);
                    event.setGoogleEventId(googleEventId);
                    event.setSyncStatus("SYNCED");
                    event.setLastSyncedAt(LocalDateTime.now());
                    eventRepository.save(event);
                    result.put("created", result.get("created") + 1);
                    log.info("Created event in Google: {}", event.getTitle());
                } else {
                    // Update in Google
                    googleApiService.updateEvent(event);
                    event.setSyncStatus("SYNCED");
                    event.setLastSyncedAt(LocalDateTime.now());
                    eventRepository.save(event);
                    result.put("updated", result.get("updated") + 1);
                    log.info("Updated event in Google: {}", event.getTitle());
                }
            } catch (Exception e) {
                log.error("Failed to sync event {}: {}", event.getTitle(), e.getMessage());
                event.setSyncStatus("FAILED");
                eventRepository.save(event);
                result.put("failed", result.get("failed") + 1);
            }
        }

        return result;
    }

    @Transactional
    public Map<String, Integer> pullGoogleChangesToLocal() {
        Map<String, Integer> result = new HashMap<>();
        result.put("created", 0);
        result.put("updated", 0);

        try {
            // Fetch events from the last 30 days
            LocalDateTime since = LocalDateTime.now().minusDays(30);
            List<CalendarEvent> googleEvents = googleApiService.fetchRecentEvents(since);

            log.info("Fetched {} events from Google Calendar", googleEvents.size());

            for (CalendarEvent googleEvent : googleEvents) {
                CalendarEvent existingEvent = eventRepository.findByGoogleEventId(googleEvent.getGoogleEventId());

                if (existingEvent == null) {
                    // New event from Google - create locally
                    eventRepository.save(googleEvent);
                    result.put("created", result.get("created") + 1);
                    log.info("Created local event from Google: {}", googleEvent.getTitle());
                } else {
                    // Event exists - check if Google version is newer
                    // For simplicity, we'll always update with Google's version (Google is source of truth)
                    existingEvent.setTitle(googleEvent.getTitle());
                    existingEvent.setDescription(googleEvent.getDescription());
                    existingEvent.setLocation(googleEvent.getLocation());
                    existingEvent.setStartDateTime(googleEvent.getStartDateTime());
                    existingEvent.setEndDateTime(googleEvent.getEndDateTime());
                    existingEvent.setAllDay(googleEvent.getAllDay());
                    existingEvent.setSyncStatus("SYNCED");
                    existingEvent.setLastSyncedAt(LocalDateTime.now());
                    eventRepository.save(existingEvent);
                    result.put("updated", result.get("updated") + 1);
                    log.info("Updated local event from Google: {}", googleEvent.getTitle());
                }
            }
        } catch (Exception e) {
            log.error("Failed to pull events from Google: {}", e.getMessage(), e);
        }

        return result;
    }

    public void markEventForSync(CalendarEvent event) {
        if (event.getSyncStatus() == null || !event.getSyncStatus().equals("FAILED")) {
            event.setSyncStatus("PENDING");
        }
    }
}
