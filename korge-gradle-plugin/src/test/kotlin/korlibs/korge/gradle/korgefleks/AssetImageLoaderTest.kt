package korlibs.korge.gradle.korgefleks

import korlibs.korge.gradle.typedresources.getResourceBytes
import korlibs.korge.gradle.util.ASEInfo
import org.junit.Assert
import org.junit.Test
import java.io.File

class AssetImageLoaderTest {
    @Test
    fun testImageFrameDuration() {
        val assetInfo = AssetInfo()
        val assetImageLoader = AssetImageLoader(
            asepriteExe = "aseprite",
            assetDir = File("."),
            exportTilesDir = File("."),
            exportTilesetDir = File("."),
            gameResourcesDir = File("."),
            assetInfoList = assetInfo
        )

        assetImageLoader.runAsepriteExport = {}  // In case of testing do not run export command
        assetImageLoader.loadAseInfo = { filename ->
            ASEInfo.getAseInfo(getResourceBytes(filename.name))
        }

        assetImageLoader.addImageAse("sprites.ase", listOf("Layer1"), listOf("TestNum", "FireTrail"), "sprites")
        // Frames:
        // TestNum: 1 - 10
        // FireTrail: 12 - 34
        val spriteTestnumFrames = assetInfo.images["sprites_TestNum"]?.frames ?: error("sprites_TestNum not found in sprites.ase")
        val spriteFiretrailFrames = assetInfo.images["sprites_FireTrail"]?.frames ?: error("sprites_FireTrail not found in sprites.ase")

        // Check now that frame durations were correctly read from aseprite file and inserted into the frames array
        Assert.assertEquals("Expect correct frame 0 duration", 242f, spriteTestnumFrames[0].duration)
        Assert.assertEquals("Expect correct frame 1 duration", 200f, spriteTestnumFrames[1].duration)
        Assert.assertEquals("Expect correct frame 2 duration", 200f, spriteTestnumFrames[8].duration)
        Assert.assertEquals("Expect correct frame 3 duration", 242f, spriteTestnumFrames[9].duration)

        Assert.assertEquals("Expect correct frame 0 duration", 40f, spriteFiretrailFrames[0].duration)
        Assert.assertEquals("Expect correct frame 1 duration", 41f, spriteFiretrailFrames[1].duration)
        Assert.assertEquals("Expect correct frame 2 duration", 42f, spriteFiretrailFrames[2].duration)
        Assert.assertEquals("Expect correct frame 3 duration", 40f, spriteFiretrailFrames[3].duration)
        Assert.assertEquals("Expect correct frame 20 duration", 40f, spriteFiretrailFrames[20].duration)
        Assert.assertEquals("Expect correct frame 21 duration", 41f, spriteFiretrailFrames[21].duration)
        Assert.assertEquals("Expect correct frame 22 duration", 42f, spriteFiretrailFrames[22].duration)
    }
}
