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
//internal actual external fun anyIdentityHashCode(obj: Any?): Int

internal actual fun anyIdentityHashCode(obj: Any?): Int {
    println("anyIdentityHashCode not implemented in WASM!")
    return 0
}
