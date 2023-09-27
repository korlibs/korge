package korlibs.image.format

import korlibs.time.milliseconds
import korlibs.io.async.suspendTestNoBrowser
import korlibs.io.file.std.resourcesVfs
import kotlin.test.Test
import kotlin.test.assertEquals

class GIFTest {
    val GIFProps = ImageDecodingProps(format = GIF)

    @Test
    fun test() = suspendTestNoBrowser {
        val data = resourcesVfs["small-animated-gif-images-2.gif"].readImageData(GIFProps)
        assertEquals(500, data.width)
        assertEquals(500, data.height)
        assertEquals(3, data.frames.size)
        assertEquals(10.milliseconds, data.frames[0].duration)
        assertEquals(10.milliseconds, data.frames[1].duration)
        assertEquals(10.milliseconds, data.frames[2].duration)
        //for (frame in data.frames) frame.bitmap.showImageAndWait()
    }

    @Test
    fun testIssue636() = suspendTestNoBrowser {
        val data = resourcesVfs["200.gif"].readImageData(GIFProps)
        assertEquals(30.milliseconds, data.defaultAnimation.frames.first().duration)

        //for (frame in data.frames) frame.bitmap.showImageAndWait()
    }
}
