package com.javaisland.bank_backend.security;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class EncryptionServiceTest {

    private static final String VALID_KEY_32B = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    private EncryptionService service() {
        return new EncryptionService(VALID_KEY_32B);
    }

    @Test
    void encryptDecrypt_roundTrip() {
        EncryptionService svc = service();
        String plain = "1234567890123456";
        String ciphertext = svc.encrypt(plain);
        assertNotNull(ciphertext);
        assertNotEquals(plain, ciphertext);
        assertEquals(plain, svc.decrypt(ciphertext));
    }

    @Test
    void encrypt_generatesDifferentCiphertextEachCall() {
        EncryptionService svc = service();
        String plain = "0001112223334445";
        assertNotEquals(svc.encrypt(plain), svc.encrypt(plain));
    }

    @Test
    void decrypt_withWrongKey_throws() {
        EncryptionService svc = service();
        String ciphertext = svc.encrypt("1234");

        String otherKey = Base64.getEncoder().encodeToString("abcdefghijklmnopqrstuvwxyz123456".getBytes());
        EncryptionService other = new EncryptionService(otherKey);

        assertThrows(IllegalStateException.class, () -> other.decrypt(ciphertext));
    }

    @Test
    void decrypt_tamperedCiphertext_throws() {
        EncryptionService svc = service();
        String ciphertext = svc.encrypt("1234");
        String tampered = ciphertext.substring(0, ciphertext.length() - 2) + "aa";
        assertThrows(IllegalStateException.class, () -> svc.decrypt(tampered));
    }

    @Test
    void constructor_withShortKey_throws() {
        String shortKey = Base64.getEncoder().encodeToString("too short".getBytes());
        assertThrows(IllegalArgumentException.class, () -> new EncryptionService(shortKey));
    }

    @Test
    void constructor_withBlankKey_throws() {
        assertThrows(IllegalArgumentException.class, () -> new EncryptionService("   "));
    }

    @Test
    void sha256Hex_isDeterministicAndKnown() {
        assertEquals("9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08",
                EncryptionService.sha256Hex("test"));
        assertEquals(EncryptionService.sha256Hex("1234567890123456"),
                EncryptionService.sha256Hex("1234567890123456"));
    }
}