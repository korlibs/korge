package korlibs.image.format

import korlibs.image.bitmap.*
import korlibs.io.async.*
import korlibs.io.file.std.*
import kotlin.test.*

class HEICTest {
    @Test
    fun test() = suspendTest {
        val heic = resourcesVfs["heic.heic"].readBitmap().toBMP32().premultiplied()
        val png = resourcesVfs["heic.heic.png"].readBitmap(PNG.toProps()).toBMP32().premultiplied()
        //localVfs("/tmp/heic.heic.png").writeBitmap(heic, PNG)
        assertGreaterOrEqual(30.0, Bitmap32.computePsnr(heic, png), message = "PSNR")
    }

    fun assertGreaterOrEqual(expected: Double, actual: Double, message: String = "") {
        assertTrue("Expected $actual >= $expected : $message") { actual >= expected }
    }
}
