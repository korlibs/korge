package korlibs.datastructure.internal


private val anyIdentityHashCodeOnce by lazy {
    //console.error("!!!!! anyIdentityHashCode not implemented in WASM!")
    println("!!!!! anyIdentityHashCode not implemented in WASM!")
}

internal actual fun anyIdentityHashCode(obj: Any?): Int {
    if (obj == null) return 0
    anyIdentityHashCodeOnce
    return -1
}
