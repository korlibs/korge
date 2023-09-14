package korlibs.template.internal

internal infix fun Int.umod(other: Int): Int {
	val remainder = this % other
	return when {
		remainder < 0 -> remainder + other
		else -> remainder
	}
}

internal fun Int.mask(): Int = (1 shl this) - 1
internal fun Int.extract(offset: Int, count: Int): Int = (this ushr offset) and count.mask()
