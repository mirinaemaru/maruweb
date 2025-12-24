package com.maru.calendar.service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.maru.calendar.entity.GoogleOAuthToken;
import com.maru.calendar.repository.GoogleOAuthTokenRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class GoogleOAuthService {

    private static final String USER_IDENTIFIER = "default";
    private static final List<String> SCOPES = Arrays.asList(
            "https://www.googleapis.com/auth/calendar",
            "https://www.googleapis.com/auth/calendar.events"
    );
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    @Value("${google.oauth2.client-id}")
    private String clientId;

    @Value("${google.oauth2.client-secret}")
    private String clientSecret;

    @Value("${google.oauth2.redirect-uri}")
    private String redirectUri;

    private final GoogleOAuthTokenRepository tokenRepository;
    private final TokenEncryptionService encryptionService;

    public GoogleOAuthService(GoogleOAuthTokenRepository tokenRepository,
                             TokenEncryptionService encryptionService) {
        this.tokenRepository = tokenRepository;
        this.encryptionService = encryptionService;
    }

    public String getAuthorizationUrl() throws GeneralSecurityException, IOException {
        NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();

        log.info("=== OAuth Configuration ===");
        log.info("Client ID: {}", clientId);
        log.info("Redirect URI: {}", redirectUri);
        log.info("===========================");

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                httpTransport, JSON_FACTORY, clientId, clientSecret, SCOPES)
                .setAccessType("offline")
                .setApprovalPrompt("force")
                .build();

        String authUrl = flow.newAuthorizationUrl()
                .setRedirectUri(redirectUri)
                .setState("state-token")
                .build();

        log.info("Generated Authorization URL: {}", authUrl);
        return authUrl;
    }

    @Transactional
    public void exchangeCodeForTokens(String code) throws IOException, GeneralSecurityException {
        NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();

        GoogleTokenResponse tokenResponse = new GoogleAuthorizationCodeTokenRequest(
                httpTransport, JSON_FACTORY,
                clientId, clientSecret, code, redirectUri)
                .execute();

        String accessToken = tokenResponse.getAccessToken();
        String refreshToken = tokenResponse.getRefreshToken();
        Long expiresInSeconds = tokenResponse.getExpiresInSeconds();

        // Encrypt tokens before storing
        String encryptedAccessToken = encryptionService.encrypt(accessToken);
        String encryptedRefreshToken = refreshToken != null ? encryptionService.encrypt(refreshToken) : null;

        // Calculate expiration time
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(expiresInSeconds);

        // Save or update token
        Optional<GoogleOAuthToken> existingToken = tokenRepository.findByUserIdentifier(USER_IDENTIFIER);
        GoogleOAuthToken token;

        if (existingToken.isPresent()) {
            token = existingToken.get();
            token.setAccessToken(encryptedAccessToken);
            if (encryptedRefreshToken != null) {
                token.setRefreshToken(encryptedRefreshToken);
            }
            token.setExpiresAt(expiresAt);
        } else {
            token = new GoogleOAuthToken();
            token.setUserIdentifier(USER_IDENTIFIER);
            token.setAccessToken(encryptedAccessToken);
            token.setRefreshToken(encryptedRefreshToken);
            token.setExpiresAt(expiresAt);
            token.setScope(String.join(" ", SCOPES));
        }

        tokenRepository.save(token);
        log.info("OAuth tokens saved successfully for user: {}", USER_IDENTIFIER);
    }

    public boolean isAuthenticated() {
        return tokenRepository.findByUserIdentifier(USER_IDENTIFIER).isPresent();
    }

    public Optional<Credential> getCredentials() throws GeneralSecurityException, IOException {
        Optional<GoogleOAuthToken> tokenOpt = tokenRepository.findByUserIdentifier(USER_IDENTIFIER);

        if (!tokenOpt.isPresent()) {
            return Optional.empty();
        }

        GoogleOAuthToken token = tokenOpt.get();

        // Check if token is expired
        if (LocalDateTime.now().isAfter(token.getExpiresAt())) {
            // Refresh token
            refreshAccessToken(token);
        }

        // Decrypt tokens
        String accessToken = encryptionService.decrypt(token.getAccessToken());
        String refreshToken = token.getRefreshToken() != null ?
                encryptionService.decrypt(token.getRefreshToken()) : null;

        // Create credential
        NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                httpTransport, JSON_FACTORY, clientId, clientSecret, SCOPES)
                .build();

        Credential credential = flow.createAndStoreCredential(
                new GoogleTokenResponse()
                        .setAccessToken(accessToken)
                        .setRefreshToken(refreshToken)
                        .setExpiresInSeconds(3600L),
                USER_IDENTIFIER
        );

        return Optional.of(credential);
    }

    @Transactional
    private void refreshAccessToken(GoogleOAuthToken token) throws IOException, GeneralSecurityException {
        if (token.getRefreshToken() == null) {
            throw new IOException("No refresh token available");
        }

        String refreshToken = encryptionService.decrypt(token.getRefreshToken());
        NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();

        GoogleTokenResponse tokenResponse = new com.google.api.client.googleapis.auth.oauth2.GoogleRefreshTokenRequest(
                httpTransport, JSON_FACTORY, refreshToken, clientId, clientSecret)
                .execute();

        String newAccessToken = tokenResponse.getAccessToken();
        Long expiresInSeconds = tokenResponse.getExpiresInSeconds();

        // Encrypt and save new access token
        token.setAccessToken(encryptionService.encrypt(newAccessToken));
        token.setExpiresAt(LocalDateTime.now().plusSeconds(expiresInSeconds));
        tokenRepository.save(token);

        log.info("Access token refreshed for user: {}", USER_IDENTIFIER);
    }

    @Transactional
    public void disconnect() {
        tokenRepository.deleteByUserIdentifier(USER_IDENTIFIER);
        log.info("User {} disconnected from Google Calendar", USER_IDENTIFIER);
    }
}
