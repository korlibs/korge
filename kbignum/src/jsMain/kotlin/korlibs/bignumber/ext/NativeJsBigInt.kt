package korlibs.bignumber.ext

@JsName("BigInt")
internal external class NativeJsBig

@JsName("BigInt")
internal external fun NativeJsBigInt(value: String): NativeJsBig
@JsName("BigInt")
internal external fun NativeJsBigInt(value: Number): NativeJsBig
@JsName("parseInt")
internal external fun NativeJsParseInt(value: NativeJsBig): Int

internal fun NativeJsInv(a: dynamic): dynamic = js("(~(a))")
internal fun NativeJsShl(a: dynamic, b: dynamic): dynamic = js("((a) << (b))")
internal fun NativeJsShr(a: dynamic, b: dynamic): dynamic = js("((a) >> (b))")
internal fun NativeJsXor(a: dynamic, b: dynamic): dynamic = js("((a) ^ (b))")
internal fun NativeJsOr(a: dynamic, b: dynamic): dynamic = js("((a) | (b))")
internal fun NativeJsAnd(a: dynamic, b: dynamic): dynamic = js("((a) & (b))")
//internal fun NativeJsPow(a: dynamic, b: dynamic): dynamic = js("((a) ** (b))") // @TODO: Kotlin.JS Bug
internal val NativeJsPow: dynamic by lazy { eval("(function(a, b) { return a ** b; })") } // by lazy to prevent syntax errors on old browsers

internal val supportNativeJsBigInt = js("(((typeof globalThis) !== 'undefined') && (typeof (globalThis.BigInt)) !== 'undefined')").unsafeCast<Boolean>()
