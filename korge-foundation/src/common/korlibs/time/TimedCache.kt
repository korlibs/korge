package korlibs.time

import kotlin.reflect.*

class TimedCache<T : Any>(var validTime: TimeSpan, val timeProvider: TimeProvider = TimeProvider, val gen: () -> T) {
    private var cachedTime: DateTime = DateTime.EPOCH
    private lateinit var _value: T

    var value: T
        get() = getValue(Unit, null)
        set(value) { setValue(Unit, null, value) }

    operator fun getValue(obj: Any, prop: KProperty<*>?): T {
        val now = timeProvider.now()
        if (cachedTime == DateTime.EPOCH || (now - cachedTime) >= validTime) {
            cachedTime = now
            this._value = gen()
        }
        return _value
    }

    operator fun setValue(obj: Any, prop: KProperty<*>?, value: T) {
        this._value = value
    }
}

class IntTimedCache(val ttl: TimeSpan, val timeProvider: TimeProvider = TimeProvider, val gen: () -> Int) {
    @PublishedApi internal var last = DateTime.EPOCH
    @PublishedApi internal var _value: Int = 0

    val value: Int get() = get()

    inline fun get(): Int {
        val now = timeProvider.now()
        if (now - last > ttl) {
            last = now
            _value = gen()
        }
        return _value
    }

    operator fun getValue(obj: Any?, property: KProperty<*>): Int = get()
}
