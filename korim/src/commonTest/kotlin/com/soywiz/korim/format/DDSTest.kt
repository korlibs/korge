package com.soywiz.korim.format

import com.soywiz.korim.bitmap.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.std.*
import kotlin.test.*

class DDSTest {
    val formats = ImageFormats(PNG, DDS)

    @Test
    fun dxt1() = suspendTestNoBrowser {
        val output = resourcesVfs["dxt1.dds"].readBitmapNoNative(formats)
        val expected = resourcesVfs["dxt1.png"].readBitmapNoNative(formats)
        assertEquals(0, output.matchContentsDistinctCount(expected))
    }

    @Test
    fun dxt3() = suspendTestNoBrowser {
        val output = resourcesVfs["dxt3.dds"].readBitmapNoNative(formats)
        val expected = resourcesVfs["dxt3.png"].readBitmapNoNative(formats)
        assertEquals(0, output.matchContentsDistinctCount(expected))
        //output.writeTo(LocalVfs("c:/temp/dxt3.png"))
    }

    @Test
    fun dxt5() = suspendTestNoBrowser {
        val output = resourcesVfs["dxt5.dds"].readBitmapNoNative(formats)
        val expected = resourcesVfs["dxt5.png"].readBitmapNoNative(formats)
        assertEquals(0, output.matchContentsDistinctCount(expected))
        //output.writeTo(LocalVfs("c:/temp/dxt5.png"))
    }

    @Test
    fun dxt5_registered() = suspendTestNoBrowser {
        RegisteredImageFormats.temporalRegister(DDS, PNG) {
            val output = resourcesVfs["dxt5.dds"].readBitmapNoNative()
            val expected = resourcesVfs["dxt5.png"].readBitmapNoNative()
            assertEquals(0, output.matchContentsDistinctCount(expected))
            //output.writeTo(LocalVfs("c:/temp/dxt5.png"))
        }
    }
}
