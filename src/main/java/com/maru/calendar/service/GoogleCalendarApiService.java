package com.maru.calendar.service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.Events;
import com.maru.calendar.entity.CalendarEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class GoogleCalendarApiService {

    private static final String APPLICATION_NAME = "Maru Todo Calendar";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String CALENDAR_ID = "primary";

    private final GoogleOAuthService oauthService;

    public GoogleCalendarApiService(GoogleOAuthService oauthService) {
        this.oauthService = oauthService;
    }

    private Calendar getCalendarService() throws GeneralSecurityException, IOException {
        Optional<Credential> credentialOpt = oauthService.getCredentials();
        if (!credentialOpt.isPresent()) {
            throw new IOException("Not authenticated with Google Calendar");
        }

        NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        return new Calendar.Builder(httpTransport, JSON_FACTORY, credentialOpt.get())
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    public String createEvent(CalendarEvent calendarEvent) throws GeneralSecurityException, IOException {
        Calendar service = getCalendarService();
        Event googleEvent = convertToGoogleEvent(calendarEvent);

        Event createdEvent = service.events().insert(CALENDAR_ID, googleEvent).execute();
        log.info("Google Calendar event created: {}", createdEvent.getId());

        return createdEvent.getId();
    }

    public void updateEvent(CalendarEvent calendarEvent) throws GeneralSecurityException, IOException {
        if (calendarEvent.getGoogleEventId() == null) {
            throw new IllegalArgumentException("Event has no Google Event ID");
        }

        Calendar service = getCalendarService();
        Event googleEvent = convertToGoogleEvent(calendarEvent);

        service.events().update(CALENDAR_ID, calendarEvent.getGoogleEventId(), googleEvent).execute();
        log.info("Google Calendar event updated: {}", calendarEvent.getGoogleEventId());
    }

    public void deleteEvent(String googleEventId) throws GeneralSecurityException, IOException {
        Calendar service = getCalendarService();
        service.events().delete(CALENDAR_ID, googleEventId).execute();
        log.info("Google Calendar event deleted: {}", googleEventId);
    }

    public List<CalendarEvent> fetchRecentEvents(LocalDateTime since) throws GeneralSecurityException, IOException {
        Calendar service = getCalendarService();

        // Convert LocalDateTime to RFC3339 format
        ZonedDateTime zonedDateTime = since.atZone(ZoneId.systemDefault());
        DateTime timeMin = new DateTime(zonedDateTime.toInstant().toEpochMilli());

        Events events = service.events().list(CALENDAR_ID)
                .setTimeMin(timeMin)
                .setOrderBy("startTime")
                .setSingleEvents(true)
                .setMaxResults(100)
                .execute();

        List<CalendarEvent> calendarEvents = new ArrayList<>();
        for (Event event : events.getItems()) {
            CalendarEvent calendarEvent = convertFromGoogleEvent(event);
            if (calendarEvent != null) {
                calendarEvents.add(calendarEvent);
            }
        }

        log.info("Fetched {} events from Google Calendar", calendarEvents.size());
        return calendarEvents;
    }

    private Event convertToGoogleEvent(CalendarEvent calendarEvent) {
        Event event = new Event()
                .setSummary(calendarEvent.getTitle())
                .setDescription(calendarEvent.getDescription())
                .setLocation(calendarEvent.getLocation());

        // Set start and end times
        EventDateTime start = convertToEventDateTime(calendarEvent.getStartDateTime(), "Y".equals(calendarEvent.getAllDay()));
        EventDateTime end = convertToEventDateTime(calendarEvent.getEndDateTime(), "Y".equals(calendarEvent.getAllDay()));

        event.setStart(start);
        event.setEnd(end);

        return event;
    }

    private CalendarEvent convertFromGoogleEvent(Event googleEvent) {
        if (googleEvent.getStart() == null || googleEvent.getEnd() == null) {
            log.warn("Skipping event without start/end time: {}", googleEvent.getId());
            return null;
        }

        CalendarEvent calendarEvent = new CalendarEvent();
        calendarEvent.setTitle(googleEvent.getSummary() != null ? googleEvent.getSummary() : "No Title");
        calendarEvent.setDescription(googleEvent.getDescription());
        calendarEvent.setLocation(googleEvent.getLocation());

        // Parse start and end times
        LocalDateTime startDateTime = parseEventDateTime(googleEvent.getStart());
        LocalDateTime endDateTime = parseEventDateTime(googleEvent.getEnd());
        boolean isAllDay = googleEvent.getStart().getDate() != null;

        calendarEvent.setStartDateTime(startDateTime);
        calendarEvent.setEndDateTime(endDateTime);
        calendarEvent.setAllDay(isAllDay ? "Y" : "N");

        // Set Google metadata
        calendarEvent.setGoogleEventId(googleEvent.getId());
        calendarEvent.setGoogleCalendarId(CALENDAR_ID);
        calendarEvent.setSyncStatus("SYNCED");
        calendarEvent.setLastSyncedAt(LocalDateTime.now());
        calendarEvent.setDeleted("N");

        return calendarEvent;
    }

    private EventDateTime convertToEventDateTime(LocalDateTime dateTime, boolean isAllDay) {
        EventDateTime eventDateTime = new EventDateTime();

        if (isAllDay) {
            // For all-day events, use date only (no time component)
            String dateStr = dateTime.toLocalDate().toString();
            eventDateTime.setDate(new DateTime(dateStr));
        } else {
            // For timed events, use full datetime with timezone
            ZonedDateTime zonedDateTime = dateTime.atZone(ZoneId.systemDefault());
            eventDateTime.setDateTime(new DateTime(zonedDateTime.toInstant().toEpochMilli()));
            eventDateTime.setTimeZone(ZoneId.systemDefault().getId());
        }

        return eventDateTime;
    }

    private LocalDateTime parseEventDateTime(EventDateTime eventDateTime) {
        if (eventDateTime.getDate() != null) {
            // All-day event - use date only, set time to start of day
            return LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(eventDateTime.getDate().getValue()),
                    ZoneId.systemDefault()
            ).toLocalDate().atStartOfDay();
        } else if (eventDateTime.getDateTime() != null) {
            // Timed event
            return LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(eventDateTime.getDateTime().getValue()),
                    ZoneId.systemDefault()
            );
        }
        return LocalDateTime.now();
    }
}
