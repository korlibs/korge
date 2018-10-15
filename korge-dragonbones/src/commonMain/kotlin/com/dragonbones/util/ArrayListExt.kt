package com.dragonbones.util

import com.soywiz.kds.*

var IntArrayList.lengthSet: Int
	get() = size
	set(value) = run { size = value }


var DoubleArrayList.lengthSet: Int
	get() = size
	set(value) = run { size = value }

var IntArrayList.length: Int
	get() = size
	set(value) = run { size = value }

var DoubleArrayList.length: Int
	get() = size
	set(value) = run { size = value }


var <T> ArrayList<T>.lengthSet
	get() = size
	set(value) = if (value == 0) clear() else {
		while (size > value) this.removeAt(size - 1)
		@Suppress("UNCHECKED_CAST")
		while (size < value) (this as ArrayList<T?>).add(null)
	}

var <T> ArrayList<T>.length; get() = size; set(value) = run { lengthSet = value }

//@Deprecated("", ReplaceWith("this.add(value)"))
fun <T> ArrayList<T>.push(value: T) {
	this.add(value)
}

fun <T> ArrayList<T>.unshift(value: T) {
	this.add(0, value)
}

fun DoubleArrayList.push(value: Double) = this.add(value)
fun IntArrayList.push(value: Int) = this.add(value)

fun <T> MutableList<T>.splice2(removeOffset: Int, removeCount: Int, vararg itemsToAdd: T) {
	// @TODO: Improve performance
	for (n in 0 until removeCount) this.removeAt(removeOffset)
	for (n in 0 until itemsToAdd.size) {
		this.add(removeOffset + n, itemsToAdd[n])
	}
}
