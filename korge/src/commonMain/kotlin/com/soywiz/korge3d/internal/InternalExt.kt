package com.soywiz.korge3d.internal

import com.soywiz.kds.*
import com.soywiz.kmem.*
import com.soywiz.korma.geom.Vector3D
import kotlin.native.concurrent.ThreadLocal

internal fun <T : Any> Map<String, T>.toFast() = FastStringMap<T>().apply {
	@Suppress("MapGetWithNotNullAssertionOperator")
	for (k in this@toFast.keys) {
		this[k] = this@toFast[k]!!
	}
}

internal operator fun Vector3D.get(char: Char): Float = when (char) {
	'x', 'r' -> this[0]
	'y', 'g' -> this[1]
	'z', 'b' -> this[2]
	'w', 'a' -> this[3]
	'0' -> 0f
	'1' -> 1f
	else -> Float.NaN
}

internal fun Vector3D.swizzle(name: String, input: Vector3D = this): Vector3D {
	val x = name.getOrElse(0) { '0' }
	val y = name.getOrElse(1) { '0' }
	val z = name.getOrElse(2) { '0' }
	val w = name.getOrElse(3) { '1' }
	return this.setTo(input[x], input[y], input[z], input[w])
}

internal operator fun Vector3D.get(name: String): Vector3D = Vector3D().copyFrom(this).swizzle(name)

@ThreadLocal
internal val vector3DTemps = Vector3DTemps()

internal class Vector3DTemps {
	@PublishedApi
	internal var pos = 0
	@PublishedApi
	internal val items = arrayListOf<Vector3D>(Vector3D(), Vector3D(), Vector3D())

	fun alloc(): Vector3D {
		val npos = pos++
		return if (npos < items.size) {
			items[npos]
		} else {
			val item = Vector3D()
			items.add(item)
			item
		}
	}

	inline operator fun <T> invoke(callback: Vector3DTemps.() -> T): T {
		val oldPos = pos
		try {
			return callback()
		} finally {
			pos = oldPos
		}
	}

	operator fun Vector3D.plus(that: Vector3D) = alloc().setToFunc { this[it] + that[it] }
	operator fun Vector3D.minus(that: Vector3D) = alloc().setToFunc { this[it] - that[it] }
}

internal fun FloatArrayList.toNBuffer(): Buffer = toFloatArray().toNBuffer()
internal fun FloatArray.toNBuffer(): Buffer = Buffer.allocDirect(this.size * 4).also { it.setArrayFloat32(0, this, 0, this.size) }
internal fun IntArrayList.toNBuffer(): Buffer = toIntArray().toNBuffer()
internal fun IntArray.toNBuffer(): Buffer = Buffer.allocDirect(this.size * 4).also { it.setArrayInt32(0, this, 0, this.size) }
internal fun ShortArrayList.toNBuffer(): Buffer = toShortArray().toNBuffer()
internal fun ShortArray.toNBuffer(): Buffer = Buffer.allocDirect(this.size * 2).also { it.setArrayInt16(0, this, 0, this.size) }
