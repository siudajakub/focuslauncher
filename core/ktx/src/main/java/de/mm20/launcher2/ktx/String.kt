package de.mm20.launcher2.ktx

import org.apache.commons.lang3.StringUtils
import java.net.URLDecoder
import java.util.Locale

fun String.decodeUrl(charset: String): String? {
    return URLDecoder.decode(this, charset)
}

fun String.stripStartOrNull(s: String): String?
    = if (startsWith(s)) removePrefix(s) else null

fun String.stripEndOrNull(s: String): String?
    = if (endsWith(s)) removeSuffix(s) else null

fun String.normalize(): String {
    return StringUtils.stripAccents(lowercase(Locale.ROOT))
        .replace("æ", "ae")
        .replace("œ", "oe")
        .replace("ß", "ss")
        .replace("文", "文wen")
        .replace("件", "件jian")
}
