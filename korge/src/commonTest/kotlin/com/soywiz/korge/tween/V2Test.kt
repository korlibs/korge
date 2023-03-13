package com.soywiz.korge.tween

import com.soywiz.korio.util.toStringDecimal
import com.soywiz.korma.interpolation.*
import com.soywiz.korma.math.*
import kotlin.test.*

class V2Test {
    var x: Double = 50.0

    class MyClass(var v: Double)

    @Test
    fun testReusingV2WithoutInitial() {
        val instance = MyClass(v = 10.0)
        val v2 = instance::v[20.0]
        v2.init()
        v2.set(0.0); assertEquals(10.0, instance.v)
        v2.set(0.5); assertEquals(15.0, instance.v)
        v2.set(1.0); assertEquals(20.0, instance.v)
        instance.v = 30.0
        v2.init()
        v2.set(0.0); assertEquals(30.0, instance.v)
        v2.set(0.5); assertEquals(25.0, instance.v)
        v2.set(1.0); assertEquals(20.0, instance.v)
    }

    fun V2<*>.set(ratio: Double): Unit = set(ratio.toRatio())

    @Test
    fun testReusingIncrementalV2() {
        val instance = MyClass(v = 10.0)
        val v2 = instance::v.incr(10.0)
        v2.init()
        v2.set(0.0); assertEquals(10.0, instance.v)
        v2.set(0.5); assertEquals(15.0, instance.v)
        v2.set(1.0); assertEquals(20.0, instance.v)
        instance.v = 30.0
        v2.init()
        v2.set(0.0); assertEquals(30.0, instance.v)
        v2.set(0.5); assertEquals(35.0, instance.v)
        v2.set(1.0); assertEquals(40.0, instance.v)
    }

    @Test
    fun testEasingList() {
        assertEquals(
            "smooth,ease-in-elastic,ease-out-elastic,ease-out-bounce,linear,ease,ease-in,ease-out,ease-in-out,ease-in-old,ease-out-old,ease-in-out-old,ease-out-in-old,ease-in-back,ease-out-back,ease-in-out-back,ease-out-in-back,ease-in-out-elastic,ease-out-in-elastic,ease-in-bounce,ease-in-out-bounce,ease-out-in-bounce,ease-in-quad,ease-out-quad,ease-in-out-quad,ease-sine,ease-clamp-start,ease-clamp-end,ease-clamp-middle",
            Easing.ALL_LIST.joinToString(",")
        )
    }

