package com.soywiz.klogger.atomic

import kotlin.native.concurrent.*

actual fun <T> kloggerAtomicRef(initial: T): KloggerAtomicRef<T> = object : KloggerAtomicRef<T>() {
    private val ref = FreezableAtomicReference<T>(initial.freeze())

    override var value: T
        get() = ref.value
        set(value) { ref.value = value.freeze() }
}
