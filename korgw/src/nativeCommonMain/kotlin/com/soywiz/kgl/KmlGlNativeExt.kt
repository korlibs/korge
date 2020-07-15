package com.soywiz.kgl

import com.soywiz.kmem.*
import kotlinx.cinterop.*

fun Boolean.toBool(): Boolean = this
fun Byte.toBool(): Boolean = this.toInt() != 0
fun Int.toBool(): Boolean = this != 0
fun Long.toBool(): Boolean = this != 0L

fun UByte.toBool(): Boolean = this.toUInt() != 0u
fun UInt.toBool(): Boolean = this != 0u
fun ULong.toBool(): Boolean = this != 0uL

fun CPointer<UByteVar>.toKString(): String = this.reinterpret<ByteVar>().toKString()
//inline fun <reified R : Any> Boolean.convert(): R = (if (this) 1 else 0).convert() // @TODO: Doesn't work

fun Boolean.toInt(): Int = if (this) 1 else 0

fun Int.convertSize(): Long = this.toLong() // For 64-bit
fun Float.convertFloat(): Double = this.toDouble() // For 64-bit

class TempBufferAddress {
	val pool = arrayListOf<Pinned<ByteArray>>()
	companion object {
		val ARRAY1 = ByteArray(1)
	}
	fun FBuffer.unsafeAddress(): CPointer<ByteVar> {
		val byteArray = this.mem.data
		val rbyteArray = if (byteArray.size > 0) byteArray else ARRAY1
		val pin = rbyteArray.pin()
		pool += pin
		return pin.addressOf(0)
	}

	fun start() {
		pool.clear()
	}

	fun dispose() {
		// Kotlin-native: Try to avoid allocating an iterator (lists not optimized yet)
		for (n in 0 until pool.size) pool[n].unpin()
		//for (p in pool) p.unpin()
		pool.clear()
	}

	inline operator fun <T> invoke(callback: TempBufferAddress.() -> T): T {
		start()
		try {
			return callback()
		} finally {
			dispose()
		}
	}
}
