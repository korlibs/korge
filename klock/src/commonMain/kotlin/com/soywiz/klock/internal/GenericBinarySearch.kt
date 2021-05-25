package com.soywiz.klock.internal

import kotlin.jvm.JvmInline

// Original implementation grabbed from Kds to prevent additional dependencies:
// - https://github.com/korlibs/kds/blob/965f6017d7ad82e4bad714acf26cd7189186bdb3/kds/src/commonMain/kotlin/com/soywiz/kds/_Extensions.kt#L48
internal inline fun genericBinarySearch(
	fromIndex: Int,
	toIndex: Int,
	invalid: (from: Int, to: Int, low: Int, high: Int) -> Int = { from, to, low, high -> -low - 1 },
	check: (index: Int) -> Int
): Int {
	var low = fromIndex
	var high = toIndex - 1

	while (low <= high) {
		val mid = (low + high) / 2
		val mval = check(mid)

		when {
			mval < 0 -> low = mid + 1
			mval > 0 -> high = mid - 1
			else -> return mid
		}
	}
	return invalid(fromIndex, toIndex, low, high)
}

@JvmInline
internal value class BSearchResult(val raw: Int) {
	val found: Boolean get() = raw >= 0
	val index: Int get() = if (found) raw else -1
	val nearIndex: Int get() = if (found) raw else -raw - 1
}