    @Test fun testSMOOTH() = testEasing(Easing.SMOOTH, listOf(100.00, 102.80, 110.40, 121.60, 135.20, 150.00, 164.80, 178.40, 189.60, 197.20, 200.00))
    @Test fun testEASE_IN_ELASTIC() = testEasing(Easing.EASE_IN_ELASTIC, listOf(100.00, 100.20, 99.80, 99.61, 101.56, 98.44, 96.88, 112.50, 87.50, 75.00, 200.00))
    @Test fun testEASE_OUT_ELASTIC() = testEasing(Easing.EASE_OUT_ELASTIC, listOf(100.00, 225.00, 212.50, 187.50, 203.12, 201.56, 198.44, 200.39, 200.20, 199.80, 200.00))
    @Test fun testEASE_OUT_BOUNCE() = testEasing(Easing.EASE_OUT_BOUNCE, listOf(100.00, 107.56, 130.25, 168.06, 191.00, 176.56, 177.25, 193.06, 194.00, 198.81, 200.00))
    @Test fun testLINEAR() = testEasing(Easing.LINEAR, listOf(100.00, 110.00, 120.00, 130.00, 140.00, 150.00, 160.00, 170.00, 180.00, 190.00, 200.00))
    @Test fun testEASE() = testEasing(Easing.EASE, listOf(100.00, 109.57, 129.68, 151.35, 168.20, 180.20, 188.57, 194.08, 197.55, 199.42, 200.00))
    @Test fun testEASE_IN() = testEasing(Easing.EASE_IN, listOf(100.00, 101.70, 106.20, 112.91, 121.40, 131.64, 142.87, 155.38, 169.24, 183.84, 200.00))
    @Test fun testEASE_OUT() = testEasing(Easing.EASE_OUT, listOf(100.00, 116.16, 130.76, 144.62, 157.13, 168.36, 178.60, 187.09, 193.80, 198.30, 200.00))
    @Test fun testEASE_IN_OUT() = testEasing(Easing.EASE_IN_OUT, listOf(100.00, 101.94, 108.12, 118.72, 133.19, 150.00, 166.81, 181.28, 191.88, 198.06, 200.00))
    @Test fun testEASE_IN_OLD() = testEasing(Easing.EASE_IN_OLD, listOf(100.00, 100.10, 100.80, 102.70, 106.40, 112.50, 121.60, 134.30, 151.20, 172.90, 200.00))
    @Test fun testEASE_OUT_OLD() = testEasing(Easing.EASE_OUT_OLD, listOf(100.00, 127.10, 148.80, 165.70, 178.40, 187.50, 193.60, 197.30, 199.20, 199.90, 200.00))
    @Test fun testEASE_IN_OUT_OLD() = testEasing(Easing.EASE_IN_OUT_OLD, listOf(100.00, 100.40, 103.20, 110.80, 125.60, 150.00, 174.40, 189.20, 196.80, 199.60, 200.00))
    @Test fun testEASE_OUT_IN_OLD() = testEasing(Easing.EASE_OUT_IN_OLD, listOf(100.00, 124.40, 139.20, 146.80, 149.60, 150.00, 150.40, 153.20, 160.80, 175.60, 200.00))
    @Test fun testEASE_IN_BACK() = testEasing(Easing.EASE_IN_BACK, listOf(100.00, 98.57, 95.35, 91.98, 90.06, 91.23, 97.10, 109.29, 129.42, 159.12, 200.00))
    @Test fun testEASE_OUT_BACK() = testEasing(Easing.EASE_OUT_BACK, listOf(100.00, 140.88, 170.58, 190.71, 202.90, 208.77, 209.94, 208.02, 204.65, 201.43, 200.00))
    @Test fun testEASE_IN_OUT_BACK() = testEasing(Easing.EASE_IN_OUT_BACK, listOf(100.00, 97.68, 95.03, 98.55, 114.71, 150.00, 185.29, 201.45, 204.97, 202.32, 200.00))
    @Test fun testEASE_OUT_IN_BACK() = testEasing(Easing.EASE_OUT_IN_BACK, listOf(100.00, 135.29, 151.45, 154.97, 152.32, 150.00, 147.68, 145.03, 148.55, 164.71, 200.00))
    @Test fun testEASE_IN_OUT_ELASTIC() = testEasing(Easing.EASE_IN_OUT_ELASTIC, listOf(100.00, 99.90, 100.78, 98.44, 93.75, 150.00, 206.25, 201.56, 199.22, 200.10, 200.00))
    @Test fun testEASE_OUT_IN_ELASTIC() = testEasing(Easing.EASE_OUT_IN_ELASTIC, listOf(100.00, 156.25, 151.56, 149.22, 150.10, 150.00, 149.90, 150.78, 148.44, 143.75, 200.00))
    @Test fun testEASE_IN_BOUNCE() = testEasing(Easing.EASE_IN_BOUNCE, listOf(100.00, 101.19, 106.00, 106.94, 122.75, 123.44, 109.00, 131.94, 169.75, 192.44, 200.00))
    @Test fun testEASE_IN_OUT_BOUNCE() = testEasing(Easing.EASE_IN_OUT_BOUNCE, listOf(100.00, 103.00, 111.38, 104.50, 134.87, 150.00, 165.13, 195.50, 188.63, 197.00, 200.00))
    @Test fun testEASE_OUT_IN_BOUNCE() = testEasing(Easing.EASE_OUT_IN_BOUNCE, listOf(100.00, 115.13, 145.50, 138.62, 147.00, 150.00, 153.00, 161.37, 154.50, 184.87, 200.00))
    @Test fun testEASE_IN_QUAD() = testEasing(Easing.EASE_IN_QUAD, listOf(100.00, 101.00, 104.00, 109.00, 116.00, 125.00, 136.00, 149.00, 164.00, 181.00, 200.00))
    @Test fun testEASE_OUT_QUAD() = testEasing(Easing.EASE_OUT_QUAD, listOf(100.00, 119.00, 136.00, 151.00, 164.00, 175.00, 184.00, 191.00, 196.00, 199.00, 200.00))
    @Test fun testEASE_IN_OUT_QUAD() = testEasing(Easing.EASE_IN_OUT_QUAD, listOf(100.00, 102.00, 108.00, 118.00, 132.00, 150.00, 168.00, 182.00, 192.00, 198.00, 200.00))
    @Test fun testEASE_SINE() = testEasing(Easing.EASE_SINE, listOf(100.00, 115.64, 130.90, 145.40, 158.78, 170.71, 180.90, 189.10, 195.11, 198.77, 200.00))
    @Test fun testEASE_CLAMP_START() = testEasing(Easing.EASE_CLAMP_START, listOf(100.00, 200.00, 200.00, 200.00, 200.00, 200.00, 200.00, 200.00, 200.00, 200.00, 200.00))
    @Test fun testEASE_CLAMP_END() = testEasing(Easing.EASE_CLAMP_END, listOf(100.00, 100.00, 100.00, 100.00, 100.00, 100.00, 100.00, 100.00, 100.00, 100.00, 200.00))
    @Test fun testEASE_CLAMP_MIDDLE() = testEasing(Easing.EASE_CLAMP_MIDDLE, listOf(100.00, 100.00, 100.00, 100.00, 100.00, 200.00, 200.00, 200.00, 200.00, 200.00, 200.00))

