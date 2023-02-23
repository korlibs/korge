package com.soywiz.benchmarks

import com.soywiz.kds.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.format.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.std.*
import kotlin.time.*
import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.BenchmarkMode
import kotlinx.benchmark.BenchmarkTimeUnit
import kotlinx.benchmark.Measurement
import kotlinx.benchmark.Mode
import kotlinx.benchmark.OutputTimeUnit
import kotlinx.benchmark.Scope
import kotlinx.benchmark.Setup
import kotlinx.benchmark.State
import org.openjdk.jmh.annotations.*

@State(Scope.Benchmark)
@Measurement(iterations = 10, time = 100, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
@Warmup(iterations = 4, time = 100, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
@OutputTimeUnit(BenchmarkTimeUnit.MICROSECONDS)
@BenchmarkMode(Mode.AverageTime)
class BitArrayBenchmarks {
    val booleanArray = BooleanArray(100000)
    val bitArray = BitArray(100000)

    @Setup
    fun setup() {
    }

    @Benchmark
    fun updateBooleanArray(): Boolean {
        // Success: 34.202 ±(99.9%) 1.005 us/op [Average]
        // (min, avg, max) = (32.500, 34.202, 60.000), stdev = 2.962
        // CI (99.9%): [33.198, 35.207] (assumes normal distribution)
        var toggle = false
        for (n in 0 until booleanArray.size) {
            booleanArray[n] = (n % 2) != 0
            toggle = toggle xor booleanArray[n]
        }
        return toggle
    }

    @Benchmark
    fun updateBitArray(): Boolean {
        // Success: 130.487 ±(99.9%) 1.930 us/op [Average]
        // (min, avg, max) = (122.209, 130.487, 173.625), stdev = 5.690
        // CI (99.9%): [128.557, 132.417] (assumes normal distribution)
        var toggle = false
        for (n in 0 until bitArray.size) {
            bitArray[n] = (n % 2) != 0
            toggle = toggle xor bitArray[n]
        }
        return toggle
    }
}
