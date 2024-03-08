package korlibs.io.lang

import kotlin.reflect.KProperty

class threadLocal<T>(val gen: () -> T) {
	val local = object : NativeThreadLocal<T>() {
		override fun initialValue(): T = gen()
	}

    var value: T
        get() = getValue(null, null)
        set(value) { setValue(null, null, value) }

    inline operator fun getValue(thisRef: Any?, property: KProperty<*>?): T = local.get()
	inline operator fun setValue(thisRef: Any?, property: KProperty<*>?, value: T): Unit = local.set(value)
}

expect abstract class NativeThreadLocal<T>() {
	abstract fun initialValue(): T
	fun get(): T
	fun set(value: T)
}
