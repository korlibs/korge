package com.soywiz.korio.concurrent.atomic

actual fun <T> korAtomic(initial: T): KorAtomicRef<T> = KorAtomicRef(initial, true)
actual fun korAtomic(initial: Boolean): KorAtomicBoolean = KorAtomicBoolean(initial, true)
actual fun korAtomic(initial: Int): KorAtomicInt = KorAtomicInt(initial, true)
actual fun korAtomic(initial: Long): KorAtomicLong = KorAtomicLong(initial, true)
