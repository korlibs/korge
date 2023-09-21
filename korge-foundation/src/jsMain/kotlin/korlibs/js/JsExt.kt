package korlibs.js

fun <T> JSIterable<T>.toArray(): Array<T> {
    return Array_from(this)
}

val Symbol_asyncIterator get() = Symbol.asyncIterator

external val Symbol: dynamic

external interface JSIterableResult<T> {
    val value: T
    val done: Boolean
}

@JsName("WeakMap")
external class JsWeakMap {
    fun has(k: dynamic): Boolean
    fun set(k: dynamic, v: dynamic): Unit
    fun get(k: dynamic): dynamic
    fun delete(k: dynamic)
}

fun Array_from(value: dynamic): Array<dynamic> = JsArray.from(value)
inline operator fun <T> JsArray<T>.get(index: Int): T = asDynamic()[index]
inline operator fun <T> JsArray<T>.set(index: Int, value: T): Unit { asDynamic()[index] = value }
fun <T> JsArray.Companion.createEmpty(): JsArray<T> = js("([])").unsafeCast<JsArray<T>>()
//external internal operator fun <T> JsArray<T>.get(index: Int): T = definedExternally
//external internal operator fun <T> JsArray<T>.set(index: Int, value: T): T = definedExternally

//@JsName("delete")
//external fun jsDelete(v: dynamic): Unit


@JsName("Map")
external class JsMap { }

@JsName("Array")
external class JsArray<T> {
    var length: Int
    //@nativeGetter operator fun get(index: Int): T = definedExternally
    //@nativeSetter operator fun set(index: Int, value: T): T = definedExternally
    fun concat(vararg arrays: JsArray<T>): JsArray<T>
    fun indexOf(e: T): Int
    fun lastIndexOf(e: T): Int
    fun splice(start: Int, deleteCount: Int, vararg items: T): JsArray<T>
    fun unshift(vararg items: T)
    fun push(vararg items: T)
    fun shift(): T
    fun pop(): T
    companion object {
        fun from(value: dynamic): Array<dynamic>
    }
}
