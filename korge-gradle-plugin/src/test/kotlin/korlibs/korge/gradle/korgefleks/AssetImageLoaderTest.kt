package korlibs.korge.gradle.korgefleks

import korlibs.korge.gradle.korgefleks.AssetConfig.Companion.IMAGES
import korlibs.korge.gradle.typedresources.getResourceBytes
import korlibs.korge.gradle.util.ASEInfo
import org.junit.Assert
import org.junit.Test
import java.io.File

class AssetImageLoaderTest {
    @Test
    fun testImageFrameDuration() {
        val assetInfo = linkedMapOf<String, Any>()
        assetInfo[IMAGES] = linkedMapOf<String, Any>()

        val assetImageAseExporter = AssetImageAseExporter(
            asepriteExe = "aseprite",
            assetDir = File("."),
            exportTilesDir = File("."),
            assetInfo = assetInfo
        )

        assetImageAseExporter.runAsepriteExport = {}  // In case of testing do not run export command
        assetImageAseExporter.getFile = { File(it) }
        assetImageAseExporter.loadAseInfo = { filename ->
            ASEInfo.getAseInfo(getResourceBytes(filename.name))
        }

        assetImageAseExporter.addImageAse("sprites.ase", listOf("Layer1"), listOf("TestNum", "FireTrail"), "sprites")
        // Frames:
        // TestNum: 1 - 10
        // FireTrail: 12 - 34
        val spriteTestnumFrames = (((assetInfo[IMAGES] as Map<String, Any>)["sprites_TestNum"] as Map<String, Any>)["fs"] as List<Map<String, Int>>)
        val spriteFiretrailFrames = (((assetInfo[IMAGES] as Map<String, Any>)["sprites_FireTrail"] as Map<String, Any>)["fs"] as List<Map<String, Int>>)

        // Check now that frame durations were correctly read from aseprite file and inserted into the frames array
        Assert.assertEquals("Expect correct \"Testnum\" frame 0 duration", 242, spriteTestnumFrames[0]["d"])
        Assert.assertEquals("Expect correct \"Testnum\" frame 1 duration", 200, spriteTestnumFrames[1]["d"])
        Assert.assertEquals("Expect correct \"Testnum\" frame 2 duration", 200, spriteTestnumFrames[8]["d"])
        Assert.assertEquals("Expect correct \"Testnum\" frame 3 duration", 242, spriteTestnumFrames[9]["d"])

        Assert.assertEquals("Expect correct \"Firetrail\" frame 0 duration", 40, spriteFiretrailFrames[0]["d"])
        Assert.assertEquals("Expect correct \"Firetrail\" frame 1 duration", 41, spriteFiretrailFrames[1]["d"])
        Assert.assertEquals("Expect correct \"Firetrail\" frame 2 duration", 42, spriteFiretrailFrames[2]["d"])
        Assert.assertEquals("Expect correct \"Firetrail\" frame 3 duration", 40, spriteFiretrailFrames[3]["d"])
        Assert.assertEquals("Expect correct \"Firetrail\" frame 20 duration", 40, spriteFiretrailFrames[20]["d"])
        Assert.assertEquals("Expect correct \"Firetrail\" frame 21 duration", 41, spriteFiretrailFrames[21]["d"])
        Assert.assertEquals("Expect correct \"Firetrail\" frame 22 duration", 42, spriteFiretrailFrames[22]["d"])
    }
}
