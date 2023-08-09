package korlibs.template.internal

import kotlin.native.concurrent.SharedImmutable

@SharedImmutable
private val charToEntity = linkedMapOf('"' to "&quot;", '\'' to "&apos;", '<' to "&lt;", '>' to "&gt;", '&' to "&amp;")

internal fun String.htmlspecialchars() = buildString {
	val str = this@htmlspecialchars
	for (n in 0 until str.length) {
		val it = str[n]
		val entry = charToEntity[it]
		when {
			entry != null -> append(entry)
			else -> append(it)
		}
	}
}
