package korlibs.image.atlas

import korlibs.io.async.*
import korlibs.io.file.std.*
import korlibs.platform.*
import kotlin.test.*

class AtlasInfoTest {
    @Test
    /* This test is reading a sprite atlas which was created by Aseprite version 1.2.24
     */
    fun name() = suspendTest({ Platform.isJvm }) {
        val atlas = resourcesVfs["atlas_info_aseprite_test.json"].readAtlas()
        // Check for layer info
        assertEquals("layer3d", atlas.info.meta.layers[0].name)
        assertEquals("layer_9", atlas.info.meta.layers[10].name)
        // Check for slices info
        assertEquals("Slice 1", atlas.info.meta.slices[0].name)
        assertEquals("Slice 334", atlas.info.meta.slices[333].name)
        // Check for frame tags info
        assertEquals("default", atlas.info.meta.frameTags[0].name)
    }
}
