package korlibs.benchmarks


import korlibs.memory.UByteArrayInt
import korlibs.image.bitmap.Bitmap32
import korlibs.image.color.*
import korlibs.image.format.*
import korlibs.io.async.runBlockingNoJs
import korlibs.io.file.std.resourcesVfs
import kotlinx.benchmark.*

// +Benchmark                                  Mode  Cnt  Score   Error  Units
// RGBABenchmarks.depremultiplied             avgt   10  0.032 ± 0.001  us/op
// RGBABenchmarks.depremultipliedAccurate     avgt   10  0.049 ± 0.004  us/op
// RGBABenchmarks.depremultipliedAccurateAlt  avgt   10  0.086 ± 0.011  us/op
// RGBABenchmarks.depremultipliedFast         avgt   10  0.032 ± 0.001  us/op
// RGBABenchmarks.premultiplied               avgt   10  0.022 ± 0.001  us/op
// RGBABenchmarks.premultipliedAccurate       avgt   10  0.043 ± 0.001  us/op
// RGBABenchmarks.premultipliedAccurateAlt    avgt   10  0.059 ± 0.001  us/op
// RGBABenchmarks.premultipliedFast           avgt   10  0.021 ± 0.001  us/op
@State(Scope.Benchmark)
@Measurement(iterations = 10, time = 100, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
@Warmup(iterations = 4, time = 100, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
@OutputTimeUnit(BenchmarkTimeUnit.MICROSECONDS)
@BenchmarkMode(Mode.AverageTime)
class RGBABenchmarks {
    var qoiBytes: ByteArray = ByteArray(0)
    var qoiOutputBitmap = Bitmap32(800, 600, premultiplied = false)
    val decodePropToOutputBitmap = ImageDecodingProps(out = qoiOutputBitmap)

    var bitmapImage = Bitmap32(1, 1, true)
    val preAllocatedArrayForEncoding = UByteArrayInt(QOI.calculateMaxSize(800, 600))
    val encodePropWithPreAllocatedArray = ImageEncodingProps {
        preAllocatedArrayForQOI = preAllocatedArrayForEncoding
    }

    @Setup
    fun setup() {
        runBlockingNoJs {
            qoiBytes = resourcesVfs["dice.qoi"].readBytes()
            bitmapImage = resourcesVfs["dice.qoi"].readBitmapNoNative(QOI).toBMP32()
        }
    }

    val colors = RgbaArray(IntArray(16) {
        Colors.PALEVIOLETRED.withAd(it.toDouble() / 15.0).value
    })

    val colorsPremult = RgbaPremultipliedArray(IntArray(16) {
        Colors.PALEVIOLETRED.withAd(it.toDouble() / 15.0).premultiplied.value
    })

    @Benchmark
    fun depremultiplied(): Int {
        var v = 0
        for (n in 0 until colorsPremult.size) v += colorsPremult[n].depremultiplied.value
        return v
    }

    @Benchmark
    fun depremultipliedFast(): Int {
        var v = 0
        for (n in 0 until colorsPremult.size) v += colorsPremult[n].depremultipliedFast.value
        return v
    }

    @Benchmark
    fun depremultipliedAccurate(): Int {
        var v = 0
        for (n in 0 until colorsPremult.size) v += colorsPremult[n].depremultipliedAccurate.value
        return v
    }

    @Benchmark
    fun depremultipliedAccurateAlt(): Int {
        var v = 0
        for (n in 0 until colorsPremult.size) v += colorsPremult[n].depremultipliedAccurateAlt.value
        return v
    }

    @Benchmark
    fun premultiplied(): Int {
        var v = 0
        for (n in 0 until colors.size) v += colors[n].premultiplied.value
        return v
    }

    @Benchmark
    fun premultipliedFast(): Int {
        var v = 0
        for (n in 0 until colors.size) v += colors[n].premultipliedFast.value
        return v
    }

    @Benchmark
    fun premultipliedAccurate(): Int {
        var v = 0
        for (n in 0 until colors.size) v += colors[n].premultipliedAccurate.value
        return v
    }

    @Benchmark
    fun premultipliedAccurateAlt(): Int {
        var v = 0
        for (n in 0 until colors.size) v += colors[n].premultipliedAccurateAlt.value
        return v
    }
}
