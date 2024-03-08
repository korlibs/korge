package korlibs.io.util

data class Glob(
    val pattern: String,
    val extended: Boolean = false,
    val globstar: Boolean = false,
    val full: Boolean = true,
    val ignoreCase: Boolean = false,
    val multiline: Boolean = false,

) {
    val regex: Regex = globToRegExp(pattern, extended = extended, globstar = globstar, full = full, ignoreCase = ignoreCase, multiline = multiline)

    infix fun matches(input: CharSequence): Boolean {
        return regex.matches(input)
    }

    companion object {
        private fun globToRegExp(str: String, ignoreCase: Boolean = false, multiline: Boolean = false, extended: Boolean = false, globstar: Boolean = false, full: Boolean = true): Regex {
            var reStr = ""

            var inGroup = false
            for (i in str.indices) {
                val c = str[i]

                reStr += when (c) {
                    '/', '$', '^', '+', '.', '(', ')', '=', '!', '|' -> "\\$c"
                    '?' -> if (extended) "." else "\\$c"
                    '[', ']' -> if (extended) "$c" else "\\$c"
                    '{' -> if (extended) {
                        inGroup = true
                        "("
                    } else "\\$c"
                    '}' -> if (extended) {
                        inGroup = false
                        ")"
                    } else "\\$c"
                    ',' -> if (inGroup) "|" else "\\$c"
                    '*' -> {
                        val prevChar = str.getOrNull(i - 1)
                        var starCount = 1
                        while (str.getOrNull(i + 1) == '*') starCount++
                        val nextChar = str.getOrNull(i + 1)

                        if (!globstar) {
                            ".*"
                        } else {
                            val isGlobstar = starCount > 1 && (prevChar == '/' || prevChar == null) && (nextChar == '/' || nextChar == null)
                            when {
                                isGlobstar -> "((?:[^/]*(?:\\/|$))*)"
                                else -> "([^/]*)"
                            }
                        }
                    }
                    else -> "$c"
                }
            }

            if (full) {
                reStr = "^$reStr$"
            }

            return Regex(reStr, setOfNotNull(
                if (ignoreCase) RegexOption.IGNORE_CASE else null,
                if (multiline) RegexOption.MULTILINE else null,
            ))
        }
    }
}