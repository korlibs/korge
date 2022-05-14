package com.soywiz.korgw.internal

import com.soywiz.klock.DateTime
import com.soywiz.klock.TimeSpan
import kotlin.reflect.KProperty

// @TODO: Move to Klock?

internal class TimedCache<T : Any>(val ttl: TimeSpan, val gen: () -> T) {
    var last = DateTime.EPOCH
    lateinit var value: T

    inline fun get(): T {
        val now = DateTime.now()
        if (now - last > ttl) {
            last = now
            value = gen()
        }
        return value
    }

    operator fun getValue(obj: Any?, property: KProperty<*>): T = get()
}

internal class IntTimedCache(val ttl: TimeSpan, val gen: () -> Int) {
    var last = DateTime.EPOCH
    var value: Int = 0

    inline fun get(): Int {
        val now = DateTime.now()
        if (now - last > ttl) {
            last = now
            value = gen()
        }
        return value
    }

    operator fun getValue(obj: Any?, property: KProperty<*>): Int = get()
}
