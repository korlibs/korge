package com.soywiz.korim.format

import com.soywiz.kds.iterators.*
import com.soywiz.korim.atlas.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.util.*
import kotlin.test.*

class ASETest {
    @Test
    fun test() = suspendTest({ !OS.isJs }) {
        val atlas = MutableAtlasUnit(2048)
        val simple = resourcesVfs["simple.ase"].readImageData(ASE, atlas = atlas)
        val simple2 = resourcesVfs["simple2.ase"].readImageData(ASE, atlas = atlas)
        val simple3 = resourcesVfs["simple3.ase"].readImageData(ASE, atlas = atlas)
        val sliceExample = resourcesVfs["slice-example.ase"].readImageDataContainer(ASE, atlas = atlas)
        //println(sliceExample)
        //atlas.allBitmaps.showImagesAndWait()
    }
}
