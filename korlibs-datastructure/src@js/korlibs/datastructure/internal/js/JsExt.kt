
@JsName("WeakMap")
external class JsWeakMap {
    fun has(k: dynamic): Boolean
    fun set(k: dynamic, v: dynamic)
    fun get(k: dynamic): dynamic
    fun delete(k: dynamic)
}

internal fun Array_from(value: dynamic): Array<dynamic> = JsArray.from(value)
inline operator fun <T> JsArray<T>.get(index: Int): T = asDynamic()[index]
inline operator fun <T> JsArray<T>.set(index: Int, value: T) {
    asDynamic()[index] = value
}
@JsName("Map")
external class JsMap

@JsName("Array")
external class JsArray<T> {
    var length: Int
    //@nativeGetter operator fun get(index: Int): T = definedExternally
    //@nativeSetter operator fun set(index: Int, value: T): T = definedExternally
    fun concat(vararg arrays: JsArray<T>): JsArray<T>
    fun indexOf(e: T): Int
    fun lastIndexOf(e: T): Int
    fun splice(start: Int, deleteCount: Int, vararg items: T): JsArray<T>
    fun push(vararg items: T)
    fun shift(): T
    fun pop(): T

    companion object {
        fun from(value: dynamic): Array<dynamic>
    }
}
