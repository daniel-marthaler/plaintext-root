/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.email.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;

/**
 * Service für Ver- und Entschlüsselung von Email-Konfigurationen
 */
@Service
@Slf4j
public class ConfigEncryptionService {

    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final String KEY_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int KEY_LENGTH = 256;
    private static final int ITERATION_COUNT = 65536;
    private static final String ENCRYPTED_PREFIX = "ENC[";
    private static final String ENCRYPTED_SUFFIX = "]";

    /**
     * Verschlüsselt einen Text mit einem Passwort
     *
     * @param plainText Klartext
     * @param password Passwort für die Verschlüsselung
     * @return Verschlüsselter Text im Format "ENC[base64]"
     */
    public String encrypt(String plainText, String password) {
        try {
            // Generate random salt
            byte[] salt = new byte[16];
            SecureRandom random = new SecureRandom();
            random.nextBytes(salt);

            // Generate random IV
            byte[] iv = new byte[16];
            random.nextBytes(iv);

            // Derive key from password
            SecretKey key = deriveKey(password, salt);

            // Encrypt
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // Combine salt + IV + encrypted data
            byte[] combined = new byte[salt.length + iv.length + encrypted.length];
            System.arraycopy(salt, 0, combined, 0, salt.length);
            System.arraycopy(iv, 0, combined, salt.length, iv.length);
            System.arraycopy(encrypted, 0, combined, salt.length + iv.length, encrypted.length);

            // Encode to Base64 with prefix
            return ENCRYPTED_PREFIX + Base64.getEncoder().encodeToString(combined) + ENCRYPTED_SUFFIX;

        } catch (Exception e) {
            log.error("Encryption failed", e);
            throw new RuntimeException("Verschlüsselung fehlgeschlagen: " + e.getMessage(), e);
        }
    }

    /**
     * Entschlüsselt einen verschlüsselten Text
     *
     * @param encryptedText Verschlüsselter Text im Format "ENC[base64]"
     * @param password Passwort für die Entschlüsselung
     * @return Entschlüsselter Klartext
     */
    public String decrypt(String encryptedText, String password) {
        try {
            // Check if text is encrypted
            if (!isEncrypted(encryptedText)) {
                throw new IllegalArgumentException("Text ist nicht verschlüsselt (fehlendes ENC[ ... ] Format)");
            }

            // Remove prefix and suffix
            String base64 = encryptedText.substring(
                    ENCRYPTED_PREFIX.length(),
                    encryptedText.length() - ENCRYPTED_SUFFIX.length()
            );

            // Decode Base64
            byte[] combined = Base64.getDecoder().decode(base64);

            // Extract salt, IV, and encrypted data
            byte[] salt = new byte[16];
            byte[] iv = new byte[16];
            byte[] encrypted = new byte[combined.length - 32];

            System.arraycopy(combined, 0, salt, 0, 16);
            System.arraycopy(combined, 16, iv, 0, 16);
            System.arraycopy(combined, 32, encrypted, 0, encrypted.length);

            // Derive key from password
            SecretKey key = deriveKey(password, salt);

            // Decrypt
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
            byte[] decrypted = cipher.doFinal(encrypted);

            return new String(decrypted, StandardCharsets.UTF_8);

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Decryption failed", e);
            throw new RuntimeException("Entschlüsselung fehlgeschlagen. Falsches Passwort?", e);
        }
    }

    /**
     * Prüft ob ein Text verschlüsselt ist
     *
     * @param text Zu prüfender Text
     * @return true wenn verschlüsselt, false sonst
     */
    public boolean isEncrypted(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        return text.startsWith(ENCRYPTED_PREFIX) && text.endsWith(ENCRYPTED_SUFFIX);
    }

    /**
     * Ableitung eines Schlüssels aus einem Passwort mittels PBKDF2
     */
    private SecretKey deriveKey(String password, byte[] salt) throws Exception {
        SecretKeyFactory factory = SecretKeyFactory.getInstance(KEY_ALGORITHM);
        KeySpec spec = new PBEKeySpec(
                password.toCharArray(),
                salt,
                ITERATION_COUNT,
                KEY_LENGTH
        );
        SecretKey tmp = factory.generateSecret(spec);
        return new SecretKeySpec(tmp.getEncoded(), "AES");
    }
}
