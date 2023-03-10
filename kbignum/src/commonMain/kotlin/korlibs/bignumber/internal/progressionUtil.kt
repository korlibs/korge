package korlibs.bignumber.internal

import korlibs.bignumber.BigInt

/**
 * @see kotlin.internal.mod
 */
internal fun mod(a: BigInt, b: BigInt): BigInt {
    val mod = a % b
    return if (mod >= BigInt.ZERO) mod else mod + b
}

/**
 * @see kotlin.internal.differenceModulo
 */
internal fun differenceModulo(a: BigInt, b: BigInt, c: BigInt): BigInt {
    return mod(mod(a, c) - mod(b, c), c)
}

/**
 * @see kotlin.internal.getProgressionLastElement
 */
internal fun getProgressionLastElement(start: BigInt, end: BigInt, step: BigInt): BigInt = when {
    step > BigInt.ZERO -> if (start >= end) end else end - differenceModulo(end, start, step)
    step < BigInt.ZERO -> if (start <= end) end else end + differenceModulo(start, end, -step)
    else -> throw IllegalArgumentException("Step is zero.")
}
