package korlibs.io.util

import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty0

class RedirectField<V>(val redirect: KProperty0<V>) {
	inline operator fun getValue(thisRef: Any?, property: KProperty<*>): V = redirect.get()
}

class RedirectMutableField<V>(val redirect: KMutableProperty0<V>) {
	inline operator fun getValue(thisRef: Any?, property: KProperty<*>): V = redirect.get()
	inline operator fun setValue(thisRef: Any?, property: KProperty<*>, value: V) = redirect.set(value)
}

class RedirectMutableFieldGen<V>(val redirect: () -> KMutableProperty0<V>) {
	inline operator fun getValue(thisRef: Any?, property: KProperty<*>): V = redirect().get()
	inline operator fun setValue(thisRef: Any?, property: KProperty<*>, value: V) = redirect().set(value)
}

class RedirectFieldGen<V>(val redirect: () -> KProperty0<V>) {
	inline operator fun getValue(thisRef: Any?, property: KProperty<*>): V = redirect().get()
}

inline fun <V> (() -> KProperty0<V>).redirected() = RedirectFieldGen(this)
inline fun <V> (() -> KMutableProperty0<V>).redirected() = RedirectMutableFieldGen(this)

class TransformedField<V, R>(val prop: KProperty0<V>, val transform: (V) -> R) {
	inline operator fun getValue(thisRef: Any?, property: KProperty<*>): R = transform(prop.get())
}

class TransformedMutableField<V, R>(val prop: KMutableProperty0<V>, val transform: (V) -> R, val reverseTransform: (R) -> V) {
	inline operator fun getValue(thisRef: Any?, property: KProperty<*>): R = transform(prop.get())
	inline operator fun setValue(thisRef: Any?, property: KProperty<*>, value: R) = prop.set(reverseTransform(value))
}

fun <V, R> KMutableProperty0<V>.transformed(transform: (V) -> R, reverseTransform: (R) -> V) = TransformedMutableField(this, transform, reverseTransform)
fun <V, R> KProperty0<V>.transformed(transform: (V) -> R) = TransformedField<V, R>(this, transform)
