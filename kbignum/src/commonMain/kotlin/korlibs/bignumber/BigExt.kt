package korlibs.bignumber

// Big Integer
/** Converts this into a [BigInt] */
val Long.bi: BigInt get() = BigInt(this)
/** Converts this into a [BigInt] */
val Int.bi: BigInt get() = BigInt(this)
/** Converts this into a [BigInt] */
val String.bi: BigInt get() = BigInt(this)
/** Converts this into a [BigInt] using a specific [radix], that is the base to use. radix=10 for decimal, radix=16 for hexadecimal */
fun String.bi(radix: Int): BigInt = BigInt(this, radix)

// Big Number
/** Converts this into a [BigNum] */
val Double.bn: BigNum get() = BigNum("$this")
/** Converts this into a [BigNum] */
val Long.bn: BigNum get() = BigNum(this.bi, 0)
/** Converts this into a [BigNum] */
val Int.bn: BigNum get() = BigNum(this.bi, 0)
/** Converts this into a [BigNum] */
val String.bn: BigNum get() = BigNum(this)
