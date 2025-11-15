package com.task.sheet.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class AesGcmCrypto {

    private static final String TRANSFORM = "AES/GCM/NoPadding";
    private static final int IV_BYTES = 12;     // 12 bytes nonce for GCM
    private static final int TAG_BITS = 128;    // 128-bit auth tag
    private static final SecureRandom RNG = new SecureRandom();

    private final SecretKey key;

    public AesGcmCrypto(@Value("${crypto.aes.key-base64}") String keyBase64) {
        byte[] k = Base64.getDecoder().decode(keyBase64);
        this.key = new SecretKeySpec(k, "AES");
    }

    /** Encrypts plaintext and returns Base64( IV || CIPHERTEXT_WITH_TAG ). */
    public String encrypt(String plaintext) {
        System.out.println(plaintext);
        try {
            byte[] iv = new byte[IV_BYTES];
            RNG.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORM);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] ct = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            byte[] out = ByteBuffer.allocate(iv.length + ct.length)
                    .put(iv)
                    .put(ct)
                    .array();
            return Base64.getEncoder().encodeToString(out);
        } catch (Exception e) {
            throw new RuntimeException("AES-GCM encrypt failed", e);
        }
    }

    /** Decrypts Base64( IV || CIPHERTEXT_WITH_TAG ) back to plaintext. */
    public String decrypt(String base64IvCt) {
        try {
            byte[] all = Base64.getDecoder().decode(base64IvCt);
            if (all.length <= IV_BYTES) {
                throw new IllegalArgumentException("Ciphertext too short");
            }
            byte[] iv = new byte[IV_BYTES];
            byte[] ct = new byte[all.length - IV_BYTES];
            System.arraycopy(all, 0, iv, 0, IV_BYTES);
            System.arraycopy(all, IV_BYTES, ct, 0, ct.length);

            Cipher cipher = Cipher.getInstance(TRANSFORM);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] pt = cipher.doFinal(ct);
            return new String(pt, StandardCharsets.UTF_8);
        } catch (Exception e) {
            // If key/IV/ciphertext don’t match, GCM throws AEADBadTagException here.
            throw new RuntimeException("AES-GCM decrypt failed (bad key/IV/data?)", e);
        }
    }
}
