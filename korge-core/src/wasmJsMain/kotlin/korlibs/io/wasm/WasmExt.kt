package korlibs.io.wasm

fun <T, R : JsAny?> List<T>.mapToJsArray(key: (T) -> R): JsArray<R> {
    return jsArrayOf(*this.map { key(it) }.toTypedArray())
}

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
    return out as Array<T>
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

@JsFun("(obj, key) => { return obj[key] !== undefined; }")
internal external fun JsAny_has(obj: JsAny, key: JsAny?): Boolean

@JsFun("(obj, key, params) => { return obj[key].apply(obj, params); }")
internal external fun JsAny_invokeApply(obj: JsAny, key: JsAny?, params: JsArray<JsAny?>): JsAny?

//inline class JsDynamic(val value: JsAny?) {
class JsDynamic(val value: JsAny?) {
    inline fun <T : JsAny> unsafeCast(): T? = value?.unsafeCast<T>()

    override fun toString(): String = value.toString()
    fun toInt(default: Int = 0): Int = unsafeCast<JsNumber>()?.toInt() ?: default
    fun toFloat(default: Float = 0f): Float = unsafeCast<JsNumber>()?.toDouble()?.toFloat() ?: default
    fun toDouble(default: Double = 0.0): Double = unsafeCast<JsNumber>()?.toDouble() ?: default
    fun toLong(default: Long = 0L): Long = unsafeCast<JsNumber>()?.toLong() ?: default

    operator fun contains(key: Int): Boolean = this.value?.hasAny(key) == true
    operator fun contains(key: String): Boolean = this.value?.hasAny(key) == true
    operator fun contains(key: JsAny?): Boolean = this.value?.hasAny(key) == true
    operator fun contains(key: JsDynamic): Boolean = this.value?.hasAny(key.value) == true

    operator fun get(key: Int): JsDynamic = JsDynamic(this.value?.getAny(key))
    operator fun get(key: String): JsDynamic = JsDynamic(this.value?.getAny(key))
    operator fun get(key: JsAny?): JsDynamic = JsDynamic(this.value?.getAny(key))
    operator fun get(key: JsDynamic): JsDynamic = this[key.value]

    operator fun set(key: Int, value: JsAny?) { this.value?.setAny(key, value) }
    operator fun set(key: String, value: JsAny?) { this.value?.setAny(key, value) }
    operator fun set(key: JsAny?, value: JsAny?) { this.value?.setAny(key, value) }
    operator fun set(key: JsDynamic, value: JsAny?) { this.value?.setAny(key.value, value) }

    operator fun set(key: Int, value: JsDynamic) { this.value?.setAny(key, value.value) }
    operator fun set(key: String, value: JsDynamic) { this.value?.setAny(key, value.value) }
    operator fun set(key: JsAny?, value: JsDynamic) { this.value?.setAny(key, value.value) }
    operator fun set(key: JsDynamic, value: JsDynamic) { this.value?.setAny(key.value, value.value) }
}

val JsAny?.jsDyn: JsDynamic get() = JsDynamic(this)

fun JsAny.getAny(key: Int): JsAny? = JsAny_get(this, key)
fun JsAny.getAny(key: String): JsAny? = JsAny_get(this, key)
fun JsAny.getAny(key: JsAny?): JsAny? = JsAny_get(this, key)

fun JsAny.setAny(key: Int, value: JsAny?) = setAny(key.toJsNumber(), value)
fun JsAny.setAny(key: String, value: JsAny?) = setAny(key.toJsString(), value)
fun JsAny.setAny(key: JsAny?, value: JsAny?) = JsAny_set(this, key, value)

fun JsAny.hasAny(key: Int): Boolean = JsAny_has(this, key.toJsNumber())
// @TODO: Not working!
@Deprecated("Not working!")
fun JsAny.hasAny(key: String): Boolean = JsAny_has(this, key.toJsString())
fun JsAny.hasAny(key: JsString): Boolean = JsAny_has(this, key)
fun JsAny.hasAny(key: JsAny?): Boolean = JsAny_has(this, key)

fun JsAny.dynamicInvoke(key: JsString, params: JsArray<JsAny?>): JsAny? = JsAny_invokeApply(this, key, params)

@JsFun("() => { return {}; }")
external fun jsEmptyObj(): JsAny
@JsFun("() => { return []; }")
external fun <T : JsAny?> jsEmptyArray(): JsArray<T>
@JsFun("(obj) => { return (Object.keys(obj)); }")
external fun jsObjectKeys(obj: JsAny?): JsArray<JsString>

fun <T : JsAny?> JsArray<T>.push(value: T) { JsArray_push(this, value) }
