package com.soywiz.korim.format

import com.soywiz.korim.bitmap.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.util.*
import kotlin.test.*

class ASETest {
    @Test
    fun test() = suspendTest({ !OS.isJs }) {
        //val image = resourcesVfs["simple2.ase"].readImageData(ASE)
        val image = resourcesVfs["simple.ase"].readImageData(ASE)
        //val image = resourcesVfs["simple3.ase"].readImageData(ASE)
        val packed = image.packInAtlas()
        //for (img in image.frames.flatMap { it.layerData }.map { it.bitmap }) img.showImageAndWait()
        //for (img in packed.atlas.atlases.map { it.tex }) img.showImageAndWait()
        //Bitmap32(10, 10).flipX()
    }
}
