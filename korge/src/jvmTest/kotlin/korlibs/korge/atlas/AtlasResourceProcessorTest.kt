package korlibs.korge.atlas

import korlibs.logger.*
import korlibs.memory.*
import korlibs.korge.resources.*
import korlibs.image.atlas.*
import korlibs.io.async.*
import korlibs.io.file.std.*
import korlibs.io.util.*
import kotlinx.coroutines.flow.toList
import org.junit.Test
import kotlin.test.*

class AtlasResourceProcessorTest {
    val logger = Logger("AtlasResourceProcessorTest")

    // @TODO: Do not test on windows since CI is checking out files converting \n to \r\n and SHA-1 file is different because of this :/
    @Test
    fun name() = suspendTest({ !Platform.isWindows }) {
        val memoryVfs = MemoryVfs()
        memoryVfs["atlas"].mkdir()
        val processed1 = AtlasResourceProcessor.process(resourcesVfs["atlas/simple.atlas"], memoryVfs["atlas/simple.atlas"])
        logger.info { memoryVfs.listRecursive().toList() }
        assertEquals(true, processed1)
        assertEquals(true, memoryVfs["atlas/simple.atlas.json"].exists())
        assertEquals(true, memoryVfs["atlas/simple.atlas.png"].exists())
        assertEquals(
            ResourceVersion(
                name = "simple.atlas",
                loaderVersion = 0,
                sha1 = "2001b3a895e3d9a8bee7267928a93941b6aee181",
                configSha1 = ""
            ),
            ResourceVersion.readMeta(memoryVfs["atlas/simple.atlas.json.meta"])
        )
        val processed2 = AtlasResourceProcessor.process(resourcesVfs["atlas/simple.atlas"], memoryVfs["atlas/simple.atlas"])
        assertEquals(false, processed2)

        memoryVfs["atlas/simple.atlas.json"].readAtlas()
    }
}