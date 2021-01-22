package com.soywiz.klogger.atomic

actual class KloggerAtomicRef<T> actual constructor(initial: T) {
    @PublishedApi
    internal var _value: T = initial
    actual val value: T get() = _value
    actual inline fun update(block: (T) -> T) {
        _value = block(_value)
    }
}

