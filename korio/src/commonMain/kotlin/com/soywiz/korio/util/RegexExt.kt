package com.soywiz.korio.util

import com.soywiz.korio.lang.*

//fun Regex.Companion.quote(str: String): String = str.replace(Regex("[.?*+^\$\\[\\]\\\\(){}|\\-]")) { "\\${it.value}" }

fun Regex.Companion.isSpecial(c: Char): Boolean = when (c) {
    '.', '?', '*', '+', '^', '\\', '$', '[', ']', '(', ')', '{', '}', '|', '-' -> true
    else -> false
}

fun Regex.Companion.quote(str: String): String = str.eachBuilder { c ->
    if (Regex.isSpecial(c)) append('\\')
	append(c)
}

/**
 * Converts a typical glob (*, ?) into a regular expression
 */
fun Regex.Companion.fromGlob(glob: String): Regex {
    return Regex(buildString {
        append("^")
        for (c in glob) {
            when (c) {
                '*' -> append(".*")
                '?' -> append(".?")
                '.' -> append("\\.")
                else -> {
                    if (Regex.isSpecial(c)) append('\\')
                    append(c)
                }
            }
        }
        append("$")
    })
}
