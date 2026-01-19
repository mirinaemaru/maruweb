package com.maru.calendar.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TokenEncryptionService 테스트")
class TokenEncryptionServiceTest {

    private TokenEncryptionService encryptionService;

    @BeforeEach
    void setUp() {
        encryptionService = new TokenEncryptionService();
        // Set encryption key (must be valid for AES encryption)
        ReflectionTestUtils.setField(encryptionService, "encryptionKey", "testEncryptionKey123456");
        encryptionService.init();
    }

    @Nested
    @DisplayName("encrypt 메서드")
    class EncryptTest {

        @Test
        @DisplayName("일반 텍스트 암호화 - 성공")
        void encrypt_Success() {
            // given
            String plainText = "test-access-token-12345";

            // when
            String encrypted = encryptionService.encrypt(plainText);

            // then
            assertThat(encrypted).isNotNull();
            assertThat(encrypted).isNotEmpty();
            assertThat(encrypted).isNotEqualTo(plainText);
        }

        @Test
        @DisplayName("null 값 암호화 - null 반환")
        void encrypt_NullValue() {
            // given
            String plainText = null;

            // when
            String result = encryptionService.encrypt(plainText);

            // then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("빈 문자열 암호화 - 빈 문자열 반환")
        void encrypt_EmptyString() {
            // given
            String plainText = "";

            // when
            String result = encryptionService.encrypt(plainText);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("동일한 텍스트 암호화 시 매번 다른 결과")
        void encrypt_DifferentResultsForSameInput() {
            // given
            String plainText = "same-text-to-encrypt";

            // when
            String encrypted1 = encryptionService.encrypt(plainText);
            String encrypted2 = encryptionService.encrypt(plainText);

            // then - Spring Security의 TextEncryptor는 매번 다른 IV를 사용
            assertThat(encrypted1).isNotEqualTo(encrypted2);
        }

        @Test
        @DisplayName("긴 텍스트 암호화 - 성공")
        void encrypt_LongText() {
            // given
            String plainText = "A".repeat(1000);

            // when
            String encrypted = encryptionService.encrypt(plainText);

            // then
            assertThat(encrypted).isNotNull();
            assertThat(encrypted).isNotEmpty();
            assertThat(encrypted).isNotEqualTo(plainText);
        }

        @Test
        @DisplayName("특수문자 포함 텍스트 암호화")
        void encrypt_SpecialCharacters() {
            // given
            String plainText = "token!@#$%^&*()_+-=[]{}|;':\",./<>?";

            // when
            String encrypted = encryptionService.encrypt(plainText);

            // then
            assertThat(encrypted).isNotNull();
            assertThat(encrypted).isNotEmpty();
        }

        @Test
        @DisplayName("한글 텍스트 암호화")
        void encrypt_KoreanText() {
            // given
            String plainText = "테스트 액세스 토큰입니다";

            // when
            String encrypted = encryptionService.encrypt(plainText);

            // then
            assertThat(encrypted).isNotNull();
            assertThat(encrypted).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("decrypt 메서드")
    class DecryptTest {

        @Test
        @DisplayName("암호화된 텍스트 복호화 - 성공")
        void decrypt_Success() {
            // given
            String originalText = "test-access-token-12345";
            String encrypted = encryptionService.encrypt(originalText);

            // when
            String decrypted = encryptionService.decrypt(encrypted);

            // then
            assertThat(decrypted).isEqualTo(originalText);
        }

        @Test
        @DisplayName("null 값 복호화 - null 반환")
        void decrypt_NullValue() {
            // given
            String encryptedText = null;

            // when
            String result = encryptionService.decrypt(encryptedText);

            // then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("빈 문자열 복호화 - 빈 문자열 반환")
        void decrypt_EmptyString() {
            // given
            String encryptedText = "";

            // when
            String result = encryptionService.decrypt(encryptedText);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("잘못된 암호문 복호화 - 예외 발생")
        void decrypt_InvalidCiphertext() {
            // given
            String invalidCiphertext = "this-is-not-valid-ciphertext";

            // when & then
            assertThatThrownBy(() -> encryptionService.decrypt(invalidCiphertext))
                    .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("긴 텍스트 암호화 후 복호화")
        void decrypt_LongText() {
            // given
            String originalText = "B".repeat(500);
            String encrypted = encryptionService.encrypt(originalText);

            // when
            String decrypted = encryptionService.decrypt(encrypted);

            // then
            assertThat(decrypted).isEqualTo(originalText);
        }

        @Test
        @DisplayName("특수문자 포함 텍스트 암호화 후 복호화")
        void decrypt_SpecialCharacters() {
            // given
            String originalText = "token!@#$%^&*()_+-=[]{}|;':\",./<>?";
            String encrypted = encryptionService.encrypt(originalText);

            // when
            String decrypted = encryptionService.decrypt(encrypted);

            // then
            assertThat(decrypted).isEqualTo(originalText);
        }

        @Test
        @DisplayName("한글 텍스트 암호화 후 복호화")
        void decrypt_KoreanText() {
            // given
            String originalText = "테스트 액세스 토큰입니다";
            String encrypted = encryptionService.encrypt(originalText);

            // when
            String decrypted = encryptionService.decrypt(encrypted);

            // then
            assertThat(decrypted).isEqualTo(originalText);
        }
    }

    @Nested
    @DisplayName("암호화-복호화 라운드트립 테스트")
    class RoundTripTest {

        @Test
        @DisplayName("JSON 형식 토큰 암호화-복호화")
        void roundTrip_JsonToken() {
            // given
            String jsonToken = "{\"access_token\":\"ya29.xxx\",\"refresh_token\":\"1//xxx\",\"expires_in\":3600}";

            // when
            String encrypted = encryptionService.encrypt(jsonToken);
            String decrypted = encryptionService.decrypt(encrypted);

            // then
            assertThat(decrypted).isEqualTo(jsonToken);
        }

        @Test
        @DisplayName("Base64 인코딩된 토큰 암호화-복호화")
        void roundTrip_Base64Token() {
            // given
            String base64Token = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJhY2NvdW50cy5nb29nbGUuY29tIn0.signature";

            // when
            String encrypted = encryptionService.encrypt(base64Token);
            String decrypted = encryptionService.decrypt(encrypted);

            // then
            assertThat(decrypted).isEqualTo(base64Token);
        }

        @Test
        @DisplayName("여러 번 암호화-복호화 반복")
        void roundTrip_MultipleIterations() {
            // given
            String originalText = "test-token";

            // when
            String text = originalText;
            for (int i = 0; i < 5; i++) {
                String encrypted = encryptionService.encrypt(text);
                text = encryptionService.decrypt(encrypted);
            }

            // then
            assertThat(text).isEqualTo(originalText);
        }
    }
}
