package com.soywiz.klogger.atomic

import kotlin.reflect.*

internal expect class KloggerAtomicRef<T>(initial: T) {
    var value: T
    inline fun update(block: (T) -> T)
}

internal operator fun <T> KloggerAtomicRef<T>.setValue(receiver: Any?, property: KProperty<*>, value: T) {
    this.value = value
}

internal operator fun <T> KloggerAtomicRef<T>.getValue(receiver: Any?, property: KProperty<*>): T {
    return this.value
}

