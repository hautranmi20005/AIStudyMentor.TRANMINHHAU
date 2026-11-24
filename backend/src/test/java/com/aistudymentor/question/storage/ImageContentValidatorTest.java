package com.aistudymentor.question.storage;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ImageContentValidatorTest {

    @Test
    void acceptsSupportedMagicBytesAndRejectsRenamedOrCorruptFiles() {
        assertEquals("image/png", ImageContentValidator.validate("image/png", pngHeader()));
        assertEquals("image/jpeg", ImageContentValidator.validate(
                "image/jpeg", new byte[] {(byte) 0xff, (byte) 0xd8, (byte) 0xff, 0}));
        assertEquals("image/webp", ImageContentValidator.validate(
                "image/webp", new byte[] {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'}));
        assertEquals("image/heic", ImageContentValidator.validate(
                "image/heic", isoBrand("heic")));
        assertEquals("image/heif", ImageContentValidator.validate(
                "image/heif", isoBrand("mif1")));

        assertThrows(ImageContentValidator.InvalidImageException.class,
                () -> ImageContentValidator.validate("image/jpeg", pngHeader()));
        assertThrows(ImageContentValidator.InvalidImageException.class,
                () -> ImageContentValidator.validate("image/png", new byte[] {'%', 'P', 'D', 'F'}));
    }

    @Test
    void rejectsFilesAboveTenMegabytes() {
        byte[] oversized = Arrays.copyOf(pngHeader(), (int) ImageContentValidator.MAX_IMAGE_BYTES + 1);
        assertThrows(ImageContentValidator.InvalidImageException.class,
                () -> ImageContentValidator.validate("image/png", oversized));
    }

    private byte[] pngHeader() {
        return new byte[] {(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10};
    }

    private byte[] isoBrand(String brand) {
        return new byte[] {0, 0, 0, 0, 'f', 't', 'y', 'p',
                (byte) brand.charAt(0), (byte) brand.charAt(1),
                (byte) brand.charAt(2), (byte) brand.charAt(3)};
    }
}
