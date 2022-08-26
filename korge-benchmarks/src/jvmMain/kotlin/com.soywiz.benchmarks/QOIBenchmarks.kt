package com.soywiz.benchmarks

import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.format.ImageDecodingProps
import com.soywiz.korim.format.QOI
import com.soywiz.korio.async.runBlockingNoJs
import com.soywiz.korio.file.std.resourcesVfs
import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.BenchmarkMode
import kotlinx.benchmark.BenchmarkTimeUnit
import kotlinx.benchmark.Measurement
import kotlinx.benchmark.Mode
import kotlinx.benchmark.OutputTimeUnit
import kotlinx.benchmark.Scope
import kotlinx.benchmark.Setup
import kotlinx.benchmark.State

@State(Scope.Benchmark)
@Measurement(iterations = 100, time = 3, timeUnit = BenchmarkTimeUnit.MICROSECONDS)
@OutputTimeUnit(BenchmarkTimeUnit.MICROSECONDS)
@BenchmarkMode(Mode.AverageTime)
class QOIBenchmarks {
    var qoiBytes: ByteArray = ByteArray(0)
    var qoiOutputBitmap = Bitmap32(800, 600, premultiplied = false)
    val prop = ImageDecodingProps(out = qoiOutputBitmap)

    @Setup
    fun setup() {
        runBlockingNoJs {
            qoiBytes = resourcesVfs["dice.qoi"].readBytes()
        }
    }

    @Benchmark
    fun decodeBitmap(): Bitmap {
        // 1207.829 ±(99.9%) 9.497 us/op [Average]
        //  (min, avg, max) = (1175.562, 1207.829, 1290.125), stdev = 28.001
        //  CI (99.9%): [1198.332, 1217.325] (assumes normal distribution)
        return QOI.decode(qoiBytes)
    }

    @Benchmark
    fun decodeToOutputBitmap(): Bitmap {
        // 1024.693 ±(99.9%) 8.932 us/op [Average]
        //  (min, avg, max) = (1002.131, 1024.693, 1212.177), stdev = 26.335
        //  CI (99.9%): [1015.761, 1033.624] (assumes normal distribution)
        return QOI.decode(qoiBytes, prop)
    }
}
