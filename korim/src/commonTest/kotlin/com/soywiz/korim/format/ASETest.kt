package com.soywiz.korim.format

import com.soywiz.korim.atlas.*
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
        val sliceExample2 = resourcesVfs["slice-example2.ase"].readImageDataContainer(ASE, atlas = atlas)
        val hiddenLayer = resourcesVfs["hidden-layer.ase"].readImageData(ASE, atlas = atlas)

        assertEquals(2, hiddenLayer.frames[0].layerData.size, "There are only two (visible) layers in layerData of hidden-layer.ase file")
        assertEquals(4, sliceExample2.imageDatas.size, "4 image data objects, two for 2 slices on layer 1 and two for anothe 2 slices on layer 2")
        assertEquals(5, sliceExample.imageDatas.size, "5 slices create 5 image data objects")

        // TODO: Add test for readParallaxDataContainerFromAseFile
        //       Check if slicing is working per selected layer

        //println(sliceExample)
        //atlas.allBitmaps.showImagesAndWait()
    }
}
