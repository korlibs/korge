package korlibs.bignumber

internal actual val InternalCryptoBigIntNativeFactory: BigIntConstructor = object : BigIntConstructor {
    override fun create(value: Int): BigInt =
        if (supportNativeJsBigInt) InternalCryptoJsBigInt.create(value) else InternalCryptoCommonBigInt.create(value)

    override fun create(value: String, radix: Int): BigInt =
        if (supportNativeJsBigInt) InternalCryptoJsBigInt.create(value, radix) else InternalCryptoCommonBigInt.create(value, radix)

}

internal class InternalCryptoJsBigInt internal constructor(private val value: NativeJsBig) : BigInt, BigIntConstructor by InternalCryptoJsBigInt {
    companion object : BigIntConstructor {
        override fun create(value: Int): BigInt = InternalCryptoJsBigInt(NativeJsBigInt(value))
        override fun create(value: String, radix: Int): BigInt {
            if (value.isEmpty()) {
                throw BigIntInvalidFormatException("Zero length BigInteger")
            }
            if (radix == 10) {
                validateRadix(value, radix)
                return InternalCryptoJsBigInt(NativeJsBigInt(value))
            }
            return super.create(value, radix)
        }
        private fun validateRadix(value: String, radix: Int) {
            for (c in value) digit(c, radix)
        }
    }

    val BigInt.js: dynamic get() = (this as InternalCryptoJsBigInt).value.asDynamic()
    val Int.js: dynamic get() = NativeJsBigInt(this)

    override val signum: Int get() {
        if (js < NativeJsBigInt(0)) return -1
        if (js > NativeJsBigInt(0)) return +1
        return 0
    }

    override fun unaryMinus(): BigInt = InternalCryptoJsBigInt(-js)

    override fun inv(): BigInt = InternalCryptoJsBigInt(NativeJsInv(js))
    override fun pow(exponent: BigInt): BigInt {
        if (exponent.isNegative) throw BigIntNegativeExponentException()
        return InternalCryptoJsBigInt(NativeJsPow(js, exponent.js))
    }
    override fun pow(exponent: Int): BigInt {
        if (exponent < 0) throw BigIntNegativeExponentException()
        return pow(InternalCryptoJsBigInt(NativeJsBigInt(exponent)))
    }

    override fun and(other: BigInt): BigInt = InternalCryptoJsBigInt(NativeJsAnd(js, other.js))
    override fun or(other: BigInt): BigInt = InternalCryptoJsBigInt(NativeJsOr(js, other.js))
    override fun xor(other: BigInt): BigInt = InternalCryptoJsBigInt(NativeJsXor(js, other.js))

    override fun shl(count: Int): BigInt = InternalCryptoJsBigInt(NativeJsShl(js, count.js))
    override fun shr(count: Int): BigInt = InternalCryptoJsBigInt(NativeJsShr(js, count.js))

    override fun plus(other: BigInt): BigInt = InternalCryptoJsBigInt(this.js + other.js)
    override fun minus(other: BigInt): BigInt = InternalCryptoJsBigInt(this.js - other.js)
    override fun times(other: BigInt): BigInt = InternalCryptoJsBigInt(this.js * other.js)
    override fun div(other: BigInt): BigInt {
        if (other.isZero) throw BigIntDivisionByZeroException()
        return InternalCryptoJsBigInt(this.js / other.js)
    }
    override fun rem(other: BigInt): BigInt = InternalCryptoJsBigInt(this.js % other.js)

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
    override fun equals(other: Any?): Boolean = other is InternalCryptoJsBigInt && this.value == other.value
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
