package de.mm20.launcher2.ktx

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.IOException
import java.io.InputStream

fun InputStream.asBitmap(options : BitmapFactory.Options? = null): Bitmap? {
    return BitmapFactory.decodeStream(this, null, options)
}

/**
 * Reads the input stream up to [maxLength] characters.
 * Throws an [IOException] if the stream exceeds the maximum length.
 * Useful to prevent OutOfMemoryError and DoS attacks (e.g. Zip Bombs).
 */
fun InputStream.readTextLimited(maxLength: Int): String {
    val reader = this.reader()
    val stringBuilder = StringBuilder()
    val buffer = CharArray(8192)
    var charsRead: Int
    var totalChars = 0
    while (reader.read(buffer).also { charsRead = it } != -1) {
        totalChars += charsRead
        if (totalChars > maxLength) {
            throw IOException("Input stream exceeded maximum length of $maxLength characters")
        }
        stringBuilder.append(buffer, 0, charsRead)
    }
    return stringBuilder.toString()
}