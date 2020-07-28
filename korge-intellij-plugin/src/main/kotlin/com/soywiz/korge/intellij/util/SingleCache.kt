package com.soywiz.korge.intellij.util

class SingleCache<K : Any, V : Any> {
	var key: K? = null
	var value: V? = null
	fun invalidate() {
		key = null
	}
	fun get(key: K, callback: () -> V): V {
		if (this.key != key) {
			this.key = key
			value = callback()
		}
		return value!!
	}
}
