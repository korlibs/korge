package korlibs.io.wasm

import korlibs.memory.internal.*

fun <T : JsAny?> jsArrayOf(vararg values: T): JsArray<T> {
    val array = JsArray<T>()
    //array.length = values.size
    for (n in 0 until values.size) array[n] = values[n]
    return array
}

fun <T : JsAny?> JsArray<T>.toList(): List<T> {
    val out = ArrayList<T>(length)
    for (n in 0 until length) out.add(this[n]!!)
    return out
}

inline fun <reified T : JsAny?> JsArray<T>.toTypedArray(): Array<T> {
    val out = Array<T?>(length) { null }
    for (n in 0 until length) out[n] = (this[n]!!)
    return out.unsafeCast()
}

fun JsNumber.toLong(): Long = this.toDouble().toLong()

@JsFun("(obj, key) => { return obj[key]; }")
internal external fun JsAny_get(obj: JsAny, key: JsAny?): JsAny?

@JsFun("(obj, key) => { return obj[key]; }")
internal external fun JsAny_get(obj: JsAny, key: Int): JsAny?

@JsFun("(obj, key) => { return obj[key]; }")
internal external fun JsAny_get(obj: JsAny, key: String): JsAny?

@JsFun("(obj, key, value) => { obj[key] = value; }")
internal external fun JsAny_set(obj: JsAny, key: JsAny?, value: JsAny?)

@JsFun("(obj, value) => { obj.push(value); }")
internal external fun JsArray_push(obj: JsAny, value: JsAny?)

fun JsAny.getAny(key: Int): JsAny? = JsAny_get(this, key)
fun JsAny.getAny(key: String): JsAny? = JsAny_get(this, key)
fun JsAny.getAny(key: JsAny?): JsAny? = JsAny_get(this, key)
fun JsAny.setAny(key: JsAny?, value: JsAny?) {
    JsAny_set(this, key, value)
}

@JsFun("() => { return {}; }")
external fun jsEmptyObj(): JsAny
@JsFun("() => { return []; }")
external fun <T : JsAny?> jsEmptyArray(): JsArray<T>
@JsFun("(obj) => { return (Object.keys(obj)); }")
external fun jsObjectKeys(obj: JsAny?): JsArray<JsString>

fun <T : JsAny?> JsArray<T>.push(value: T) { JsArray_push(this, value) }
