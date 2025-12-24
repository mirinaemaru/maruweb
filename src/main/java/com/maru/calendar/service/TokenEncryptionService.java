package com.maru.calendar.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service
public class TokenEncryptionService {

    @Value("${calendar.oauth.encryption-key}")
    private String encryptionKey;

    private TextEncryptor encryptor;

    @PostConstruct
    public void init() {
        // Use a hex salt for additional security (32 hex characters = 16 bytes)
        String salt = "deadbeefdeadbeefdeadbeefdeadbeef";
        this.encryptor = Encryptors.text(encryptionKey, salt);
    }

    public String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }
        return encryptor.encrypt(plainText);
    }

    public String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isEmpty()) {
            return encryptedText;
        }
        return encryptor.decrypt(encryptedText);
    }
}
