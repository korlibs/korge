package com.soywiz.korau.mod

import com.soywiz.kmem.toUint8Buffer
import com.soywiz.kmem.writeArrayLE
import com.soywiz.korau.sound.encodeToFile
import com.soywiz.korau.sound.toSound
import com.soywiz.korio.async.suspendTest
import com.soywiz.korio.file.std.localVfs
import com.soywiz.korio.file.std.resourcesVfs
import doIOTest
import kotlin.test.Test

class XMTest {
    @Test
    fun test() = suspendTest({ doIOTest }) {
        resourcesVfs["poliamber.xm"].readXM().toAudioData(maxSamples = 4096)

        //val mod = Fasttracker()
        //mod.parseAndInit(resourcesVfs["poliamber.xm"].readBytes().toUint8Buffer())
        //val bufSize = 4 * 44100
        //val out = Array(2) { FloatArray(bufSize) }
        //mod.mix(out)

        //val ba = ByteArray(bufSize * 4)
        //ba.writeArrayLE(0, out[0])
        //localVfs("/tmp/demo.wav.raw").writeBytes(ba)
        ////resourcesVfs["transatlantic.xm"].readXM().toData(4 * 44100)
        //resourcesVfs["poliamber.xm"].readXM().toData(4 * 44100)
        //    .toSound().playAndWait()
        //    //.encodeToFile(localVfs("/tmp/demo.wav"))

    }
}
