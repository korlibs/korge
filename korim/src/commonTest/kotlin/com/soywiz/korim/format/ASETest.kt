package com.soywiz.korim.format

import com.soywiz.kds.ExtraTypeCreate
import com.soywiz.kds.setExtra
import com.soywiz.korim.atlas.MutableAtlasUnit
import com.soywiz.korim.tiles.render
import com.soywiz.korio.async.suspendTest
import com.soywiz.korio.file.std.resourcesVfs
import com.soywiz.korio.util.OS
import com.soywiz.korma.geom.RectangleInt
import com.soywiz.korma.geom.Size
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ASETest {
    @Test
    fun test() = suspendTest({ !OS.isJs }) {
        val atlas = MutableAtlasUnit(2048)
        val simple = resourcesVfs["simple.ase"].readImageData(ASE, atlas = atlas)
        val simple2 = resourcesVfs["simple2.ase"].readImageData(ASE, atlas = atlas)
        val simple3 = resourcesVfs["simple3.ase"].readImageData(ASE, atlas = atlas)

        // Check reading of objects from different slices of an image
        val sliceExample = resourcesVfs["slice-example.ase"].readImageDataContainer(ASE, atlas = atlas)
        assertEquals(5, sliceExample.imageDatas.size, "5 slices create 5 image data objects")

        // Check that invisible layers are ignored
        val hiddenLayer = resourcesVfs["hidden-layer.ase"].readImageData(ASE, atlas = atlas)
        assertEquals(2, hiddenLayer.frames[0].layerData.size, "There are only two (visible) layers in layerData of hidden-layer.ase file")

        // Check slicing of an image with 2 layers
        val sliceExample2 = resourcesVfs["slice-example2.ase"].readImageDataContainer(ASE, atlas = atlas)
        assertEquals(2, sliceExample2.imageDatas.size, "2 image data objects for 2 slices")
        assertEquals(2, sliceExample2.imageDatas[0].layers.size, "2 layers on slice 1")
        assertEquals(2, sliceExample2.imageDatas[1].layers.size, "2 layers on slice 2")

        // Check if loading only specific layer(s) works
        val props = ImageDecodingProps(extra = ExtraTypeCreate())
        props.setExtra("layers", "Layer 2")
        var specificLayers = resourcesVfs["hidden-layer.ase"].readImageDataContainer(ASE, props = props, atlas = atlas)
        assertEquals(1, specificLayers.imageDatas[0].layers.size, "Read only one layer from Aseprite file")
        props.setExtra("layers", "Layer 2,Layer 3")
        specificLayers = resourcesVfs["hidden-layer.ase"].readImageDataContainer(ASE, props = props, atlas = atlas)
        assertEquals(2, specificLayers.imageDatas[0].layers.size, "Read two layers from Aseprite file")

        // Check if disabling slicing is working for an Aseprite file which contains slices
        val props2 = ImageDecodingProps(extra = ExtraTypeCreate())
        props2.setExtra("disableSlicing", true)
        val sliceExample3 = resourcesVfs["slice-example.ase"].readImageDataContainer(ASE, props = props2, atlas =
        atlas)
        assertEquals(1, sliceExample3.imageDatas.size, "1 image which contains all 5 \"objects\" because slicing is disabled")

        // Check if position of sliced objects is taken over correctly
        props2.setExtra("disableSlicing", false)
        props2.setExtra("useSlicePosition", true)
        val sliceExample4 = resourcesVfs["slice-example.ase"].readImageDataContainer(ASE, props = props2, atlas = atlas)
        assertEquals(5, sliceExample4.imageDatas.size, "5 slices create 5 image data objects")
        assertEquals(16, sliceExample4.imageDatasByName["giantHilt"]!!.frames[0].targetX, "X position of giantHilt set to slice X position")
        assertEquals(16, sliceExample4.imageDatasByName["giantHilt"]!!.frames[0].targetY, "Y position of giantHilt set to slice Y position")
        assertEquals(48, sliceExample4.imageDatasByName["coin"]!!.frames[0].targetX, "X position of coin set to slice X position")
        assertEquals(80, sliceExample4.imageDatasByName["coin"]!!.frames[0].targetY, "Y position of coin set to slice Y position")

        // complex example (empty cells within frames of an ase file with animations tags)
        val props3 = ImageDecodingProps(extra = ExtraTypeCreate())
        props3.setExtra("layers", "shield")
        val complexLayersAndTags = resourcesVfs["space_ship.ase"].readImageDataContainer(ASE, props = props3, atlas = atlas)
        assertEquals(17, complexLayersAndTags.default.frames.size, "ImageFrame array not correctly created")
        assertEquals(1, complexLayersAndTags.default.frames[0].width, "First frame is not an empty image (width and height == 1)")
        assertEquals(61, complexLayersAndTags.default.frames[5].width, "First frame of animation tag \"shield_loop\" is not a valid image (width == 61)")
        assertEquals(62, complexLayersAndTags.default.frames[10].width, "Last frame of animation tag \"shield_loop\" is not a valid image (width == 62)")
        assertEquals(6, complexLayersAndTags.default.animationsByName["shield_loop"]?.frames?.size ?: 0, "Animation \"shield_loop\" was not found in ase file.")

        //println(sliceExample)
        //atlas.allBitmaps.showImagesAndWait()
    }

    @Test
    fun testTilemap() = suspendTest({ !OS.isJs }) {
        val ase = resourcesVfs["asepritetilemap.aseprite"].readImageData(ASE)
        val tilemap = ase.frames[0].layerData[1].tilemap
        assertNotNull(tilemap)
        val tilemapStr = tilemap.data.toStringList({ it.digitToChar() }).joinToString("\n")
        assertEquals(
            """
                12121212
                34343434
                56565656
                78787878
                12121212
                34343434
                56565656
                78787878
            """.trimIndent(),
            tilemapStr
        )
        val tileSet = tilemap.tileSet
        assertNotNull(tileSet)
        assertEquals(
            listOf(0, 1, 2, 3, 4, 5, 6, 7, 8),
            tileSet.texturesMap.keys.toList().sorted()
        )
        assertEquals(Size(16, 144), tileSet.base.size)
        for (n in 0..8) {
            assertEquals(RectangleInt(0, (n * 16), 16, 16), tileSet.texturesMap[n]!!.slice.bmpCoords.getRectInt())
        }
    }
}
