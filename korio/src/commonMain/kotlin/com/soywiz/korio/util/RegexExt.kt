package com.soywiz.korio.util

import com.soywiz.korio.lang.*

//fun Regex.Companion.quote(str: String): String = str.replace(Regex("[.?*+^\$\\[\\]\\\\(){}|\\-]")) { "\\${it.value}" }

fun Regex.Companion.quote(str: String): String = str.eachBuilder { c ->
	when (c) {
		'.', '?', '*', '+', '^', '\\', '$', '[', ']', '(', ')', '{', '}', '|', '-' -> append('\\')
	}
	append(c)
}
