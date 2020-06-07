package com.soywiz.korge.view

import com.soywiz.klock.*
import com.soywiz.korge.atlas.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.std.*
import kotlin.test.*

class SpriteAnimationTest {
    @Test
    fun test() = suspendTest {
        val animation = resourcesVfs["atlas/spritesheet.json"].readAtlas().getSpriteAnimation("RunRight", 100.milliseconds)
        assertEquals(4, animation.size)
        assertEquals("RunRight01.png", animation[0].name)
        assertEquals("RunRight02.png", animation[1].name)
        assertEquals("RunRight03.png", animation[2].name)
        assertEquals("RunRight04.png", animation[3].name)
    }
}
