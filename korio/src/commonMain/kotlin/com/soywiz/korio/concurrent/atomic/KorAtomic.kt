package com.soywiz.korio.concurrent.atomic

import kotlin.reflect.*

expect fun <T> korAtomic(initial: T): KorAtomicRef<T>
expect fun korAtomic(initial: Boolean): KorAtomicBoolean
expect fun korAtomic(initial: Int): KorAtomicInt
expect fun korAtomic(initial: Long): KorAtomicLong

//fun <T> korAtomic(initial: T): KorAtomicRef<T> = KorAtomicRef(initial)
//fun korAtomic(initial: Boolean): KorAtomicBoolean = KorAtomicBoolean(initial)
//fun korAtomic(initial: Int): KorAtomicInt = KorAtomicInt(initial)
//fun korAtomic(initial: Long): KorAtomicLong = KorAtomicLong (initial)

interface KorAtomicBase<T> {
	var value: T
	fun compareAndSet(expect: T, update: T): Boolean
}

interface KorAtomicNumber<T : Number> : KorAtomicBase<T> {
	fun addAndGet(delta: T): T
}

open class KorAtomicRef<T>(initial: T) : KorAtomicBase<T> {
	override var value: T = initial

	override fun compareAndSet(expect: T, update: T): Boolean {
		return if (value == expect) {
			value = update
			true
		} else {
			false
		}
	}
}

open class KorAtomicBoolean(initial: Boolean) : KorAtomicBase<Boolean> {
	override var value: Boolean = initial

	override fun compareAndSet(expect: Boolean, update: Boolean): Boolean {
		return if (value == expect) {
			value = update
			true
		} else {
			false
		}
	}
}

open class KorAtomicInt(initial: Int) : KorAtomicNumber<Int> {
	override var value: Int = initial

	override fun compareAndSet(expect: Int, update: Int): Boolean {
		return if (value == expect) {
			value = update
			true
		} else {
			false
		}
	}

	override fun addAndGet(delta: Int): Int {
		this.value += delta
		return this.value
	}
}

open class KorAtomicLong(initial: Long) : KorAtomicNumber<Long> {
	override var value: Long = initial

	override fun compareAndSet(expect: Long, update: Long): Boolean {
		return if (value == expect) {
			value = update
			true
		} else {
			false
		}
	}

	override fun addAndGet(delta: Long): Long {
		this.value += delta
		return this.value
	}
}

fun KorAtomicInt.incrementAndGet() = addAndGet(1)
fun KorAtomicLong.incrementAndGet() = addAndGet(1)


inline operator fun <T> KorAtomicRef<T>.getValue(obj: Any, prop: KProperty<Any?>): T = this.value
inline operator fun <T> KorAtomicRef<T>.setValue(obj: Any, prop: KProperty<Any?>, v: T) = run { this.value = v }

inline operator fun KorAtomicBoolean.getValue(obj: Any, prop: KProperty<Any?>): Boolean = this.value
inline operator fun KorAtomicBoolean.setValue(obj: Any, prop: KProperty<Any?>, v: Boolean) = run { this.value = v }

inline operator fun KorAtomicInt.getValue(obj: Any, prop: KProperty<Any?>): Int = this.value
inline operator fun KorAtomicInt.setValue(obj: Any, prop: KProperty<Any?>, v: Int) = run { this.value = v }

inline operator fun KorAtomicLong.getValue(obj: Any, prop: KProperty<Any?>): Long = this.value
inline operator fun KorAtomicLong.setValue(obj: Any, prop: KProperty<Any?>, v: Long) = run { this.value = v }
