package korlibs.bignumber

actual val BigIntNativeFactory: BigIntConstructor = object : BigIntConstructor {
    override fun create(value: Int): BigInt =
        if (supportNativeJsBigInt) JsBigInt.create(value) else CommonBigInt.create(value)

    override fun create(value: String, radix: Int): BigInt =
        if (supportNativeJsBigInt) JsBigInt.create(value, radix) else CommonBigInt.create(value, radix)

}

class JsBigInt internal constructor(private val value: NativeJsBig) : BigInt, BigIntConstructor by JsBigInt {
    companion object : BigIntConstructor {
        override fun create(value: Int): BigInt = JsBigInt(NativeJsBigInt(value))
        override fun create(value: String, radix: Int): BigInt {
            if (value.isEmpty()) {
                throw BigIntInvalidFormatException("Zero length BigInteger")
            }
            if (radix == 10) {
                validateRadix(value, radix)
                return JsBigInt(NativeJsBigInt(value))
            }
            return super.create(value, radix)
        }
        private fun validateRadix(value: String, radix: Int) {
            for (c in value) digit(c, radix)
        }
    }

    val BigInt.js: dynamic get() = (this as JsBigInt).value.asDynamic()
    val Int.js: dynamic get() = NativeJsBigInt(this)

    override val signum: Int get() {
        if (js < NativeJsBigInt(0)) return -1
        if (js > NativeJsBigInt(0)) return +1
        return 0
    }

    override fun unaryMinus(): BigInt = JsBigInt(-js)

    override fun inv(): BigInt = JsBigInt(NativeJsInv(js))
    override fun pow(exponent: BigInt): BigInt {
        if (exponent.isNegative) throw BigIntNegativeExponentException()
        return JsBigInt(NativeJsPow(js, exponent.js))
    }
    override fun pow(exponent: Int): BigInt {
        if (exponent < 0) throw BigIntNegativeExponentException()
        return pow(JsBigInt(NativeJsBigInt(exponent)))
    }

    override fun and(other: BigInt): BigInt = JsBigInt(NativeJsAnd(js, other.js))
    override fun or(other: BigInt): BigInt = JsBigInt(NativeJsOr(js, other.js))
    override fun xor(other: BigInt): BigInt = JsBigInt(NativeJsXor(js, other.js))

    override fun shl(count: Int): BigInt = JsBigInt(NativeJsShl(js, count.js))
    override fun shr(count: Int): BigInt = JsBigInt(NativeJsShr(js, count.js))

    override fun plus(other: BigInt): BigInt = JsBigInt(this.js + other.js)
    override fun minus(other: BigInt): BigInt = JsBigInt(this.js - other.js)
    override fun times(other: BigInt): BigInt = JsBigInt(this.js * other.js)
    override fun div(other: BigInt): BigInt {
        if (other.isZero) throw BigIntDivisionByZeroException()
        return JsBigInt(this.js / other.js)
    }
    override fun rem(other: BigInt): BigInt = JsBigInt(this.js % other.js)

    private val cachedToString by lazy { value.toString() }

    override fun toInt(): Int = NativeJsParseInt(value)
    override fun toString(radix: Int): String = js.toString(radix)
    override fun toString(): String = cachedToString

    override fun compareTo(other: BigInt): Int = when {
        this.js < other.js -> -1
        this.js > other.js -> +1
        else -> 0
    }

    override fun hashCode(): Int = cachedToString.hashCode()
    override fun equals(other: Any?): Boolean = other is JsBigInt && this.value == other.value
}

@JsName("BigInt")
internal external class NativeJsBig

@JsName("BigInt")
private external fun NativeJsBigInt(value: String): NativeJsBig
@JsName("BigInt")
private external fun NativeJsBigInt(value: Number): NativeJsBig
@JsName("parseInt")
private external fun NativeJsParseInt(value: NativeJsBig): Int

private fun NativeJsInv(a: dynamic): dynamic = js("(~(a))")
private fun NativeJsShl(a: dynamic, b: dynamic): dynamic = js("((a) << (b))")
private fun NativeJsShr(a: dynamic, b: dynamic): dynamic = js("((a) >> (b))")
private fun NativeJsXor(a: dynamic, b: dynamic): dynamic = js("((a) ^ (b))")
private fun NativeJsOr(a: dynamic, b: dynamic): dynamic = js("((a) | (b))")
private fun NativeJsAnd(a: dynamic, b: dynamic): dynamic = js("((a) & (b))")
//private fun NativeJsPow(a: dynamic, b: dynamic): dynamic = js("((a) ** (b))") // @TODO: Kotlin.JS Bug
private val NativeJsPow: dynamic by lazy { eval("(function(a, b) { return a ** b; })") } // by lazy to prevent syntax errors on old browsers

private val supportNativeJsBigInt = js("(((typeof globalThis) !== 'undefined') && (typeof (globalThis.BigInt)) !== 'undefined')").unsafeCast<Boolean>()
