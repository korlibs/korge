package com.soywiz.kortest

import kotlin.math.abs

private fun checkAbsoluteTolerance(absoluteTolerance: Double) {
    require(absoluteTolerance >= 0.0) { "Illegal negative absolute tolerance <$absoluteTolerance>." }
    require(!absoluteTolerance.isNaN()) { "Illegal NaN absolute tolerance <$absoluteTolerance>." }
}

internal fun checkDoublesAreEqual(
    expected: Double,
    actual: Double,
    absoluteTolerance: Double,
    shouldFail: Boolean = false
) {
    checkAbsoluteTolerance(absoluteTolerance)
    val equal = expected.toBits() == actual.toBits() || abs(expected - actual) <= absoluteTolerance

    if (shouldFail && !equal) {
        throw AssertionError("""
            absolute difference: $absoluteTolerance
            expected:<$expected> but was:<$actual>
        """.trimIndent())
    }
}

class DoubleSubject(val actual: Double) {
    fun isEqualTo(e: Double, absoluteTolerance: Double = 0.00001) {
        checkDoublesAreEqual(e, actual, absoluteTolerance, shouldFail = true)
    }
}
