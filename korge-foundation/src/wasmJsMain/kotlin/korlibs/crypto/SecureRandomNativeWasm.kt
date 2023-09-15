package korlibs.crypto

import org.khronos.webgl.*

actual fun fillRandomBytes(array: ByteArray) {
    val temp = Int8Array(array.size)
    if (isNodeJs()) {
        _fillRandomBytesNode(temp)
    } else {
        _fillRandomBytesBrowser(temp)
    }
    for (n in 0 until array.size) array[n] = temp[n]
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
private external fun _fillRandomBytesNode(array: Int8Array)

@JsFun("""(array) => {
    (globalThis || window || this).crypto.getRandomValues(array)
}
""")
private external fun _fillRandomBytesBrowser(array: Int8Array)
