package com.soywiz.korge.animate.serialization

class OptimizedStringAllocator {
	private var finalized = false
	private val stringsCount = hashMapOf<String, Int>()
	var strings = arrayOf<String>(); private set
	private val stringsToIndex = hashMapOf<String, Int>()

	fun add(str: String) {
		if (finalized) throw IllegalStateException()
		stringsCount.putIfAbsent(str, 0)
		stringsCount[str] = stringsCount[str]!! + 1
	}

	operator fun get(str: String): Int = getIndex(str)

	fun getIndex(str: String): Int {
		if (!finalized) throw IllegalStateException()
		return stringsToIndex[str]!!
	}

	fun finalize() {
		this.strings = stringsCount.entries.sortedByDescending { it.value }.map { it.key }.toTypedArray()
		for (n in 0 until this.strings.size) {
			stringsToIndex[this.strings[n]] = n
		}
		finalized = true
	}
}
