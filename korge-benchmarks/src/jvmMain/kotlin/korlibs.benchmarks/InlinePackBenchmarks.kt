package korlibs.benchmarks

import korlibs.memory.*
import kotlinx.benchmark.*
import kotlinx.benchmark.Benchmark

@State(Scope.Benchmark)
@Measurement(iterations = 10, time = 100, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
@Warmup(iterations = 4, time = 100, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
@OutputTimeUnit(BenchmarkTimeUnit.MICROSECONDS)
@BenchmarkMode(Mode.AverageTime)
class InlinePackBenchmarks {
    inline class LongPack(val data: Long) {
        constructor(x: Int, y: Int) : this((x.toLong() and 0xFFFFFFFFL) or (y.toLong() shl 32))

        val x: Int get() = data.toInt()
        val y: Int get() = (data shr 32).toInt()
    }

    inline class FloatPack(val long: LongPack) {
        constructor(x: Float, y: Float) : this(LongPack(x.reinterpretAsInt(), y.reinterpretAsInt()))

        val x: Float get() = long.x.reinterpretAsFloat()
        val y: Float get() = long.y.reinterpretAsFloat()
    }

    class SeparatedPack(val x: Int, val y: Int) {
        val data: Long get() = (x.toLong() and 0xFFFFFFFFL) or (y.toLong() shl 32)
    }

    class SeparatedPackFloat(val x: Float, val y: Float) {
        //val data: Long get() = (x.toLong() and 0xFFFFFFFFL) or (y.toLong() shl 32)
    }


    @Setup
    fun setup() {
    }

    @Benchmark
    fun useLongPack(): Int {
        var res = 0
        for (n in 0 until 1000) {
            val pack = LongPack(7777777 * n, 3333333 * n)
            res += pack.x * pack.y + pack.x + pack.y
        }
        return res
    }

    @Benchmark
    fun useFloatPack(): Float {
        var res = 0f
        for (n in 0 until 1000) {
            val pack = FloatPack(7777777f * n, 3333333f * n)
            res += pack.x * pack.y + pack.x + pack.y
        }
        return res
    }

    @Benchmark
    fun useSeparatedPack(): Int {
        var res = 0
        for (n in 0 until 1000) {
            val pack = SeparatedPack(7777777 * n, 3333333 * n)
            res += pack.x * pack.y + pack.x + pack.y
        }
        return res
    }

    @Benchmark
    fun useSeparatedFloat(): Float {
        var res = 0f
        for (n in 0 until 1000) {
            val pack = SeparatedPackFloat(7777777f * n, 3333333f * n)
            res += pack.x * pack.y + pack.x + pack.y
        }
        return res
    }
}