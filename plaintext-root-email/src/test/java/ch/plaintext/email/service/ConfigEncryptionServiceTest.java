/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.email.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConfigEncryptionServiceTest {

    private ConfigEncryptionService service;

    @BeforeEach
    void setUp() {
        service = new ConfigEncryptionService();
    }

    @Test
    void encryptProducesEncryptedFormat() {
        String encrypted = service.encrypt("hello", "password");

        assertTrue(encrypted.startsWith("ENC["));
        assertTrue(encrypted.endsWith("]"));
    }

    @Test
    void decryptRecoversOriginalText() {
        String password = "mySecretPassword123!";
        String plainText = "This is a secret message";

        String encrypted = service.encrypt(plainText, password);
        String decrypted = service.decrypt(encrypted, password);

        assertEquals(plainText, decrypted);
    }

    @Test
    void decryptWithWrongPasswordThrows() {
        String encrypted = service.encrypt("secret", "correctPassword");

        assertThrows(RuntimeException.class,
                () -> service.decrypt(encrypted, "wrongPassword"));
    }

    @Test
    void decryptNonEncryptedTextThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> service.decrypt("plain text", "password"));
    }

    @Test
    void encryptDifferentInputsProduceDifferentOutputs() {
        String password = "password";
        String encrypted1 = service.encrypt("text1", password);
        String encrypted2 = service.encrypt("text2", password);

        assertNotEquals(encrypted1, encrypted2);
    }

    @Test
    void encryptSameInputProducesDifferentOutputsDueToRandomSalt() {
        String password = "password";
        String encrypted1 = service.encrypt("same-text", password);
        String encrypted2 = service.encrypt("same-text", password);

        // Due to random salt and IV, same input should produce different outputs
        assertNotEquals(encrypted1, encrypted2);

        // But both should decrypt to the same text
        assertEquals("same-text", service.decrypt(encrypted1, password));
        assertEquals("same-text", service.decrypt(encrypted2, password));
    }

    @Test
    void isEncryptedReturnsTrueForEncryptedText() {
        String encrypted = service.encrypt("test", "password");

        assertTrue(service.isEncrypted(encrypted));
    }

    @Test
    void isEncryptedReturnsFalseForPlainText() {
        assertFalse(service.isEncrypted("just plain text"));
    }

    @Test
    void isEncryptedReturnsFalseForNull() {
        assertFalse(service.isEncrypted(null));
    }

    @Test
    void isEncryptedReturnsFalseForBlank() {
        assertFalse(service.isEncrypted(""));
        assertFalse(service.isEncrypted("   "));
    }

    @Test
    void isEncryptedReturnsFalseForPartialFormat() {
        assertFalse(service.isEncrypted("ENC["));
        assertFalse(service.isEncrypted("]"));
        assertFalse(service.isEncrypted("ENC[incomplete"));
    }

    @Test
    void encryptAndDecryptEmptyString() {
        String password = "password";
        String encrypted = service.encrypt("", password);
        String decrypted = service.decrypt(encrypted, password);

        assertEquals("", decrypted);
    }

    @Test
    void encryptAndDecryptUnicodeText() {
        String password = "passwort";
        String unicodeText = "Hallo Welt! Umlaute: ae oe ue ss - Sonderzeichen: @#$%";

        String encrypted = service.encrypt(unicodeText, password);
        String decrypted = service.decrypt(encrypted, password);

        assertEquals(unicodeText, decrypted);
    }

    @Test
    void encryptAndDecryptLongText() {
        String password = "password";
        String longText = "A".repeat(10000);

        String encrypted = service.encrypt(longText, password);
        String decrypted = service.decrypt(encrypted, password);

        assertEquals(longText, decrypted);
    }
}
