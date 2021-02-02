package com.soywiz.korio.dynamic

@Suppress("DEPRECATION")
@Deprecated("Use Dyn instead")
open class KDynamic {
	companion object : KDynamic() {
		inline operator fun <T> invoke(callback: KDynamic.() -> T): T = callback(KDynamic)
		inline operator fun <T, R> invoke(value: T, callback: KDynamic.(T) -> R): R = callback(KDynamic, value)
	}

	val global get() = Dyn.global.value

	interface Invokable {
		fun invoke(name: String, args: Array<out Any?>): Any?
	}

	fun Any?.dynamicInvoke(name: String, vararg args: Any?): Any? = this.dyn.dynamicInvoke(name, *args).value
	operator fun Any?.set(key: Any?, value: Any?) = this.dyn.set(key, value)
	operator fun Any?.get(key: Any?): Any? = this.dyn.get(key).value
	val Any?.map: Map<Any?, Any?> get() = this.dyn.mapAny
	val Any?.list: List<Any?> get() = this.dyn.listAny
	val Any?.keys: List<Any?> get() = this.dyn.keysAny
	fun Any?.toNumber(): Number = this.dyn.toNumber()
	fun Any?.toBool(): Boolean = this.dyn.toBool()
	fun Any?.toByte(): Byte = this.dyn.toByte()
	fun Any?.toChar(): Char = this.dyn.toChar()
	fun Any?.toShort(): Short = this.dyn.toShort()
	fun Any?.toInt(): Int = this.dyn.toInt()
	fun Any?.toLong(): Long = this.dyn.toLong()
	fun Any?.toFloat(): Float = this.dyn.toFloat()
	fun Any?.toDouble(): Double = this.dyn.toDouble()
	fun Any?.toBoolOrNull(): Boolean? = this.dyn.toBoolOrNull()
	fun Any?.toIntOrNull(): Int? = this.dyn.toIntOrNull()
	fun Any?.toLongOrNull(): Long? = this.dyn.toLongOrNull()
	fun Any?.toDoubleOrNull(): Double? = this.dyn.toDoubleOrNull()
	fun Any?.toIntDefault(default: Int = 0): Int = this.dyn.toIntDefault(default)
	fun Any?.toLongDefault(default: Long = 0L): Long = this.dyn.toLongDefault(default)
	fun Any?.toFloatDefault(default: Float = 0f): Float = this.dyn.toFloatDefault(default)
	fun Any?.toDoubleDefault(default: Double = 0.0): Double = this.dyn.toDoubleDefault(default)

	val Any?.str: String get() = this.dyn.str
	val Any?.int: Int get() = this.dyn.int
	val Any?.bool: Boolean get() = this.dyn.bool
	val Any?.float: Float get() = this.dyn.float
	val Any?.double: Double get() = this.dyn.double
	val Any?.long: Long get() = this.dyn.long

	val Any?.intArray: IntArray get() = this.dyn.intArray
	val Any?.floatArray: FloatArray get() = this.dyn.floatArray
	val Any?.doubleArray: DoubleArray get() = this.dyn.doubleArray
	val Any?.longArray: LongArray get() = this.dyn.longArray
}
