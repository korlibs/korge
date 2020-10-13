package com.soywiz.korge.atlas

import com.soywiz.korim.atlas.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.util.OS
import com.soywiz.korma.geom.*
import kotlin.test.*

class AtlasInfoTest {
    @Test
    fun name() = suspendTest({ OS.isJvm }) {
        val atlas = AtlasInfo.loadJsonSpriter(resourcesVfs["demo.json"].readString())
        assertEquals("Spriter", atlas.app)
        assertEquals("r10", atlas.version)
        assertEquals("demo.png", atlas.image)
        assertEquals("RGBA8888", atlas.format)
        assertEquals(124, atlas.frames.size)
        assertEquals(1.0, atlas.scale)
        assertEquals(Size(1021, 1003), atlas.size.size)

        val firstFrame = atlas.frames.first()
        assertEquals("arms/forearm_jump_0.png", atlas.frames.map { it.name }.first())
        assertEquals(Rectangle(993, 319, 41, 28), firstFrame.frame.rect)
        assertEquals(Size(55, 47), firstFrame.sourceSize.size)
        assertEquals(Rectangle(8, 7, 41, 28), firstFrame.spriteSourceSize.rect)
        assertEquals(true, firstFrame.rotated)
        assertEquals(true, firstFrame.trimmed)
    }
}
