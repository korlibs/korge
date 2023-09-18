package korlibs.image.format

import korlibs.image.bitmap.*
import korlibs.image.color.*
import korlibs.io.async.*
import korlibs.io.stream.*
import korlibs.math.geom.*
import korlibs.platform.*
import kotlin.test.*

class NativeEncodingTest {
    @Test
    fun test() = suspendTest {
        if (Platform.isJsNodeJs) RegisteredImageFormats.register(PNG)
        val bytes = nativeImageFormatProvider.encodeSuspend(Bitmap32(10, 10, Colors.RED), ImageEncodingProps("image.png"))
        assertEquals(Size(10, 10), PNG.decodeHeader(bytes.openSync())!!.size)

        val image = nativeImageFormatProvider.decodeSuspend(bytes)
        assertEquals(Colors.RED, image.toBMP32()[0, 0])
    }
}
