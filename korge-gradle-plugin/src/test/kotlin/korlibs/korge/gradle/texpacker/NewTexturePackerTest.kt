package korlibs.korge.gradle.texpacker

import korlibs.korge.gradle.typedresources.getResourceURL
import org.junit.Assert
import org.junit.Test
import java.io.File


class NewTexturePackerTest {
    @Test
    fun testPackTilesets() {
        val atlasInfos = NewTexturePacker.packTilesets(File(getResourceURL("tilesets").file))

        // Check if duplicate tiles were detected and merged into one tile rect area in the tileset atlas
        val atlasInfo = atlasInfos.first()
        val frames = atlasInfo.info["frames"] as Map<String, Any>

        val tile_1 = frames["test_tileset_1"] as Map<String, Any>
        val tile_2 = frames["test_tileset_4"] as Map<String, Any>
        val tile_3 = frames["test_tileset_2_0"] as Map<String, Any>  // This tile is in the second tileset but is a duplicate of test_tileset_1

        // Check that all three tiles point to the same frame data (i.e., they are duplicates)
        val tileFrame_1 = tile_1["frame"] as Map<String, Int>
        val tileFrame_2 = tile_2["frame"] as Map<String, Int>
        val tileFrame_3 = tile_3["frame"] as Map<String, Int>
        Assert.assertEquals(tileFrame_1["x"], tileFrame_2["x"])
        Assert.assertEquals(tileFrame_1["y"], tileFrame_2["y"])
        Assert.assertEquals(tileFrame_1["w"], tileFrame_2["w"])
        Assert.assertEquals(tileFrame_1["h"], tileFrame_2["h"])
        Assert.assertEquals(tileFrame_1["x"], tileFrame_3["x"])
        Assert.assertEquals(tileFrame_1["y"], tileFrame_3["y"])
        Assert.assertEquals(tileFrame_1["w"], tileFrame_3["w"])
        Assert.assertEquals(tileFrame_1["h"], tileFrame_3["h"])

        // Check that tiles are not rotated or trimmed
        val rotated_1 = tile_1["rotated"] as Boolean
        val rotated_2 = tile_2["rotated"] as Boolean
        val rotated_3 = tile_3["rotated"] as Boolean
        Assert.assertFalse(rotated_1)
        Assert.assertFalse(rotated_2)
        Assert.assertFalse(rotated_3)
        val trimmed_1 = tile_1["trimmed"] as Boolean
        val trimmed_2 = tile_2["trimmed"] as Boolean
        val trimmed_3 = tile_3["trimmed"] as Boolean
        Assert.assertFalse(trimmed_1)
        Assert.assertFalse(trimmed_2)
        Assert.assertFalse(trimmed_3)

        //println("Atlas Info: $atlasInfos")
    }
}
