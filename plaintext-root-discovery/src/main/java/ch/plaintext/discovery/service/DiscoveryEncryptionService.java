package ch.plaintext.discovery.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import java.security.KeyFactory;
import java.util.Base64;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;

/**
 * PKI encryption service for secure discovery communications
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DiscoveryEncryptionService {
    
    private KeyPair keyPair;
    
    @PostConstruct
    
    // Generate RSA key pair on startup
    public void initializeKeys() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            keyPair = generator.generateKeyPair();
            log.info("Generated RSA key pair for discovery service");
        } catch (Exception e) {
            log.error("Failed to generate RSA key pair", e);
            throw new RuntimeException("Cannot initialize encryption service", e);
        }
    }
    
    /**
     * Get public key as Base64 string for sharing
     */
    public String getPublicKeyString() {
        if (keyPair == null) {
            initializeKeys();
        }
        return Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
    }
    
    /**
     * Encrypt text with recipient's public key
     */
    public String encrypt(String plaintext, String publicKeyString) {
        try {
            PublicKey publicKey = parsePublicKey(publicKeyString);
            
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            
            byte[] encryptedBytes = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedBytes);
            
        } catch (Exception e) {
            log.error("Error encrypting data", e);
            throw new RuntimeException("Encryption failed", e);
        }
    }
    
    /**
     * Decrypt text with our private key
     */
    public String decrypt(String encryptedText) {
        try {
            if (keyPair == null) {
                throw new IllegalStateException("Keys not initialized");
            }
            
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());
            
            byte[] encryptedBytes = Base64.getDecoder().decode(encryptedText);
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            
            return new String(decryptedBytes, StandardCharsets.UTF_8);
            
        } catch (Exception e) {
            log.error("Error decrypting data", e);
            throw new RuntimeException("Decryption failed", e);
        }
    }
    
    private static final String OAEP_ALGORITHM = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";
    private static final OAEPParameterSpec OAEP_PARAMS = new OAEPParameterSpec(
        "SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT);
    private static final long TOKEN_VALIDITY_MS = 5 * 60 * 1000; // 5 minutes
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Create an encrypted discovery token for cross-app auto-login.
     * Payload: email|timestampMillis|sourceAppId|nonce
     * Encrypted with target app's RSA public key using OAEP/SHA-256.
     */
    public String createDiscoveryToken(String email, String sourceAppId, String targetPublicKey) {
        try {
            String nonce = generateNonce();
            String payload = email + "|" + System.currentTimeMillis() + "|" + sourceAppId + "|" + nonce;

            PublicKey publicKey = parsePublicKey(targetPublicKey);
            Cipher cipher = Cipher.getInstance(OAEP_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, publicKey, OAEP_PARAMS);

            byte[] encrypted = cipher.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(encrypted);

        } catch (Exception e) {
            log.error("Error creating discovery token", e);
            throw new RuntimeException("Discovery token creation failed", e);
        }
    }

    /**
     * Decrypt and parse a discovery token received from another app.
     * Returns null if decryption fails (e.g. token not meant for this app).
     */
    public DiscoveryToken decryptDiscoveryToken(String encryptedToken) {
        try {
            if (keyPair == null) {
                throw new IllegalStateException("Keys not initialized");
            }

            Cipher cipher = Cipher.getInstance(OAEP_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate(), OAEP_PARAMS);

            byte[] decoded = Base64.getUrlDecoder().decode(encryptedToken);
            byte[] decrypted = cipher.doFinal(decoded);
            String payload = new String(decrypted, StandardCharsets.UTF_8);

            String[] parts = payload.split("\\|", 4);
            if (parts.length < 3) {
                log.warn("Invalid discovery token payload format");
                return null;
            }

            return new DiscoveryToken(
                parts[0],                       // email
                Long.parseLong(parts[1]),        // timestamp
                parts[2],                        // sourceAppId
                parts.length > 3 ? parts[3] : "" // nonce
            );

        } catch (Exception e) {
            log.debug("Cannot decrypt discovery token (not for this app or invalid): {}", e.getMessage());
            return null;
        }
    }

    /**
     * Check if a discovery token's timestamp is still valid (within 5 minutes).
     */
    public boolean isTokenValid(DiscoveryToken token) {
        return (System.currentTimeMillis() - token.timestamp()) < TOKEN_VALIDITY_MS;
    }

    private String generateNonce() {
        byte[] bytes = new byte[6];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public record DiscoveryToken(String email, long timestamp, String sourceAppId, String nonce) {}

    private PublicKey parsePublicKey(String publicKeyString) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(publicKeyString);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(spec);
    }
}