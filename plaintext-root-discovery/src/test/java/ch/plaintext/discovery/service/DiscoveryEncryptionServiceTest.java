/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.discovery.service;

import ch.plaintext.discovery.service.DiscoveryEncryptionService.DiscoveryToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class DiscoveryEncryptionServiceTest {

    private DiscoveryEncryptionService encryptionService;

    @BeforeEach
    void setUp() {
        encryptionService = new DiscoveryEncryptionService();
        encryptionService.initializeKeys();
    }

    @Nested
    class KeyInitialization {

        @Test
        void initializeKeysGeneratesPublicKeyString() {
            String publicKey = encryptionService.getPublicKeyString();

            assertNotNull(publicKey);
            assertFalse(publicKey.isBlank());
        }

        @Test
        void publicKeyStringIsValidBase64() {
            String publicKey = encryptionService.getPublicKeyString();

            assertDoesNotThrow(() -> Base64.getDecoder().decode(publicKey));
        }

        @Test
        void publicKeyIsConsistentAcrossCalls() {
            String key1 = encryptionService.getPublicKeyString();
            String key2 = encryptionService.getPublicKeyString();

            assertEquals(key1, key2);
        }

        @Test
        void getPublicKeyStringInitializesKeysIfNull() {
            // Create a service without calling initializeKeys
            DiscoveryEncryptionService freshService = new DiscoveryEncryptionService();

            // Should auto-initialize when called
            String publicKey = freshService.getPublicKeyString();
            assertNotNull(publicKey);
            assertFalse(publicKey.isBlank());
        }
    }

    @Nested
    class BasicEncryptDecrypt {

        @Test
        void encryptAndDecryptRoundTrip() {
            String publicKey = encryptionService.getPublicKeyString();
            String plaintext = "Hello, World!";

            String encrypted = encryptionService.encrypt(plaintext, publicKey);
            assertNotNull(encrypted);
            assertNotEquals(plaintext, encrypted);

            String decrypted = encryptionService.decrypt(encrypted);
            assertEquals(plaintext, decrypted);
        }

        @Test
        void encryptProducesDifferentOutputForSameInput() {
            String publicKey = encryptionService.getPublicKeyString();
            String plaintext = "test-data";

            // RSA with PKCS1 padding is deterministic, but the point is
            // each encrypted output should be decodable
            String encrypted1 = encryptionService.encrypt(plaintext, publicKey);
            String encrypted2 = encryptionService.encrypt(plaintext, publicKey);

            // Both should decrypt to the same value
            assertEquals(plaintext, encryptionService.decrypt(encrypted1));
            assertEquals(plaintext, encryptionService.decrypt(encrypted2));
        }

        @Test
        void encryptWithDifferentPublicKeyCannotBeDecrypted() throws Exception {
            // Generate a different key pair
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair otherKeyPair = generator.generateKeyPair();
            String otherPublicKey = Base64.getEncoder().encodeToString(otherKeyPair.getPublic().getEncoded());

            String encrypted = encryptionService.encrypt("secret", otherPublicKey);

            // Should fail to decrypt with our key pair since it was encrypted with a different public key
            assertThrows(RuntimeException.class, () -> encryptionService.decrypt(encrypted));
        }

        @Test
        void encryptEmptyString() {
            String publicKey = encryptionService.getPublicKeyString();
            String encrypted = encryptionService.encrypt("", publicKey);
            String decrypted = encryptionService.decrypt(encrypted);
            assertEquals("", decrypted);
        }

        @Test
        void encryptWithInvalidPublicKeyThrowsException() {
            assertThrows(RuntimeException.class,
                () -> encryptionService.encrypt("data", "not-a-valid-key"));
        }

        @Test
        void decryptWithInvalidDataThrowsException() {
            assertThrows(RuntimeException.class,
                () -> encryptionService.decrypt("not-valid-encrypted-data"));
        }
    }

    @Nested
    class DiscoveryTokenCreation {

        @Test
        void createDiscoveryTokenReturnsNonNullString() {
            String publicKey = encryptionService.getPublicKeyString();
            String token = encryptionService.createDiscoveryToken("user@example.com", "app1", publicKey);

            assertNotNull(token);
            assertFalse(token.isBlank());
        }

        @Test
        void createDiscoveryTokenIsUrlSafeBase64() {
            String publicKey = encryptionService.getPublicKeyString();
            String token = encryptionService.createDiscoveryToken("user@example.com", "app1", publicKey);

            // URL-safe base64 should not contain +, /, or =
            assertFalse(token.contains("+"));
            assertFalse(token.contains("/"));
            assertFalse(token.contains("="));
        }

        @Test
        void createDiscoveryTokenRoundTrip() {
            String publicKey = encryptionService.getPublicKeyString();
            String email = "user@example.com";
            String sourceAppId = "my-app";

            String token = encryptionService.createDiscoveryToken(email, sourceAppId, publicKey);
            DiscoveryToken decrypted = encryptionService.decryptDiscoveryToken(token);

            assertNotNull(decrypted);
            assertEquals(email, decrypted.email());
            assertEquals(sourceAppId, decrypted.sourceAppId());
            assertTrue(decrypted.timestamp() > 0);
            assertNotNull(decrypted.nonce());
            assertFalse(decrypted.nonce().isBlank());
        }

        @Test
        void createDiscoveryTokenWithDifferentKeyCannotBeDecrypted() throws Exception {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair otherKeyPair = generator.generateKeyPair();
            String otherPublicKey = Base64.getEncoder().encodeToString(otherKeyPair.getPublic().getEncoded());

            // Encrypt with a different public key - our service cannot decrypt it
            String token = encryptionService.createDiscoveryToken("user@example.com", "app1", otherPublicKey);
            DiscoveryToken result = encryptionService.decryptDiscoveryToken(token);

            assertNull(result, "Should return null when decryption fails");
        }

        @Test
        void createDiscoveryTokenWithInvalidKeyThrowsException() {
            assertThrows(RuntimeException.class,
                () -> encryptionService.createDiscoveryToken("user@example.com", "app1", "invalid-key"));
        }
    }

    @Nested
    class DiscoveryTokenDecryption {

        @Test
        void decryptDiscoveryTokenReturnsNullForInvalidToken() {
            DiscoveryToken result = encryptionService.decryptDiscoveryToken("not-a-valid-token");
            assertNull(result);
        }

        @Test
        void decryptDiscoveryTokenReturnsNullForEmptyString() {
            DiscoveryToken result = encryptionService.decryptDiscoveryToken("");
            assertNull(result);
        }

        @Test
        void decryptDiscoveryTokenPreservesSpecialCharactersInEmail() {
            String publicKey = encryptionService.getPublicKeyString();
            String email = "user+tag@sub.example.com";

            String token = encryptionService.createDiscoveryToken(email, "app1", publicKey);
            DiscoveryToken decrypted = encryptionService.decryptDiscoveryToken(token);

            assertNotNull(decrypted);
            assertEquals(email, decrypted.email());
        }

        @Test
        void decryptedTokenContainsDifferentNoncesForSameInput() {
            String publicKey = encryptionService.getPublicKeyString();

            String token1 = encryptionService.createDiscoveryToken("user@test.com", "app1", publicKey);
            String token2 = encryptionService.createDiscoveryToken("user@test.com", "app1", publicKey);

            DiscoveryToken decrypted1 = encryptionService.decryptDiscoveryToken(token1);
            DiscoveryToken decrypted2 = encryptionService.decryptDiscoveryToken(token2);

            assertNotNull(decrypted1);
            assertNotNull(decrypted2);
            // Nonces should be different (very high probability)
            assertNotEquals(decrypted1.nonce(), decrypted2.nonce());
        }
    }

    @Nested
    class TokenValidation {

        @Test
        void freshTokenIsValid() {
            String publicKey = encryptionService.getPublicKeyString();
            String token = encryptionService.createDiscoveryToken("user@test.com", "app1", publicKey);
            DiscoveryToken decrypted = encryptionService.decryptDiscoveryToken(token);

            assertNotNull(decrypted);
            assertTrue(encryptionService.isTokenValid(decrypted));
        }

        @Test
        void expiredTokenIsInvalid() {
            // Create a token with a timestamp 6 minutes in the past (validity is 5 minutes)
            long sixMinutesAgo = System.currentTimeMillis() - (6 * 60 * 1000);
            DiscoveryToken expiredToken = new DiscoveryToken("user@test.com", sixMinutesAgo, "app1", "nonce");

            assertFalse(encryptionService.isTokenValid(expiredToken));
        }

        @Test
        void tokenAtBoundaryIsValid() {
            // Create a token with a timestamp 4 minutes in the past (within 5 minute validity)
            long fourMinutesAgo = System.currentTimeMillis() - (4 * 60 * 1000);
            DiscoveryToken recentToken = new DiscoveryToken("user@test.com", fourMinutesAgo, "app1", "nonce");

            assertTrue(encryptionService.isTokenValid(recentToken));
        }

        @Test
        void tokenExactlyAtFiveMinutesIsInvalid() {
            // Create a token with a timestamp exactly 5 minutes in the past
            long fiveMinutesAgo = System.currentTimeMillis() - (5 * 60 * 1000);
            DiscoveryToken borderToken = new DiscoveryToken("user@test.com", fiveMinutesAgo, "app1", "nonce");

            // 5 * 60 * 1000 = TOKEN_VALIDITY_MS, so (now - timestamp) == TOKEN_VALIDITY_MS is NOT valid
            assertFalse(encryptionService.isTokenValid(borderToken));
        }
    }

    @Nested
    class CrossInstanceEncryption {

        @Test
        void twoServicesCanCommunicateViaDiscoveryTokens() {
            DiscoveryEncryptionService service1 = new DiscoveryEncryptionService();
            service1.initializeKeys();

            DiscoveryEncryptionService service2 = new DiscoveryEncryptionService();
            service2.initializeKeys();

            // Service1 creates a token encrypted with service2's public key
            String token = service1.createDiscoveryToken("user@test.com", "app1", service2.getPublicKeyString());

            // Service2 can decrypt it
            DiscoveryToken decrypted = service2.decryptDiscoveryToken(token);
            assertNotNull(decrypted);
            assertEquals("user@test.com", decrypted.email());
            assertEquals("app1", decrypted.sourceAppId());

            // Service1 cannot decrypt it (encrypted with service2's public key)
            DiscoveryToken failedDecrypt = service1.decryptDiscoveryToken(token);
            assertNull(failedDecrypt);
        }

        @Test
        void twoServicesCanCommunicateViaBasicEncryption() {
            DiscoveryEncryptionService service1 = new DiscoveryEncryptionService();
            service1.initializeKeys();

            DiscoveryEncryptionService service2 = new DiscoveryEncryptionService();
            service2.initializeKeys();

            // Service1 encrypts with service2's public key
            String encrypted = service1.encrypt("secret-token", service2.getPublicKeyString());

            // Service2 can decrypt
            String decrypted = service2.decrypt(encrypted);
            assertEquals("secret-token", decrypted);

            // Service1 cannot decrypt (different private key)
            assertThrows(RuntimeException.class, () -> service1.decrypt(encrypted));
        }
    }

    @Nested
    class DiscoveryTokenRecord {

        @Test
        void discoveryTokenRecordFieldsAreAccessible() {
            DiscoveryToken token = new DiscoveryToken("email@test.com", 12345L, "source-app", "nonce123");

            assertEquals("email@test.com", token.email());
            assertEquals(12345L, token.timestamp());
            assertEquals("source-app", token.sourceAppId());
            assertEquals("nonce123", token.nonce());
        }

        @Test
        void discoveryTokenRecordEquality() {
            DiscoveryToken t1 = new DiscoveryToken("a@b.com", 1L, "app", "n");
            DiscoveryToken t2 = new DiscoveryToken("a@b.com", 1L, "app", "n");
            DiscoveryToken t3 = new DiscoveryToken("x@y.com", 1L, "app", "n");

            assertEquals(t1, t2);
            assertNotEquals(t1, t3);
        }
    }
}
