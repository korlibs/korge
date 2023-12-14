package korlibs.io.util

private const val HEX_DIGITS_LOWER = "0123456789abcdef"
fun String.escape(unicode: Boolean): String {
    val out = StringBuilder(this.length + 16)
    for (c in this) {
        when (c) {
            '\\' -> out.append("\\\\")
            '"' -> out.append("\\\"")
            '\n' -> out.append("\\n")
            '\r' -> out.append("\\r")
            '\t' -> out.append("\\t")
            else -> when {
                !unicode && c in '\u0000'..'\u001f' -> {
                    out.append("\\x")
                    out.append(HEX_DIGITS_LOWER[(c.code ushr 4) and 0xF])
                    out.append(HEX_DIGITS_LOWER[(c.code ushr 0) and 0xF])
                }
                unicode && !c.isPrintable() -> {
                    out.append("\\u")
                    out.append(HEX_DIGITS_LOWER[(c.code ushr 12) and 0xF])
                    out.append(HEX_DIGITS_LOWER[(c.code ushr 8) and 0xF])
                    out.append(HEX_DIGITS_LOWER[(c.code ushr 4) and 0xF])
                    out.append(HEX_DIGITS_LOWER[(c.code ushr 0) and 0xF])
                }
                else -> out.append(c)
            }
        }
    }
    return out.toString()
}
fun String.escape(): String = escape(unicode = false)
fun String.escapeUnicode(): String = escape(unicode = true)
@Deprecated("", ReplaceWith("escapeUnicode()")) fun String.uescape(): String = escapeUnicode()

fun String.unescape(): String {
    val out = StringBuilder(this.length)
    var n = 0
    while (n < this.length) {
        val c = this[n++]
        when (c) {
            '\\' -> {
                val c2 = this[n++]
                when (c2) {
                    '\\' -> out.append('\\')
                    '"' -> out.append('\"')
                    'n' -> out.append('\n')
                    'r' -> out.append('\r')
                    't' -> out.append('\t')
                    'x', 'u' -> {
                        val N = if (c2 == 'u') 4 else 2
                        val chars = this.substring(n, n + N)
                        n += N
                        out.append(chars.toInt(16).toChar())
                    }
                    else -> {
                        out.append("\\$c2")
                    }
                }
            }
            else -> out.append(c)
        }
    }
    return out.toString()
}

fun String?.quote(unicode: Boolean): String = if (this != null) "\"${this.escape(unicode)}\"" else "null"
fun String?.quote(): String = quote(unicode = false)
fun String?.quoteUnicode(): String = quote(unicode = true)
@Deprecated("", ReplaceWith("quoteUnicode()")) fun String?.uquote(): String = quoteUnicode()

fun String.isQuoted(): Boolean = this.startsWith('"') && this.endsWith('"')
fun String.unquote(): String = if (isQuoted()) this.substring(1, this.length - 1).unescape() else this

val String?.quoted: String get() = this.quote()
val String.unquoted: String get() = this.unquote()
