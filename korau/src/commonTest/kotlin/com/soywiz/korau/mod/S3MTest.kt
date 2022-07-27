package com.soywiz.korau.mod

import com.soywiz.korau.format.WAV
import com.soywiz.korau.sound.toSound
import com.soywiz.korio.async.suspendTest
import com.soywiz.korio.file.std.resourcesVfs
import com.soywiz.korio.file.writeToFile
import doIOTest
import kotlin.test.Test

class S3MTest {
    @Test
    fun test() = suspendTest({ doIOTest }) {
        val sound = resourcesVfs["12oz.s3m"].readS3M()
        val data = sound.toData(maxSamples = 44100 * 4)
        //val data = sound.toData(maxSamples = 44100 * 16)
        //val data = sound.toData(maxSamples = 5300)
        //val data = sound.toData(maxSamples = 16384)
        //data.toSound().playAndWait()
        //WAV.encodeToByteArray(data).writeToFile("/tmp/12oz.s3m.wav")
        //sound.playAndWait()
        //val mod = Protracker()
        //mod.parse(Uint8Buffer(NewInt8Buffer(MemBufferWrap(bytes), 0, bytes.size)))
        //val out = arrayOf(FloatArray(8000), FloatArray(8000))
        //mod.playing = true
        //mod.mix(mod, out)
        //println(out)
    }
}
