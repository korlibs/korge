package com.soywiz.korim.bench

/*
import com.soywiz.klock.*
import com.soywiz.klock.benchmark.*
import com.soywiz.korim.color.*

object KorimBenchmark {
    @JvmStatic
    fun main(args: Array<String>) {
        val size = 4096
        val colors1 = RgbaPremultipliedArray(size)
        val colors2 = RgbaPremultipliedArray(size)
        for (n in 0 until size) {
            colors1[n] = RGBAPremultiplied(n, n, n, n)
            colors2[n] = RGBAPremultiplied(n * 2, n * 2, n * 2, n * 2)
        }
        for (n in 0 until 10) {
            printBenchmark("mix") {
                for (n in 0 until size) {
                    colors1[n] = RGBAPremultiplied.mix(colors1[n], colors2[n])
                }
            }
            printBenchmark("mix2") {
                for (n in 0 until size) {
                    colors1[n] = RGBAPremultiplied.mix(colors1[n], colors2[n])
                }
            }
        }
    }

    inline fun <reified T> printBenchmark(name: String, full: Boolean = false, noinline block: () -> T) {
        val result = benchmark(block)
        println("Benchmark '$name' : $result")
    }
}
*/
