package korlibs.io.util

fun String.htmlspecialchars(): String = buildString(this@htmlspecialchars.length + 16) {
    for (it in this@htmlspecialchars) {
        when (it) {
            '"' -> append("&quot;")
            '\'' -> append("&apos;")
            '<' -> append("&lt;")
            '>' -> append("&gt;")
            '&' -> append("&amp;")
            else -> append(it)
        }
    }
}
