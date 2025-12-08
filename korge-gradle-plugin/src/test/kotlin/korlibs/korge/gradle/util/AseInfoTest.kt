package korlibs.korge.gradle.util

import korlibs.korge.gradle.typedresources.getResourceBytes
import org.junit.Assert
import org.junit.Test

class AseInfoTest {
    @Test
    fun test() {
        val info = ASEInfo.Companion.getAseInfo(getResourceBytes("sprites.ase"))
        Assert.assertEquals(0, info.slices.size)
        Assert.assertEquals(listOf("TestNum", "FireTrail", "FireTrail2"), info.tags.map { it.tagName })
    }

    @Test
    fun testSliceInfo() {
        val info = ASEInfo.getAseInfo(getResourceBytes("sprite-slices.ase"))
        Assert.assertEquals("Expected 2 slices", 2, info.slices.size)
        Assert.assertEquals("Slice 1 name wrong", "Slice 1", info.slices[0].sliceName)
        Assert.assertEquals("Slice 2 name wrong","Slice 2", info.slices[1].sliceName)
        Assert.assertEquals("Expected 2 keys in slice 0", 2, info.slices[0].keys.size)
        Assert.assertEquals("Expected 3 keys in slice 1", 3, info.slices[1].keys.size)

        Assert.assertEquals("Image pixel width wrong", 32, info.pixelWidth)
        Assert.assertEquals("Image pixel height wrong", 64, info.pixelHeight)
        Assert.assertEquals("Frame number of slice 0 key 0 wrong", 0, info.slices[0].keys[0].frameNumber)
        Assert.assertEquals("Frame number of slice 0 key 1 wrong", 2, info.slices[0].keys[1].frameNumber)
        Assert.assertEquals("Frame number of slice 1 key 0 wrong", 0, info.slices[1].keys[0].frameNumber)
        Assert.assertEquals("Frame number of slice 1 key 1 wrong", 1, info.slices[1].keys[1].frameNumber)
        Assert.assertEquals("Frame number of slice 1 key 2 wrong", 2, info.slices[1].keys[2].frameNumber)

        Assert.assertEquals("Slice x of slice 0 key 0 wrong", 3, info.slices[0].keys[0].x)
        Assert.assertEquals("Slice y of slice 0 key 0 wrong", 4, info.slices[0].keys[0].y)
        Assert.assertEquals("Slice width of slice 0 key 0 wrong", 24, info.slices[0].keys[0].width)
        Assert.assertEquals("Slice height of slice 0 key 0 wrong", 20, info.slices[0].keys[0].height)
        Assert.assertEquals("Slice nine-patch centerX of slice 0 key 0 wrong", 4, info.slices[0].keys[0].ninePatch?.centerX)
        Assert.assertEquals("Slice nine-patch centerY of slice 0 key 0 wrong", 5, info.slices[0].keys[0].ninePatch?.centerY)
        Assert.assertEquals("Slice nine-patch centerWidth of slice 0 key 0 wrong", 6, info.slices[0].keys[0].ninePatch?.centerWidth)
        Assert.assertEquals("Slice nine-patch centerHeight of slice 0 key 0 wrong", 7, info.slices[0].keys[0].ninePatch?.centerHeight)
        Assert.assertEquals("Slice pivotX of slice 0 key 0 wrong", 12, info.slices[0].keys[0].pivot?.pivotX)
        Assert.assertEquals("Slice pivotY of slice 0 key 0 wrong", 34, info.slices[0].keys[0].pivot?.pivotY)

        Assert.assertEquals("Slice pivotX of slice 0 key 1 wrong", 16, info.slices[0].keys[1].pivot?.pivotX)
        Assert.assertEquals("Slice pivotY of slice 0 key 1 wrong", 24, info.slices[0].keys[1].pivot?.pivotY)

        Assert.assertEquals("Frame 0 index wrong", 0, info.frames[0].index)
        Assert.assertEquals("Frame 0 duration wrong", 100, info.frames[0].duration)
        Assert.assertEquals("Frame 1 index wrong", 1, info.frames[1].index)
        Assert.assertEquals("Frame 1 duration wrong", 80, info.frames[1].duration)
        Assert.assertEquals("Frame 2 index wrong", 2, info.frames[2].index)
        Assert.assertEquals("Frame 2 duration wrong", 42, info.frames[2].duration)
    }
}
