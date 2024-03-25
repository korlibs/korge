package korlibs.image.format

import korlibs.datastructure.*
import korlibs.image.atlas.*
import korlibs.image.bitmap.*
import korlibs.image.tiles.*
import korlibs.io.async.*
import korlibs.io.file.std.*
import korlibs.math.geom.*
import korlibs.platform.*
import kotlin.test.*

class ASETest {
    val ASEDecoder = ImageDecodingProps(format = ASE)

    @Test
    fun testPremultiplied() = suspendTest({ !Platform.isJs }) {
        val noprem = resourcesVfs["vampire.ase"].readImageDataContainer(
            ASE.toProps(
                ImageDecodingProps.DEFAULT(premultiplied = false)
            )
        )
        val prem = resourcesVfs["vampire.ase"].readImageDataContainer(
            ASE.toProps(
                ImageDecodingProps.DEFAULT(premultiplied = true)
            )
        )
        assertEquals(true, prem.mainBitmap.premultiplied)
        assertEquals(false, noprem.mainBitmap.premultiplied)
        assertTrue {
            prem.imageDatas.first().frames.flatMap { it.layerData }
                .all { it.slice.bmp.premultiplied }
        }
        assertTrue {
            noprem.imageDatas.first().frames.flatMap { it.layerData }
                .all { !it.slice.bmp.premultiplied }
        }
    }

    @Test
    fun test() = suspendTest({ !Platform.isJs }) {
        val atlas = MutableAtlasUnit(2048)
        val simple = resourcesVfs["simple.ase"].readImageData(ASEDecoder, atlas = atlas)
        val simple2 = resourcesVfs["simple2.ase"].readImageData(ASEDecoder, atlas = atlas)
        val simple3 = resourcesVfs["simple3.ase"].readImageData(ASEDecoder, atlas = atlas)

        // Check reading of objects from different slices of an image
        val sliceExample =
            resourcesVfs["slice-example.ase"].readImageDataContainer(ASEDecoder, atlas = atlas)
        assertEquals(5, sliceExample.imageDatas.size, "5 slices create 5 image data objects")

        // Check that invisible layers are ignored
        val hiddenLayer = resourcesVfs["hidden-layer.ase"].readImageData(ASEDecoder, atlas = atlas)
        assertEquals(
            2,
            hiddenLayer.frames[0].layerData.size,
            "There are only two (visible) layers in layerData of hidden-layer.ase file"
        )

        // Check slicing of an image with 2 layers
        val sliceExample2 =
            resourcesVfs["slice-example2.ase"].readImageDataContainer(ASEDecoder, atlas = atlas)
        assertEquals(2, sliceExample2.imageDatas.size, "2 image data objects for 2 slices")
        assertEquals(2, sliceExample2.imageDatas[0].layers.size, "2 layers on slice 1")
        assertEquals(2, sliceExample2.imageDatas[1].layers.size, "2 layers on slice 2")

        // Check if loading only specific layer(s) works
        val props = ASEDecoder.copy(extra = ExtraTypeCreate())
        props.setExtra("layers", "Layer 2")
        var specificLayers =
            resourcesVfs["hidden-layer.ase"].readImageDataContainer(props = props, atlas = atlas)
        assertEquals(
            1,
            specificLayers.imageDatas[0].layers.size,
            "Read only one layer from Aseprite file"
        )
        props.setExtra("layers", "Layer 2,Layer 3")
        specificLayers =
            resourcesVfs["hidden-layer.ase"].readImageDataContainer(props = props, atlas = atlas)
        assertEquals(
            2,
            specificLayers.imageDatas[0].layers.size,
            "Read two layers from Aseprite file"
        )

        // Check if disabling slicing is working for an Aseprite file which contains slices
        val props2 = ASEDecoder.copy(extra = ExtraTypeCreate())
        props2.setExtra("disableSlicing", true)
        val sliceExample3 = resourcesVfs["slice-example.ase"].readImageDataContainer(
            props = props2, atlas =
            atlas
        )
        assertEquals(
            1,
            sliceExample3.imageDatas.size,
            "1 image which contains all 5 \"objects\" because slicing is disabled"
        )

        // Check if position of sliced objects is taken over correctly
        props2.setExtra("disableSlicing", false)
        props2.setExtra("useSlicePosition", true)
        val sliceExample4 =
            resourcesVfs["slice-example.ase"].readImageDataContainer(props = props2, atlas = atlas)
        assertEquals(5, sliceExample4.imageDatas.size, "5 slices create 5 image data objects")
        assertEquals(
            16,
            sliceExample4.imageDatasByName["giantHilt"]!!.frames[0].targetX,
            "X position of giantHilt set to slice X position"
        )
        assertEquals(
            16,
            sliceExample4.imageDatasByName["giantHilt"]!!.frames[0].targetY,
            "Y position of giantHilt set to slice Y position"
        )
        assertEquals(
            48,
            sliceExample4.imageDatasByName["coin"]!!.frames[0].targetX,
            "X position of coin set to slice X position"
        )
        assertEquals(
            80,
            sliceExample4.imageDatasByName["coin"]!!.frames[0].targetY,
            "Y position of coin set to slice Y position"
        )

        //println(sliceExample)
        //atlas.allBitmaps.showImagesAndWait()
    }

