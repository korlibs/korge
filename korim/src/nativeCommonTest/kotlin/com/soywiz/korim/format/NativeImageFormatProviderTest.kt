package com.soywiz.korim.format

import com.soywiz.korio.async.*
import com.soywiz.korio.file.std.*
import com.soywiz.korma.geom.*
import kotlin.test.*

class NativeImageFormatProviderTest {
    @Test
    fun test() = suspendTest {
        val bmp1 = nativeImageFormatProvider.decode(resourcesVfs["kotlin.jpg"])
        val bmp2 = nativeImageFormatProvider.decode(resourcesVfs["kotlin.jpg.png"])
        val bmp3 = nativeImageFormatProvider.decode(resourcesVfs["kotlin.bmp"])
        //val bmp4 = nativeImageFormatProvider.decode(resourcesVfs["kotlin.tga"])
        val bmp5 = nativeImageFormatProvider.decode(resourcesVfs["kotlin8.png"])
        val bmp6 = nativeImageFormatProvider.decode(resourcesVfs["kotlin24.png"])
        val bmp7 = nativeImageFormatProvider.decode(resourcesVfs["kotlin32.png"])
        assertEquals(Size(190, 190), bmp1.size)
        assertEquals(Size(190, 190), bmp2.size)
        assertEquals(Size(190, 190), bmp3.size)
        //assertEquals(Size(190, 190), bmp4.size)
        assertEquals(Size(190, 190), bmp5.size)
        assertEquals(Size(190, 190), bmp6.size)
        assertEquals(Size(190, 190), bmp7.size)
    }
}
