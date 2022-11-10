package com.soywiz.benchmarks

import com.soywiz.kmem.UByteArrayInt
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.format.*
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
    val decodePropToOutputBitmap = ImageDecodingProps(out = qoiOutputBitmap)

    var bitmapImage = Bitmap32(1, 1, true)
    val preAllocatedArrayForEncoding = UByteArrayInt(QOI.calculateMaxSize(800, 600))
    val encodePropWithPreAllocatedArray = ImageEncodingProps().apply {
        preAllocatedArrayForQOI = preAllocatedArrayForEncoding
    }

    @Setup
    fun setup() {
        runBlockingNoJs {
            qoiBytes = resourcesVfs["dice.qoi"].readBytes()
            bitmapImage = resourcesVfs["dice.qoi"].readBitmapNoNative(QOI).toBMP32()
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
        return QOI.decode(qoiBytes, decodePropToOutputBitmap)
    }

    @Benchmark
    fun encodeBitmap(): ByteArray {
        //2257.462 ±(99.9%) 49.785 us/op [Average]
        //(min, avg, max) = (2109.371, 2257.462, 2735.280), stdev = 146.792
       ubled //CI (99.9%): [2207.677, 2307.247] (assumes normal distribution)
        return QOI.encode(bitmapImage)
    }

    @Benchmark
    fun encodeBitmapWithPreallocatedArray(): ByteArray {
        //1885.790 ±(99.9%) 43.724 us/op [Average]
        //(min, avg, max) = (1750.356, 1885.790, 2407.650), stdev = 128.921
        //CI (99.9%): [1842.066, 1929.514] (assumes normal distribution)
        return QOI.encode(bitmapImage, props = encodePropWithPreAllocatedArray)
    }
}
