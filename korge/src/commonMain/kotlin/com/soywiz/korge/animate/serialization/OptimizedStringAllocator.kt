package com.soywiz.korge.animate.serialization

import kotlin.collections.contains
import kotlin.collections.hashMapOf
import kotlin.collections.map
import kotlin.collections.plus
import kotlin.collections.set
import kotlin.collections.sortedByDescending
import kotlin.collections.toTypedArray

class OptimizedStringAllocator {
	private var finalized = false
	private val stringsCount = hashMapOf<String, Int>()
	var strings = arrayOf<String?>(); private set
	private val stringsToIndex = hashMapOf<String, Int>()

	fun add(str: String?) {
		if (finalized) throw IllegalStateException()
		if (str != null) {
			if (str !in stringsCount) {
				stringsCount[str] = 0
			}
			stringsCount[str] = stringsCount[str]!! + 1
		}
	}

	operator fun get(str: String?): Int = getIndex(str)

	fun getIndex(str: String?): Int {
		if (!finalized) throw IllegalStateException()
		if (str == null) {
			return 0
		} else {
			return stringsToIndex[str]!!
		}
	}

	fun finalize() {
		this.strings = arrayOf<String?>(null) +
				stringsCount.entries.sortedByDescending { it.value }.map { it.key }.toTypedArray()
		for (n in 1 until this.strings.size) {
			stringsToIndex[this.strings[n]!!] = n
		}
		finalized = true
	}
}