    @Test
    fun testBigAseImage() = suspendTest({ !Platform.isJs && !Platform.isAndroid }) {
        val atlas = MutableAtlasUnit(2048)
        // complex example (empty cells within frames of an ase file with animations tags)
        val props3 = ASEDecoder.copy(extra = ExtraTypeCreate())
        props3.setExtra("layers", "shield")
        val complexLayersAndTags =
            resourcesVfs["space_ship.ase"].readImageDataContainer(props = props3, atlas = atlas)
        assertEquals(
            17,
            complexLayersAndTags.default.frames.size,
            "ImageFrame array not correctly created"
        )
        assertEquals(
            1,
            complexLayersAndTags.default.frames[0].width,
            "First frame is not an empty image (width and height == 1)"
        )
        assertEquals(
            61,
            complexLayersAndTags.default.frames[5].width,
            "First frame of animation tag \"shield_loop\" is not a valid image (width == 61)"
        )
        assertEquals(
            62,
            complexLayersAndTags.default.frames[10].width,
            "Last frame of animation tag \"shield_loop\" is not a valid image (width == 62)"
        )
        assertEquals(
            6,
            complexLayersAndTags.default.animationsByName["shield_loop"]?.frames?.size
                ?: 0,
            "Animation \"shield_loop\" was not found in ase file."
        )
    }

