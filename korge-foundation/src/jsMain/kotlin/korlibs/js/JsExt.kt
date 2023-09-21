package korlibs.js

import korlibs.datastructure.Array_from

fun <T> JSIterable<T>.toArray(): Array<T> {
    return Array_from(this)
}

val Symbol_asyncIterator get() = Symbol.asyncIterator

external val Symbol: dynamic

external interface JSIterableResult<T> {
    val value: T
    val done: Boolean
}
