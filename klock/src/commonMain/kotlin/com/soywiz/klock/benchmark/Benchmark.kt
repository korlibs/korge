package com.soywiz.klock.benchmark

import com.soywiz.klock.*
import kotlin.jvm.JvmName
import kotlin.math.pow

data class BenchmarkResult(
    val timePerCallNanoseconds: Long,
    val maxDeviationNanoseconds: Long,
    val partialResults: List<PartialResult>,
    val dummyResult: Double
) {
    data class PartialResult(val nanos: Long, val iters: Long) {
        val nanosPerIter get() = nanos.toDouble() / iters.toDouble()

        override fun toString(): String = "PartialResult(nanosPerIter=$nanosPerIter, nanos=$nanos, iters=$iters)"
    }

    val timePerCallMicroseconds: Double get() = timePerCallNanoseconds / 1000.0
    val maxDeviationMicroseconds: Double get() = maxDeviationNanoseconds / 1000.0

    val timePerCall get() = timePerCallNanoseconds.nanoseconds
    val maxDeviation get() = maxDeviationNanoseconds.nanoseconds

    private fun rounded(value: Double) = (value * 10000).toLong() / 10000.0

    override fun toString(): String = "${rounded(timePerCallMicroseconds)} µs ± ${rounded(maxDeviationMicroseconds)} µs"
}

@JvmName("BenchmarkGeneric")
inline fun <reified T> benchmark(noinline block: () -> T): BenchmarkResult {
    return when (T::class) {
        Unit::class -> benchmark { 1.0.also { block() } }
        Int::class -> benchmark { (block() as Int).toDouble() }
        Long::class -> benchmark { (block() as Long).toDouble() }
        Double::class -> benchmark { block() as Double }
        else -> benchmark { 1.0.also { block() } }
    }
}

private fun measureScale(block: () -> Double): Int {
    var sum = 0.0
    sum += block()
    for (scale in 0..9) {
        val iters = 10.0.pow(scale).toInt()
        val time = measureTime {
            for (n in 0 until iters) {
                sum += block()
            }
        }
        sum *= 0.1
        if (time >= 10.milliseconds) return iters + (sum * 0.00000000000001 * 0.00000000000001 * 0.00000000000001 * 0.00000000000001).toInt()
    }
    return Int.MAX_VALUE
}

fun benchmark(block: () -> Double): BenchmarkResult {
    val stopwatch = Stopwatch()
    var dummySum = 0.0
    dummySum += measureScale(block)
    val itersToGetAtLeast10Ms = measureScale(block)
    //println("SCALE: $itersToGetAtLeast10Ms")

    val allResults = ArrayList<BenchmarkResult.PartialResult>()

    for (n in 0 until 200) {
        stopwatch.start()
        for (m in 0 until itersToGetAtLeast10Ms) dummySum += block()
        val time = stopwatch.elapsedNanoseconds
        allResults += BenchmarkResult.PartialResult(time.toLong(), (itersToGetAtLeast10Ms).toLong())
    }

    val results = allResults.drop(allResults.size / 2)

    val ftotalNanoseconds = results.map { it.nanos }.sum().toDouble()
    val ftotalIters = results.map { it.iters }.sum().toDouble()
    val fminNanoseconds = results.map { it.nanosPerIter }.minOrNull()!!
    val fmaxNanoseconds = results.map { it.nanosPerIter }.maxOrNull()!!
    //for (res in results) println("res: $res")

    return BenchmarkResult(
        (ftotalNanoseconds / ftotalIters).toLong(),
        (fmaxNanoseconds - fminNanoseconds).toLong(),
        results,
        dummySum
    )
}

inline fun <reified T> printBenchmark(name: String, full: Boolean = false, noinline block: () -> T) {
    val result = benchmark(block)
    println("Benchmark '$name' : $result")
    if (full) {
        for (r in result.partialResults) println(" - $r")
    }
}

// @TODO: Show ratios
fun printBenchmarks(vararg benchmarks: Pair<String, () -> Double>, full: Boolean = false) {
    for ((name, block) in benchmarks) {
        printBenchmark(name, full, block)
    }
}
