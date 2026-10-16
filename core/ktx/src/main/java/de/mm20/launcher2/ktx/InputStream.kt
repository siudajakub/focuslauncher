package de.mm20.launcher2.ktx

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.Charset

fun InputStream.asBitmap(options : BitmapFactory.Options? = null): Bitmap? {
    return BitmapFactory.decodeStream(this, null, options)
}

/**
 * Reads this input stream completely as a String using the specified [charset],
 * but throws an [IllegalArgumentException] if the stream size exceeds [maxLength] characters.
 * This is useful to prevent OutOfMemoryError when reading from untrusted sources (e.g., zip bombs).
 */
fun InputStream.readTextLimited(maxLength: Int, charset: Charset = Charsets.UTF_8): String {
    val reader = InputStreamReader(this, charset)
    val buffer = CharArray(8192)
    val builder = StringBuilder()
    var charsRead = 0
    var read: Int
    while (reader.read(buffer).also { read = it } != -1) {
        charsRead += read
        if (charsRead > maxLength) {
            throw IllegalArgumentException("Input stream exceeded maximum length of $maxLength characters")
        }
        builder.append(buffer, 0, read)
    }
    return builder.toString()
}
