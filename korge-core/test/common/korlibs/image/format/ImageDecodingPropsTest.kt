package korlibs.image.format

import kotlin.test.Test
import kotlin.test.assertEquals

class ImageDecodingPropsTest {
    @Test
    fun testGetSampleSize() {
        val props = ImageDecodingProps(requestedMaxSize = 1024)
        assertEquals(1, props.getSampleSize(1024, 1024))
        assertEquals(2, props.getSampleSize(2048, 2048))
        assertEquals(4, props.getSampleSize(4096, 4096))
        assertEquals(4, props.getSampleSize(1024, 3000))
        assertEquals(4, props.getSampleSize(3000, 3000))
    }
}