    @Test
    fun testTilemap() = suspendTest({ !Platform.isJs }) {
        val ase = resourcesVfs["asepritetilemap.aseprite"].readImageData(ASEDecoder)
        val tilemap = ase.frames[0].layerData[1].tilemap
        assertNotNull(tilemap)
        val tilemapStr = tilemap.toStringListSimplified { it.tile.digitToChar() }.joinToString("\n")
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
            tileSet.tilesMap.keys.toList().sorted()
        )
        assertEquals(SizeInt(16, 144), tileSet.base.size)
        for (n in 0..8) {
            assertEquals(RectangleInt(0, (n * 16), 16, 16), tileSet.tilesMap[n]!!.slice.rect)
        }
    }

    @Test
    fun testSlicesIssue() = suspendTest({ !Platform.isJs }) {
        val slicesCorrupted =
            resourcesVfs["vampire_slices_corrupted.ase"].readImageDataContainer(ASE.toProps())
        val slicesFixed =
            resourcesVfs["vampire_slices_fixed.ase"].readImageDataContainer(ASE.toProps())

        assertEquals(
            listOf("vampire", "vamp", "vampire"),
            slicesCorrupted.imageDatas.map { it.name })

        assertEquals(listOf("vampire", "vamp"), slicesCorrupted.imageDatasByName.keys.toList())
        assertEquals(listOf("vamp", "vampire"), slicesFixed.imageDatasByName.keys.toList())

        // Corrupted data
        assertEquals(
            """
                -3407867,-4718584:Rectangle(x=47, y=28, width=0, height=0)
                -3407868,-4718585:Rectangle(x=49, y=29, width=0, height=0)
                -3407867,-4718584:Rectangle(x=47, y=28, width=0, height=0)
                -3407867,-4718584:Rectangle(x=47, y=28, width=0, height=0)
                -3407867,-4718585:Rectangle(x=47, y=29, width=0, height=0)
                -3407867,-4718584:Rectangle(x=47, y=28, width=0, height=0)
                -3407867,-4718584:Rectangle(x=47, y=28, width=0, height=0)
                -3407867,-4718585:Rectangle(x=47, y=29, width=0, height=0)
                -3407868,-4718584:Rectangle(x=48, y=28, width=0, height=0)
                -3407868,-4718584:Rectangle(x=48, y=28, width=0, height=0)
                -3407868,-4718585:Rectangle(x=49, y=29, width=0, height=0)
                -3407868,-4718584:Rectangle(x=48, y=28, width=0, height=0)
            """.trimIndent(),
            slicesCorrupted.imageDatas[0].frames.map { "${it.targetX},${it.targetY}:" + it.slice.bounds }
                .joinToString("\n")
        )

        // Valid data
        assertEquals(
            """
                -8,-24:Rectangle(x=27, y=0, width=20, height=28)
                -9,-25:Rectangle(x=28, y=0, width=21, height=29)
                -8,-24:Rectangle(x=27, y=0, width=20, height=28)
                -8,-24:Rectangle(x=27, y=0, width=20, height=28)
                -8,-25:Rectangle(x=27, y=0, width=20, height=29)
                -8,-24:Rectangle(x=27, y=0, width=20, height=28)
                -8,-24:Rectangle(x=27, y=0, width=20, height=28)
                -8,-25:Rectangle(x=27, y=0, width=20, height=29)
                -9,-24:Rectangle(x=28, y=0, width=20, height=28)
                -9,-24:Rectangle(x=28, y=0, width=20, height=28)
                -9,-25:Rectangle(x=28, y=0, width=21, height=29)
                -9,-24:Rectangle(x=28, y=0, width=20, height=28)
            """.trimIndent(),
            slicesCorrupted.imageDatas[1].frames.map { "${it.targetX},${it.targetY}:" + it.slice.bounds }
                .joinToString("\n")
        )
    }

    @Test
    fun readsOnlyVisibleLayers() = suspendTest({ !Platform.isJs }) {
        val ase = resourcesVfs["ase_tests/ase_with_layers.aseprite"].readImageDataContainer(
            ASE.toProps(ImageDecodingProps.DEFAULT(premultiplied = false)).apply {
                onlyReadVisibleLayers = true
            }
        )

        val layersAseIndexToName = ase.default.layers.map { it as ASE.AseLayer }
            .associate { it.originalAseIndex to it.name!! }
        assertEquals("visible1", layersAseIndexToName[0])
        assertEquals("visible2", layersAseIndexToName[2])

        val frameAseIndexToName = ase.default.frames.first().layerData.map { it.layer as ASE.AseLayer }
            .associate { it.originalAseIndex to it.name!! }

        assertEquals("visible1", frameAseIndexToName[0])
        assertEquals("visible2", frameAseIndexToName[2])
    }

    @Test
    fun readsVisibleAndHiddenLayers() = suspendTest({ !Platform.isJs }) {
        val ase = resourcesVfs["ase_tests/ase_with_layers.aseprite"].readImageDataContainer(
            ASE.toProps(ImageDecodingProps.DEFAULT(premultiplied = false)).apply {
                onlyReadVisibleLayers = false
            }
        )

        val aseIndexToName = ase.default.layers.map { it as ASE.AseLayer }
            .associate { it.originalAseIndex to it.name!! }
        assertEquals("visible1", aseIndexToName[0])
        assertEquals("hidden_layer", aseIndexToName[1])
        assertEquals("visible2", aseIndexToName[2])
        val frameAseIndexToName = ase.default.frames.first().layerData.map { it.layer as ASE.AseLayer }
            .associate { it.originalAseIndex to it.name!! }
        assertEquals("visible1", frameAseIndexToName[0])
        assertEquals("hidden_layer", frameAseIndexToName[1])
        assertEquals("visible2", frameAseIndexToName[2])
    }

    @Test
    fun testNinePatch() = suspendTest({ !Platform.isJs }) {
        val ase = resourcesVfs["ase_tests/9patch.aseprite"]
            .readImageDataContainer(ASE)

        val ninePatchSlice = ase
            .imageDatas.first()
            .frames.first()
            .first!!
            .ninePatchSlice!!

        assertEquals(
            NinePatchInfo.AxisInfo(ranges = listOf(Pair(false, 0..13), Pair(true, 14..17), Pair(false, 18..29)), totalLen = 30),
            ninePatchSlice.info.xaxis
        )
        assertEquals(
            NinePatchInfo.AxisInfo(ranges = listOf(Pair(false, 0..13), Pair(true, 14..17), Pair(false, 18..29)), totalLen = 30),
            ninePatchSlice.info.yaxis
        )
        //val ninePatchBmpSlice = resourcesVfs["Aseprite/9patch.aseprite"].readImageDataContainer(ASE).imageDatas.first().frames.first().first?.ninePatchSlice
        //ninePatch(ninePatchBmpSlice, Size(200, 100))
    }
}
