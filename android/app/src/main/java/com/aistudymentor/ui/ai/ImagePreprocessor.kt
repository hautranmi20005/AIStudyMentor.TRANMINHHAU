package com.aistudymentor.ui.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

data class ProcessedQuestionImage(
    val file: File,
    val contentType: String = "image/jpeg"
)

object ImagePreprocessor {
    const val MAX_IMAGE_BYTES = 10 * 1024 * 1024
    const val MAX_LONG_EDGE = 1600

    fun process(context: Context, uri: Uri): ProcessedQuestionImage {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val boundsStream = resolver.openInputStream(uri)
            ?: throw InvalidQuestionImageException("Could not open the selected image.")
        boundsStream.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw InvalidQuestionImageException("The selected file is not a readable image.")
        }

        val orientation = resolver.openInputStream(uri)?.use {
            ExifInterface(it).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        } ?: ExifInterface.ORIENTATION_NORMAL
        val decoded = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, MAX_LONG_EDGE)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }.let { options ->
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
        } ?: throw InvalidQuestionImageException("The selected image could not be decoded.")

        var oriented = applyOrientation(decoded, orientation)
        if (oriented !== decoded) decoded.recycle()
        val dimensions = targetDimensions(oriented.width, oriented.height, MAX_LONG_EDGE)
        if (dimensions.first != oriented.width || dimensions.second != oriented.height) {
            val resized = Bitmap.createScaledBitmap(oriented, dimensions.first, dimensions.second, true)
            if (resized !== oriented) oriented.recycle()
            oriented = resized
        }

        try {
            val encoded = encodeWithinLimit(oriented)
            val output = File(context.cacheDir, "question-${UUID.randomUUID()}.jpg")
            FileOutputStream(output).use { it.write(encoded) }
            return ProcessedQuestionImage(output)
        } finally {
            oriented.recycle()
        }
    }

    internal fun calculateInSampleSize(width: Int, height: Int, maxLongEdge: Int): Int {
        require(width > 0 && height > 0 && maxLongEdge > 0)
        var sampleSize = 1
        while (maxOf(width, height) / (sampleSize * 2) >= maxLongEdge) {
            sampleSize *= 2
        }
        return sampleSize
    }

    internal fun targetDimensions(width: Int, height: Int, maxLongEdge: Int): Pair<Int, Int> {
        require(width > 0 && height > 0 && maxLongEdge > 0)
        val longest = maxOf(width, height)
        if (longest <= maxLongEdge) return width to height
        val scale = maxLongEdge.toDouble() / longest
        return (width * scale).toInt().coerceAtLeast(1) to
            (height * scale).toInt().coerceAtLeast(1)
    }

    internal fun rotationDegrees(exifOrientation: Int): Int = when (exifOrientation) {
        ExifInterface.ORIENTATION_ROTATE_90,
        ExifInterface.ORIENTATION_TRANSPOSE -> 90
        ExifInterface.ORIENTATION_ROTATE_180,
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> 180
        ExifInterface.ORIENTATION_ROTATE_270,
        ExifInterface.ORIENTATION_TRANSVERSE -> 270
        else -> 0
    }

    internal fun isWithinUploadLimit(sizeBytes: Long): Boolean =
        sizeBytes in 1..MAX_IMAGE_BYTES.toLong()

    private fun applyOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.setScale(1f, -1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.setRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(270f)
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.setRotate(270f)
                matrix.postScale(-1f, 1f)
            }
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun encodeWithinLimit(bitmap: Bitmap): ByteArray {
        var current = bitmap
        var ownedScaledBitmap: Bitmap? = null
        try {
            repeat(6) {
                for (quality in intArrayOf(90, 82, 74, 66, 58, 50)) {
                    val output = ByteArrayOutputStream()
                    current.compress(Bitmap.CompressFormat.JPEG, quality, output)
                    if (isWithinUploadLimit(output.size().toLong())) return output.toByteArray()
                }
                if (current.width <= 320 && current.height <= 320) {
                    throw InvalidQuestionImageException("The processed image is larger than 10 MB.")
                }
                val nextWidth = (current.width * 0.75f).toInt().coerceAtLeast(1)
                val nextHeight = (current.height * 0.75f).toInt().coerceAtLeast(1)
                ownedScaledBitmap?.recycle()
                ownedScaledBitmap = Bitmap.createScaledBitmap(current, nextWidth, nextHeight, true)
                current = ownedScaledBitmap
                    ?: throw InvalidQuestionImageException("The image could not be resized.")
            }
            throw InvalidQuestionImageException("The processed image is larger than 10 MB.")
        } finally {
            ownedScaledBitmap?.recycle()
        }
    }
}

class InvalidQuestionImageException(message: String) : IllegalArgumentException(message)
