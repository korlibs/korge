package com.soywiz.klogger.atomic

import java.util.concurrent.atomic.AtomicReference

actual fun <T> kloggerAtomicRef(initial: T): KloggerAtomicRef<T> = object : KloggerAtomicRef<T>() {
    private val ref = AtomicReference<T>(initial)
    override var value: T
        get() = ref.get()
        set(value) { ref.set(value) }
}
