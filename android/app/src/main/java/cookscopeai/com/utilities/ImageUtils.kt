package cookscopeai.com.utilities

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream

/**
 * Resizes a bitmap so its longest side is at most [maxDimension], then encodes
 * it as a JPEG Base64 string suitable for sending to vision-capable AI models.
 * The output is kept under ~2.5 MB by lowering JPEG quality if needed.
 */
fun Bitmap.toBase64Jpeg(maxDimension: Int = 1024): String {
    val resized = resizeBitmap(this, maxDimension)
    val stream = ByteArrayOutputStream()
    var quality = 85
    resized.compress(Bitmap.CompressFormat.JPEG, quality, stream)
    while (stream.size() > 2_500_000 && quality > 40) {
        quality -= 15
        stream.reset()
        resized.compress(Bitmap.CompressFormat.JPEG, quality, stream)
    }
    return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
}

/**
 * Loads a [Uri] as a [Bitmap] using the content resolver, then converts it to a
 * Base64 JPEG string. Returns null if the image cannot be loaded.
 */
fun Uri.toBase64Jpeg(context: Context, maxDimension: Int = 1024): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(this) ?: return null
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream.close()
        bitmap?.toBase64Jpeg(maxDimension)
    } catch (_: Exception) {
        null
    }
}

private fun resizeBitmap(bitmap: Bitmap, maxDimension: Int): Bitmap {
    val width = bitmap.width
    val height = bitmap.height
    if (width <= maxDimension && height <= maxDimension) return bitmap

    val ratio = width.toFloat() / height.toFloat()
    val (targetW, targetH) = if (width > height) {
        maxDimension to (maxDimension / ratio).toInt()
    } else {
        (maxDimension * ratio).toInt() to maxDimension
    }
    return Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
}
