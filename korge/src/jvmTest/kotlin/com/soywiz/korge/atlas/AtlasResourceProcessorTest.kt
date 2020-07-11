package com.soywiz.korge.atlas

import com.soywiz.korge.resources.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.std.*
import kotlinx.coroutines.channels.*
import org.junit.*
import org.junit.Test
import kotlin.test.*

class AtlasResourceProcessorTest {
    @Test
    fun name() = suspendTest {
        val memoryVfs = MemoryVfs()
        memoryVfs["atlas"].mkdir()
        val processed1 = AtlasResourceProcessor.process(resourcesVfs["atlas/simple.atlas"], memoryVfs)
        println(memoryVfs.listRecursive().toList())
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
        val processed2 = AtlasResourceProcessor.process(resourcesVfs["atlas/simple.atlas"], memoryVfs)
        assertEquals(false, processed2)

        memoryVfs["atlas/simple.atlas.json"].readAtlas()
    }
}
