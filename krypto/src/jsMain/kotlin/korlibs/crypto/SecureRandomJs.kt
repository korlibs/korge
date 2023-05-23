package korlibs.crypto

import org.khronos.webgl.Int8Array
import org.khronos.webgl.Uint8Array

private val isNodeJs by lazy { js("(typeof process === 'object' && typeof require === 'function')").unsafeCast<Boolean>() }
//private external val require: dynamic
//private val require_req: dynamic by lazy { require }
//private fun require_node(name: String): dynamic = require_req(name)
private val _global: dynamic = js("((typeof global !== 'undefined') ? global : self)")

// DIRTY HACK to prevent webpack to mess with our code
val REQ get() = "req"
private external val eval: dynamic
private fun require_node(name: String): dynamic = eval("(${REQ}uire('$name'))")

actual fun fillRandomBytes(array: ByteArray) {
    if (isNodeJs) {
        // https://nodejs.org/api/crypto.html#cryptorandomfillsyncbuffer-offset-size
        require_node("crypto").randomFillSync(Uint8Array(array.unsafeCast<Int8Array>().buffer))
    } else {
        // https://developer.mozilla.org/en-US/docs/Web/API/Crypto/getRandomValues
        _global.crypto.getRandomValues(array)
    }
}

actual fun seedExtraRandomBytes(array: ByteArray) {
    seedExtraRandomBytesDefault(array)
}
