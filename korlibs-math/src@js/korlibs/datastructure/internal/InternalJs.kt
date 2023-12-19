package korlibs.datastructure.internal

@JsName("Symbol")
external fun Symbol(name: String): dynamic

private var lastIdentityHashCodeId = 0
private val IDENTITY_HASH_CODE_SYMBOL = Symbol("KotlinIdentityHashCode")

// @TODO: Note: still might collide if we create more than 2**32 objects.
// @TODO: We could use a WeakMap and some tricks but that would be slow,
// @TODO: and the purpose of this is to serve SlowIdentityHashMap and
// @TODO: to identify/differentiate objects easily for debugging purposes.
internal actual fun anyIdentityHashCode(obj: Any?): Int {
    if (obj == null) return 0
    val dyn = obj.asDynamic()
    if (dyn[IDENTITY_HASH_CODE_SYMBOL] === undefined) {
        dyn[IDENTITY_HASH_CODE_SYMBOL] = lastIdentityHashCodeId++

    }
    return dyn[IDENTITY_HASH_CODE_SYMBOL].unsafeCast<Int>()
}
