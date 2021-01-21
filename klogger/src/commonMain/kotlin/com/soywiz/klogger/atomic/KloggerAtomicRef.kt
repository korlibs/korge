package com.soywiz.klogger.atomic

import kotlin.reflect.KProperty

abstract class KloggerAtomicRef<T> {
    abstract var value: T

    inline fun update(func: (T) -> T) {
        value = func(value)
    }
    operator fun setValue(receiver: Any?, prop: KProperty<*>, newValue: T) {
        value = newValue
    }
    operator fun getValue(receiver: Any?, prop: KProperty<*>): T {
        return value
    }
}

expect fun <T> kloggerAtomicRef(initial: T): KloggerAtomicRef<T>

