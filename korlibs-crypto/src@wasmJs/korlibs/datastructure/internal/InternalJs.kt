package korlibs.datastructure.internal

//@JsFun("""(obj) => {
//    if (window.IDENTITY_HASH_CODE_SYMBOL === undefined) {
//        window.IDENTITY_HASH_CODE_SYMBOL = Symbol("KotlinIdentityHashCode");
//        window.lastIdentityHashCodeId = 0;
//    }
//    if (obj == null) return 0;
//    if (obj[window.IDENTITY_HASH_CODE_SYMBOL] === undefined) {
//        obj[window.IDENTITY_HASH_CODE_SYMBOL] = (window.lastIdentityHashCodeId = window.lastIdentityHashCodeId + 1 | 0);
//    }
//    return obj[window.IDENTITY_HASH_CODE_SYMBOL];
//}""")
//internal external fun anyIdentityHashCodeJsRef(ref: JsReference<Any>): Int
//
//internal actual fun anyIdentityHashCode(obj: Any?): Int {
//    if (obj == null) return 0
//    return anyIdentityHashCodeJsRef(obj.toJsReference())
//    //println("anyIdentityHashCode not implemented in WASM!")
//    //return -1
//}

private val anyIdentityHashCodeOnce by lazy {
    //console.error("!!!!! anyIdentityHashCode not implemented in WASM!")
    println("!!!!! anyIdentityHashCode not implemented in WASM!")
    Unit
}

internal actual fun anyIdentityHashCode(obj: Any?): Int {
    if (obj == null) return 0
    anyIdentityHashCodeOnce
    return -1
}