    @Test fun testEASE_OUT_ELASTIC_Clamped() = testEasingClamped(Easing.EASE_OUT_ELASTIC, listOf(100.00, 200.00, 200.00, 187.50, 200.00, 200.00, 198.44, 200.00, 200.00, 199.80, 200.00))
    @Test fun testEASE_IN_OUT_BACK_Clamped() = testEasingClamped(Easing.EASE_IN_OUT_BACK, listOf(100.00, 100.00, 100.00, 100.00, 114.71, 150.00, 185.29, 200.00, 200.00, 200.00, 200.00))
    @Test fun testEASE_IN_ELASTIC_Clamped() = testEasingClamped(Easing.EASE_IN_ELASTIC, listOf(100.00, 100.20, 100.00, 100.00, 101.56, 100.00, 100.00, 112.50, 100.00, 100.00, 200.00))
    @Test fun testEASE_IN_OUT_ELASTIC_Clamped() = testEasingClamped(Easing.EASE_IN_OUT_ELASTIC, listOf(100.00, 100.00, 100.78, 100.00, 100.00, 150.00, 200.00, 200.00, 199.22, 200.00, 200.00))

    private fun _testEasing(easing: Easing, expected: List<Double>, clamped: Boolean) {
        val v2 = this::x[100.0, 200.0].let { if (clamped) it.clamped() else it }.easing(easing)
        val samples = v2.samples()
        val epsilon = 1.0
        assertTrue("Actual: ${samples.map { it.toStringDecimal(2, false) }}\nExpected: $expected") { samples.zip(expected).all { it.first.isAlmostEquals(it.second, epsilon) } }
    }
    private fun testEasing(easing: Easing, expected: List<Double>) = _testEasing(easing, expected, clamped = false)
    private fun testEasingClamped(easing: Easing, expected: List<Double>) = _testEasing(easing, expected, clamped = true)

    private fun V2<Double>.samples(): List<Double> {
        val v2 = this
        return (0..10).map {
            val ratio = it.toDouble() / 10.0
            v2.set(ratio)
            v2.key.get()
        }
    }
}
