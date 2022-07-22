package com.soywiz.korim.format

import com.soywiz.korim.bitmap.matchContentsDistinctCount
import com.soywiz.korio.async.suspendTestNoBrowser
import com.soywiz.korio.file.std.resourcesVfs
import kotlin.test.Test
import kotlin.test.assertEquals

class DDSTest {
    val props = ImageDecodingProps(format = ImageFormats(PNG, DDS))

    @Test
    fun dxt1() = suspendTestNoBrowser {
        val output = resourcesVfs["dxt1.dds"].readBitmapNoNative(props)
        val expected = resourcesVfs["dxt1.png"].readBitmapNoNative(props)
        assertEquals(0, output.matchContentsDistinctCount(expected))
    }

    @Test
    fun dxt3() = suspendTestNoBrowser {
        val output = resourcesVfs["dxt3.dds"].readBitmapNoNative(props)
        val expected = resourcesVfs["dxt3.png"].readBitmapNoNative(props)
        assertEquals(0, output.matchContentsDistinctCount(expected))
        //output.writeTo(LocalVfs("c:/temp/dxt3.png"))
    }

    @Test
    fun dxt5() = suspendTestNoBrowser {
        val output = resourcesVfs["dxt5.dds"].readBitmapNoNative(props)
        val expected = resourcesVfs["dxt5.png"].readBitmapNoNative(props)
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
