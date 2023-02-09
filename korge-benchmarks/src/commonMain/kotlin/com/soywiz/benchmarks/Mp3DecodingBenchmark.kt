package com.soywiz.benchmarks

import com.soywiz.korau.format.mp3.*
import com.soywiz.korau.format.mp3.javamp3.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.std.*
import kotlinx.benchmark.*

@State(Scope.Benchmark)
@Measurement(iterations = 100, time = 3, timeUnit = BenchmarkTimeUnit.MICROSECONDS)
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
            val output = FastMP3Decoder.decode(bytes)
        }
    }

    @Benchmark
    fun testMiniMp3Java() = suspendTest {
        val bytes = resourcesVfs["mp31.mp3"].readBytes()
        //for (n in 0 until 100) {
        for (n in 0 until 100) {
            val output = JavaMp3AudioFormat.decode(bytes)
        }
    }
}
