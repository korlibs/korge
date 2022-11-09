package com.soywiz.korge.tween

import com.soywiz.korio.util.toStringDecimal
import com.soywiz.korma.interpolation.Easing
import kotlin.test.Test
import kotlin.test.assertEquals

class V2Test {
    var x: Double = 50.0

    class MyClass(var v: Double)

    @Test
    fun testReusingV2WithoutInitial() {
        val instance = MyClass(v = 10.0)
        val v2 = instance::v[20.0]
        v2.init()
        v2.set(0.0)
        assertEquals(10.0, instance.v)
        v2.set(0.5)
        assertEquals(15.0, instance.v)
        v2.set(1.0)
        assertEquals(20.0, instance.v)
        instance.v = 30.0
        v2.init()
        v2.set(0.0)
        assertEquals(30.0, instance.v)
        v2.set(0.5)
        assertEquals(25.0, instance.v)
        v2.set(1.0)
        assertEquals(20.0, instance.v)
    }

    @Test
    fun test() {
        val out = arrayListOf<String>()
        for (clamped in listOf(false, true)) {
            for (easing in Easing.ALL_LIST) {
                val v2 = this::x[100.0, 200.0].let { if (clamped) it.clamped() else it }.easing(easing)
                out.add("$easing[clamped=$clamped] : ${v2.samplesString()}")
            }
        }
        assertEquals(
            """
                smooth[clamped=false] : [100, 102.8, 110.4, 121.6, 135.2, 150, 164.8, 178.4, 189.6, 197.2, 200]
                ease-in-elastic[clamped=false] : [100, 100.195, 99.805, 99.609, 101.562, 98.438, 96.875, 112.5, 87.5, 75, 200]
                ease-out-elastic[clamped=false] : [100, 225, 212.5, 187.5, 203.125, 201.562, 198.438, 200.391, 200.195, 199.805, 200]
                ease-out-bounce[clamped=false] : [100, 107.562, 130.25, 168.062, 191, 176.562, 177.25, 193.062, 194, 198.812, 200]
                linear[clamped=false] : [100, 110, 120, 130, 140, 150, 160, 170, 180, 190, 200]
                ease[clamped=false] : [100, 109.575, 129.678, 151.353, 168.204, 180.199, 188.565, 194.085, 197.546, 199.424, 200]
                ease-in[clamped=false] : [100, 101.702, 106.195, 112.909, 121.403, 131.641, 142.873, 155.381, 169.235, 183.844, 200]
                ease-out[clamped=false] : [100, 116.156, 130.765, 144.619, 157.127, 168.359, 178.597, 187.091, 193.805, 198.298, 200]
                ease-in-out[clamped=false] : [100, 101.944, 108.116, 118.715, 133.187, 150, 166.813, 181.285, 191.884, 198.056, 200]
                ease-in-old[clamped=false] : [100, 100.1, 100.8, 102.7, 106.4, 112.5, 121.6, 134.3, 151.2, 172.9, 200]
                ease-out-old[clamped=false] : [100, 127.1, 148.8, 165.7, 178.4, 187.5, 193.6, 197.3, 199.2, 199.9, 200]
                ease-in-out-old[clamped=false] : [100, 100.4, 103.2, 110.8, 125.6, 150, 174.4, 189.2, 196.8, 199.6, 200]
                ease-out-in-old[clamped=false] : [100, 124.4, 139.2, 146.8, 149.6, 150, 150.4, 153.2, 160.8, 175.6, 200]
                ease-in-back[clamped=false] : [100, 98.569, 95.355, 91.98, 90.065, 91.23, 97.097, 109.287, 129.42, 159.117, 200]
                ease-out-back[clamped=false] : [100, 140.883, 170.58, 190.713, 202.903, 208.77, 209.935, 208.02, 204.645, 201.431, 200]
                ease-in-out-back[clamped=false] : [100, 97.677, 95.032, 98.549, 114.71, 150, 185.29, 201.451, 204.968, 202.323, 200]
                ease-out-in-back[clamped=false] : [100, 135.29, 151.451, 154.968, 152.323, 150, 147.677, 145.032, 148.549, 164.71, 200]
                ease-in-out-elastic[clamped=false] : [100, 99.902, 100.781, 98.438, 93.75, 150, 206.25, 201.562, 199.219, 200.098, 200]
                ease-out-in-elastic[clamped=false] : [100, 156.25, 151.562, 149.219, 150.098, 150, 149.902, 150.781, 148.438, 143.75, 200]
                ease-in-bounce[clamped=false] : [100, 101.188, 106, 106.938, 122.75, 123.438, 109, 131.938, 169.75, 192.438, 200]
                ease-in-out-bounce[clamped=false] : [100, 103, 111.375, 104.5, 134.875, 150, 165.125, 195.5, 188.625, 197, 200]
                ease-out-in-bounce[clamped=false] : [100, 115.125, 145.5, 138.625, 147, 150, 153, 161.375, 154.5, 184.875, 200]
                ease-in-quad[clamped=false] : [100, 101, 104, 109, 116, 125, 136, 149, 164, 181, 200]
                ease-out-quad[clamped=false] : [100, 119, 136, 151, 164, 175, 184, 191, 196, 199, 200]
                ease-in-out-quad[clamped=false] : [100, 102, 108, 118, 132, 150, 168, 182, 192, 198, 200]
                ease-sine[clamped=false] : [100, 115.643, 130.902, 145.399, 158.779, 170.711, 180.902, 189.101, 195.106, 198.769, 200]
                ease-clamp-start[clamped=false] : [100, 200, 200, 200, 200, 200, 200, 200, 200, 200, 200]
                ease-clamp-end[clamped=false] : [100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 200]
                ease-clamp-middle[clamped=false] : [100, 100, 100, 100, 100, 200, 200, 200, 200, 200, 200]
                smooth[clamped=true] : [100, 102.8, 110.4, 121.6, 135.2, 150, 164.8, 178.4, 189.6, 197.2, 200]
                ease-in-elastic[clamped=true] : [100, 100.195, 100, 100, 101.562, 100, 100, 112.5, 100, 100, 200]
                ease-out-elastic[clamped=true] : [100, 200, 200, 187.5, 200, 200, 198.438, 200, 200, 199.805, 200]
                ease-out-bounce[clamped=true] : [100, 107.562, 130.25, 168.062, 191, 176.562, 177.25, 193.062, 194, 198.812, 200]
                linear[clamped=true] : [100, 110, 120, 130, 140, 150, 160, 170, 180, 190, 200]
                ease[clamped=true] : [100, 109.575, 129.678, 151.353, 168.204, 180.199, 188.565, 194.085, 197.546, 199.424, 200]
                ease-in[clamped=true] : [100, 101.702, 106.195, 112.909, 121.403, 131.641, 142.873, 155.381, 169.235, 183.844, 200]
                ease-out[clamped=true] : [100, 116.156, 130.765, 144.619, 157.127, 168.359, 178.597, 187.091, 193.805, 198.298, 200]
                ease-in-out[clamped=true] : [100, 101.944, 108.116, 118.715, 133.187, 150, 166.813, 181.285, 191.884, 198.056, 200]
                ease-in-old[clamped=true] : [100, 100.1, 100.8, 102.7, 106.4, 112.5, 121.6, 134.3, 151.2, 172.9, 200]
                ease-out-old[clamped=true] : [100, 127.1, 148.8, 165.7, 178.4, 187.5, 193.6, 197.3, 199.2, 199.9, 200]
                ease-in-out-old[clamped=true] : [100, 100.4, 103.2, 110.8, 125.6, 150, 174.4, 189.2, 196.8, 199.6, 200]
                ease-out-in-old[clamped=true] : [100, 124.4, 139.2, 146.8, 149.6, 150, 150.4, 153.2, 160.8, 175.6, 200]
                ease-in-back[clamped=true] : [100, 100, 100, 100, 100, 100, 100, 109.287, 129.42, 159.117, 200]
                ease-out-back[clamped=true] : [100, 140.883, 170.58, 190.713, 200, 200, 200, 200, 200, 200, 200]
                ease-in-out-back[clamped=true] : [100, 100, 100, 100, 114.71, 150, 185.29, 200, 200, 200, 200]
                ease-out-in-back[clamped=true] : [100, 135.29, 151.451, 154.968, 152.323, 150, 147.677, 145.032, 148.549, 164.71, 200]
                ease-in-out-elastic[clamped=true] : [100, 100, 100.781, 100, 100, 150, 200, 200, 199.219, 200, 200]
                ease-out-in-elastic[clamped=true] : [100, 156.25, 151.562, 149.219, 150.098, 150, 149.902, 150.781, 148.438, 143.75, 200]
                ease-in-bounce[clamped=true] : [100, 101.188, 106, 106.938, 122.75, 123.438, 109, 131.938, 169.75, 192.438, 200]
                ease-in-out-bounce[clamped=true] : [100, 103, 111.375, 104.5, 134.875, 150, 165.125, 195.5, 188.625, 197, 200]
                ease-out-in-bounce[clamped=true] : [100, 115.125, 145.5, 138.625, 147, 150, 153, 161.375, 154.5, 184.875, 200]
                ease-in-quad[clamped=true] : [100, 101, 104, 109, 116, 125, 136, 149, 164, 181, 200]
                ease-out-quad[clamped=true] : [100, 119, 136, 151, 164, 175, 184, 191, 196, 199, 200]
                ease-in-out-quad[clamped=true] : [100, 102, 108, 118, 132, 150, 168, 182, 192, 198, 200]
                ease-sine[clamped=true] : [100, 115.643, 130.902, 145.399, 158.779, 170.711, 180.902, 189.101, 195.106, 198.769, 200]
                ease-clamp-start[clamped=true] : [100, 200, 200, 200, 200, 200, 200, 200, 200, 200, 200]
                ease-clamp-end[clamped=true] : [100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 200]
                ease-clamp-middle[clamped=true] : [100, 100, 100, 100, 100, 200, 200, 200, 200, 200, 200]
            """.trimIndent(),
            out.joinToString("\n")
        )
    }

    fun V2<Double>.samplesString(): String = samples().map { it.toStringDecimal(3, true) }.toString()

    fun V2<Double>.samples(): List<Double> {
        val v2 = this
        return (0..10).map {
            val ratio = it.toDouble() / 10.0
            v2.set(ratio)
            v2.key.get()
        }

    }
}
