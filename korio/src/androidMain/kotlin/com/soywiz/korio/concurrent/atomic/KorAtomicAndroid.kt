package com.soywiz.korio.concurrent.atomic

import java.util.concurrent.atomic.*

actual fun <T> korAtomic(initial: T): KorAtomicRef<T> = object : KorAtomicRef<T>(initial) {
	val ref = AtomicReference(initial)
	override var value: T get() = ref.get(); set(value) = ref.set(value)
	override fun compareAndSet(expect: T, update: T): Boolean = ref.compareAndSet(expect, update)
}

actual fun korAtomic(initial: Boolean): KorAtomicBoolean = object : KorAtomicBoolean(initial) {
	val ref = AtomicBoolean(initial)
	override var value: Boolean get() = ref.get(); set(value) = ref.set(value)

	override fun compareAndSet(expect: Boolean, update: Boolean): Boolean = ref.compareAndSet(expect, update)
}

actual fun korAtomic(initial: Int): KorAtomicInt = object : KorAtomicInt(initial) {
	val ref = AtomicInteger(initial)
	override var value: Int get() = ref.get(); set(value) = ref.set(value)
	override fun compareAndSet(expect: Int, update: Int): Boolean = ref.compareAndSet(expect, update)
	override fun addAndGet(delta: Int): Int = ref.addAndGet(delta)
}

actual fun korAtomic(initial: Long): KorAtomicLong = object : KorAtomicLong(initial) {
	val ref = AtomicLong(initial)
	override var value: Long get() = ref.get(); set(value) = ref.set(value)
	override fun compareAndSet(expect: Long, update: Long): Boolean = ref.compareAndSet(expect, update)
	override fun addAndGet(delta: Long): Long = ref.addAndGet(delta)
}
