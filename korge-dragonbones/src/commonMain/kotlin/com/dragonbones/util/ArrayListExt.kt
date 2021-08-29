package com.dragonbones.util

import com.soywiz.kds.*

internal var IntArrayList.lengthSet: Int
	get() = size
	set(value) { size = value }


internal var DoubleArrayList.lengthSet: Int
	get() = size
	set(value) { size = value }

internal var IntArrayList.length: Int
	get() = size
	set(value) { size = value }

internal var DoubleArrayList.length: Int
	get() = size
	set(value) { size = value }


internal var <T> FastArrayList<T>.lengthSet
	get() = size
	set(value) = if (value == 0) clear() else {
		while (size > value) this.removeAt(size - 1)
		@Suppress("UNCHECKED_CAST")
		while (size < value) (this as FastArrayList<T?>).add(null)
	}

internal var <T> FastArrayList<T>.length; get() = size; set(value) { lengthSet = value }

internal fun DoubleArrayList.push(value: Double) = this.add(value)
internal fun IntArrayList.push(value: Int) = this.add(value)

internal fun <T> MutableList<T>.splice(removeOffset: Int, removeCount: Int, vararg itemsToAdd: T) {
	// @TODO: Improve performance
	for (n in 0 until removeCount) this.removeAt(removeOffset)
	for (n in 0 until itemsToAdd.size) {
		this.add(removeOffset + n, itemsToAdd[n])
	}
}

