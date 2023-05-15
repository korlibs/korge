package korlibs.crypto

actual fun fillRandomBytes(array: ByteArray) {
    if (isNodeJs()) {
        _fillRandomBytesNode(array)
    } else {
        _fillRandomBytesBrowser(array)
    }
}

actual fun seedExtraRandomBytes(array: ByteArray) {
    seedExtraRandomBytesDefault(array)
}

@JsFun("() => { return (typeof process === 'object' && typeof require === 'function'); }")
private external fun isNodeJs(): Boolean

@JsFun("""(array) => {
    require_node("crypto").randomFillSync(Uint8Array(array.buffer))
}
""")
private external fun _fillRandomBytesNode(array: ByteArray)

@JsFun("""(array) => {
    (globalThis || window || this).crypto.getRandomValues(array)
}
""")
private external fun _fillRandomBytesBrowser(array: ByteArray)
