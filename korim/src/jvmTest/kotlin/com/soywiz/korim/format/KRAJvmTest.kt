package com.soywiz.korim.format

import com.soywiz.korio.async.*
import com.soywiz.korio.file.std.*
import kotlin.test.*

class KRAJvmTest {
    @Test
    fun test() = suspendTest {
        val output = resourcesVfs["krita.kra"].readImageData(KRA, ImageDecodingProps().also {
            //it.kritaPartialImageLayers = true
            it.kritaPartialImageLayers = false
            it.kritaLoadLayers = true
        })
        assertEquals(4, output.frames.size)
        //output.showImagesAndWait()
    }
}
