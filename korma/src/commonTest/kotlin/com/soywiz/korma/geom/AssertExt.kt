package com.soywiz.korma.geom

import kotlin.math.*
import kotlin.test.*

fun assertEquals(
    expected: IPoint?,
    actual: IPoint?,
    absoluteTolerance: Double = 0.0001,
    message: String = ""
) {
    if (expected == null || actual == null) {
        assertSame(
            expected, actual,
            "$expected != $actual with absolute tolerance <$absoluteTolerance>"
        )
    } else {
        assertTrue(
            (expected.x - actual.x).absoluteValue <= absoluteTolerance &&
                (expected.y - actual.y).absoluteValue <= absoluteTolerance,
            (
                "$message Expected <$expected> " +
                    "with absolute tolerance <$absoluteTolerance>, actual <$actual>."
                ).trim()
        )
    }
}
