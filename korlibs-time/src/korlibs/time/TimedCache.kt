package korlibs.time

import kotlin.reflect.*

class TimedCache<T : Any>(var ttl: TimeSpan, val timeProvider: TimeProvider = TimeProvider, val gen: () -> T) {
    private var cachedTime: DateTime = DateTime.EPOCH
    private lateinit var _value: T

    var value: T
        get() = getValue(Unit, null)
        set(value) { setValue(Unit, null, value) }

    operator fun getValue(obj: Any, prop: KProperty<*>?): T {
        val now = timeProvider.now()
        if (cachedTime == DateTime.EPOCH || (now - cachedTime) >= ttl) {
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
    @PublishedApi internal var cachedTime = DateTime.EPOCH
    @PublishedApi internal var _value: Int = 0

    var value: Int
        get() = get()
        set(value) { _value = value }

    inline fun get(): Int {
        val now = timeProvider.now()
        if (cachedTime == DateTime.EPOCH || (now - cachedTime >= ttl)) {
            cachedTime = now
            _value = gen()
        }
        return _value
    }

    operator fun getValue(obj: Any?, property: KProperty<*>): Int = get()
}
