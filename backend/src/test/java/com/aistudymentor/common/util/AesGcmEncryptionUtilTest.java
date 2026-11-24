package com.aistudymentor.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AesGcmEncryptionUtilTest {

    @Test
    void encryptsAndDecryptsWithConfiguredAesKey() {
        AesGcmEncryptionUtil encryption = new AesGcmEncryptionUtil("01234567890123456789012345678901");

        assertEquals("totp-secret", encryption.decrypt(encryption.encrypt("totp-secret")));
    }

    @Test
    void rejectsKeysShorterThanTheMinimumLength() {
        assertThrows(IllegalStateException.class,
                () -> new AesGcmEncryptionUtil("short-key"));
    }
}
