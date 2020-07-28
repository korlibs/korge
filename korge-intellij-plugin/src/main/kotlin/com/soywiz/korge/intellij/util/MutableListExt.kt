package com.soywiz.korge.intellij.util

fun <T> MutableList<T>.swapIndices(a: Int, b: Int) {
	val temp = this[a]
	this[a] = this[b]
	this[b] = temp
}