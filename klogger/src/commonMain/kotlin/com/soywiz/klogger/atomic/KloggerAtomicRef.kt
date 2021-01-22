package com.soywiz.klogger.atomic

import kotlin.reflect.KMutableProperty0

internal expect class KloggerAtomicRef<T>(initial: T) {
    var value: T
    inline fun update(block: (T) -> T)
}

//expect fun <T> kloggerAtomicRef(initial: T): KloggerAtomicRef<T>
internal fun <T> kloggerAtomicRef(initial: T): KMutableProperty0<T> = KloggerAtomicRef(initial)::value
