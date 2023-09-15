package korlibs.benchmarks

import korlibs.audio.format.*
import korlibs.io.async.*
import korlibs.io.file.std.*
import kotlinx.benchmark.*

@State(Scope.Benchmark)
@Measurement(iterations = 10, time = 100, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
@Warmup(iterations = 4, time = 100, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
@OutputTimeUnit(BenchmarkTimeUnit.MICROSECONDS)
@BenchmarkMode(Mode.AverageTime)
class Mp3DecodingBenchmark {
    @Setup
    fun setup() {
    }

    @Benchmark
    fun miniMp3Fast() = suspendTest {
        val bytes = resourcesVfs["mp31.mp3"].readBytes()
        //for (n in 0 until 100) {
        for (n in 0 until 100) {
            val output = MP3Decoder.decode(bytes)
        }
    }
}
