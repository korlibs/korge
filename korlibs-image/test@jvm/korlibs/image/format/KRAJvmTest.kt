package korlibs.image.format

import korlibs.io.async.*
import korlibs.io.file.std.*
import kotlin.test.*

class KRAJvmTest {
    @Test
    fun test() = suspendTest {
        val output = resourcesVfs["krita.kra"].readImageData(ImageDecodingProps(format = KRA).also {
            //it.kritaPartialImageLayers = true
            it.kritaPartialImageLayers = false
            it.kritaLoadLayers = true
        })
        assertEquals(4, output.frames.size)
        //output.showImagesAndWait()
    }
}
