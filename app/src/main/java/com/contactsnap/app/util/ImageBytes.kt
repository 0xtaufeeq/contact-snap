package com.contactsnap.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/** Image helpers: prepare a base64 JPEG for Gemini, and persist a thumbnail copy. */
object ImageBytes {

    fun toBase64Jpeg(context: Context, uri: Uri, maxDim: Int = 1536, quality: Int = 85): String {
        val bmp = decodeDownscaled(context, uri, maxDim)
        val out = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.JPEG, quality, out)
        bmp.recycle()
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }

    /** Saves a downscaled JPEG copy under filesDir/scans/<id>.jpg; returns the absolute path. */
    fun saveScanCopy(context: Context, uri: Uri, id: String, maxDim: Int = 1280, quality: Int = 80): String {
        val bmp = decodeDownscaled(context, uri, maxDim)
        val dir = File(context.filesDir, "scans").apply { mkdirs() }
        val file = File(dir, "$id.jpg")
        FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.JPEG, quality, it) }
        bmp.recycle()
        return file.absolutePath
    }

    private fun decodeDownscaled(context: Context, uri: Uri, maxDim: Int): Bitmap {
        val raw = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IOException("Could not read the image.")

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(raw, 0, raw.size, bounds)
        val sample = sampleSize(bounds.outWidth, bounds.outHeight, maxDim)

        val decoded = BitmapFactory.decodeByteArray(
            raw, 0, raw.size,
            BitmapFactory.Options().apply { inSampleSize = sample }
        ) ?: throw IOException("Could not decode the image.")

        val scaled = scaleToMax(decoded, maxDim)
        if (scaled != decoded) decoded.recycle()
        return scaled
    }

    private fun sampleSize(w: Int, h: Int, maxDim: Int): Int {
        var sample = 1
        var longest = maxOf(w, h)
        while (longest / 2 >= maxDim) {
            longest /= 2
            sample *= 2
        }
        return sample
    }

    private fun scaleToMax(bmp: Bitmap, maxDim: Int): Bitmap {
        val longest = maxOf(bmp.width, bmp.height)
        if (longest <= maxDim) return bmp
        val ratio = maxDim.toFloat() / longest
        return Bitmap.createScaledBitmap(bmp, (bmp.width * ratio).toInt(), (bmp.height * ratio).toInt(), true)
    }
}
