package com.aistudymentor.ui.ai

import androidx.exifinterface.media.ExifInterface
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImagePreprocessorTest {
    @Test
    fun mapsExifRotationAndMirroringOrientations() {
        assertEquals(90, ImagePreprocessor.rotationDegrees(ExifInterface.ORIENTATION_ROTATE_90))
        assertEquals(180, ImagePreprocessor.rotationDegrees(ExifInterface.ORIENTATION_ROTATE_180))
        assertEquals(270, ImagePreprocessor.rotationDegrees(ExifInterface.ORIENTATION_ROTATE_270))
        assertEquals(0, ImagePreprocessor.rotationDegrees(ExifInterface.ORIENTATION_NORMAL))
    }

    @Test
    fun downscalesTheLongEdgeWithoutChangingAspectRatio() {
        assertEquals(1600 to 800, ImagePreprocessor.targetDimensions(4000, 2000, 1600))
        assertEquals(800 to 1600, ImagePreprocessor.targetDimensions(2000, 4000, 1600))
        assertTrue(ImagePreprocessor.calculateInSampleSize(4000, 2000, 1600) > 1)
        assertEquals(1, ImagePreprocessor.calculateInSampleSize(1200, 800, 1600))
    }

    @Test
    fun acceptsOnlyNonemptyImagesAtOrBelowTenMegabytes() {
        assertTrue(ImagePreprocessor.isWithinUploadLimit(1))
        assertTrue(ImagePreprocessor.isWithinUploadLimit(ImagePreprocessor.MAX_IMAGE_BYTES.toLong()))
        assertFalse(ImagePreprocessor.isWithinUploadLimit(0))
        assertFalse(ImagePreprocessor.isWithinUploadLimit(ImagePreprocessor.MAX_IMAGE_BYTES.toLong() + 1))
    }
}
