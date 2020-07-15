package com.soywiz.korio.compression.zip

import com.soywiz.korio.async.*
import com.soywiz.korio.file.std.*
import kotlin.test.*

class ZipFileTest {
    @Test
    fun test() = suspendTest {
        resourcesVfs["krita1.kra"].open().useIt { stream ->
            val zip = ZipFile(stream)
            //println(zip.files)
            val vfs = stream.openAsZip()
            assertEquals(65859L, vfs["mergedimage.png"].size())
        }
    }
}
