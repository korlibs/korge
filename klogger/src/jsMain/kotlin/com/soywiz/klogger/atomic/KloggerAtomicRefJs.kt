package com.soywiz.klogger.atomic

internal actual class KloggerAtomicRef<T> actual constructor(initial: T) {

    actual var value: T = initial

    actual inline fun update(block: (T) -> T) {
        value = block(value)
    }
}
