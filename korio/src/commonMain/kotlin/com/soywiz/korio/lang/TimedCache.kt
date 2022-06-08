package com.soywiz.korio.lang

import com.soywiz.klock.DateTime
import com.soywiz.klock.TimeSpan
import kotlin.reflect.KProperty

class TimedCache<T : Any>(var validTime: TimeSpan, val gen: () -> T) {
    private var cachedTime: DateTime = DateTime.EPOCH
    private lateinit var value: T

    operator fun getValue(obj: Any, prop: KProperty<*>): T {
        val now = DateTime.now()
        if (cachedTime == DateTime.EPOCH || (now - cachedTime) >= validTime) {
            cachedTime = now
            this.value = gen()
        }
        return value
    }

    operator fun setValue(obj: Any, prop: KProperty<*>, value: T) {
        this.value = value
    }
}
