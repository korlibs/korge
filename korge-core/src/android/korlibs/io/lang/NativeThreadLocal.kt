package korlibs.io.lang

actual abstract class NativeThreadLocal<T> {
	actual abstract fun initialValue(): T

	private val jthreadLocal = object : java.lang.ThreadLocal<T>() {
		override fun initialValue(): T = this@NativeThreadLocal.initialValue()
	}

	actual fun get(): T = jthreadLocal.get()
	actual fun set(value: T) = jthreadLocal.set(value)
}
