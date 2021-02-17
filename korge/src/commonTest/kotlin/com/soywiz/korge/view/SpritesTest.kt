package com.soywiz.korge.view

import com.soywiz.klock.*
import com.soywiz.korge.tests.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.format.*
import com.soywiz.korio.file.std.*
import kotlinx.coroutines.*
import kotlin.test.*

class SpritesTest : ViewsForTesting() {
    lateinit var countDownSpriteMap: Bitmap32
    lateinit var digits: Array<Bitmap>
    lateinit var countDownAnimation: SpriteAnimation
    lateinit var countDownSprite: Sprite

    // MUST BE CALLED BEFORE EACH TEST
    suspend fun Stage.setup() {
        countDownSpriteMap = resourcesVfs["countDown.png"].readBitmap().toBMP32()
        //digits = Array<Bitmap>(10) { countDownSpriteMap.extract(it * 24, 0, 24, 36) }
        countDownAnimation = SpriteAnimation(countDownSpriteMap, 24, 36, 0, 0, 10, 1, 0, 0)
        countDownSprite = sprite(countDownAnimation)
    }

    val logs = arrayListOf<String>()
    suspend fun Stage.testPlayAnimation(
        triggerFrames: Boolean = false,
        autocompleteOnStop: Boolean = true,
        block: suspend CompletableDeferred<Unit>.() -> Unit
    ) {
        setup()
        deferred {
            countDownSprite.onAnimationStopped {
                logs.add("stopped")
                if (autocompleteOnStop) complete(Unit)
            }
            countDownSprite.onAnimationStarted { logs.add("started") }
            countDownSprite.onAnimationCompleted {
                logs.add("completed")
            }
            if (triggerFrames) {
                countDownSprite.onFrameChanged { logs.add("${countDownSprite.currentSpriteIndex}") }
            }
            block(this)
        }
    }

    // @TODO: Shouldn't the default value end with frame 9?
    @Test
    fun testSpriteAnimationPlay1Times() = viewsTest {
        testPlayAnimation(triggerFrames = true) {
            countDownSprite.playAnimation(1)
        }
        assertEquals(
            "started,1,2,3,4,5,6,7,8,9,stopped,completed,0",
            logs.joinToString(",")
        )
        assertEquals(10, countDownSprite.totalFramesPlayed)
        assertEquals(countDownSprite.bitmap, countDownAnimation[0])
    }

    @Test
    fun testSpriteAnimationPlay1TimesReversed() = viewsTest {
        testPlayAnimation(triggerFrames = true) {
            countDownSprite.playAnimation(1, reversed = true)
        }
        assertEquals(
            "started,9,8,7,6,5,4,3,2,1,stopped,completed,0",
            logs.joinToString(",")
        )
        assertEquals(10, countDownSprite.totalFramesPlayed)
        assertEquals(countDownSprite.bitmap, countDownAnimation[0])
    }


    @Test
    fun testSpriteAnimationPlay1TimesM1LastFrame() = viewsTest {
        testPlayAnimation(triggerFrames = true) {
            // @TODO: Shouldn't be 1 times?
            countDownSprite.playAnimation(0, endFrame = -1)
        }
        assertEquals(
            "started,1,2,3,4,5,6,7,8,stopped,completed,9",
            logs.joinToString(",")
        )
        assertEquals(9, countDownSprite.totalFramesPlayed)
        assertEquals(countDownSprite.bitmap, countDownAnimation[9])
    }

    @Test
    fun testSpriteAnimationPlay346Times() = viewsTest(timeout = 100.seconds) {
        testPlayAnimation {
            countDownSprite.playAnimation(1000)
        }
        assertEquals("started,stopped,completed", logs.joinToString(","))
        assertEquals(10000, countDownSprite.totalFramesPlayed)
        assertEquals(countDownSprite.bitmap, countDownAnimation[0])
    }

    @Test
    fun testSpriteAnimationPlay() = viewsTest {
        testPlayAnimation {
            countDownSprite.playAnimation()
        }
        assertEquals("started,stopped,completed", logs.joinToString(","))
        assertEquals(10, countDownSprite.totalFramesPlayed)
        assertEquals(countDownSprite.bitmap, countDownAnimation[0])
    }

    @Test
    fun testSpriteAnimationStartFrame() = viewsTest {
        testPlayAnimation {
            countDownSprite.playAnimation(startFrame = 3, times = 10)
        }
        assertEquals("started,stopped,completed", logs.joinToString(","))
        assertEquals(107, countDownSprite.totalFramesPlayed)
        assertEquals(countDownSprite.bitmap, countDownAnimation[0])
    }

    @Test
    fun testSpriteAnimationReversed() = viewsTest {
        testPlayAnimation {
            countDownSprite.playAnimation(reversed = true, times = 1000)
        }
        assertEquals("started,stopped,completed", logs.joinToString(","))
        assertEquals(10000, countDownSprite.totalFramesPlayed)
        assertEquals(countDownSprite.bitmap, countDownAnimation[0])
    }

    @Test
    fun testSpriteAnimationReversedStartFrame() = viewsTest {
        testPlayAnimation {
            countDownSprite.playAnimation(reversed = true, times = 1000, startFrame = 7)
        }
        assertEquals("started,stopped,completed", logs.joinToString(","))
        assertEquals(10007, countDownSprite.totalFramesPlayed)
        assertEquals(countDownSprite.bitmap, countDownAnimation[0])
    }

    @Test
    fun testSpriteAnimationTimesAndSpriteTimeDisplay() = viewsTest {
        testPlayAnimation {
            countDownSprite.playAnimation(times = 1, spriteDisplayTime = 100.milliseconds)
        }
        assertEquals("started,stopped,completed", logs.joinToString(","))
        assertEquals(10, countDownSprite.totalFramesPlayed)
        assertEquals(countDownSprite.bitmap, countDownAnimation[0])
    }

    @Test
    fun testSpriteAnimationEndFrame() = viewsTest {
        testPlayAnimation {
            countDownSprite.playAnimation(times = 1, endFrame = 3)
        }
        assertEquals("started,stopped,completed", logs.joinToString(","))
        assertEquals(13, countDownSprite.totalFramesPlayed)
        assertEquals(countDownSprite.bitmap, countDownAnimation[3])
    }

    @Test
    fun testSpriteAnimationEndFrameReversed() = viewsTest {
        testPlayAnimation {
            countDownSprite.playAnimation(times = 2, reversed = true, endFrame = 6)
        }
        assertEquals("started,stopped,completed", logs.joinToString(","))
        assertEquals(24, countDownSprite.totalFramesPlayed)
        assertEquals(countDownSprite.bitmap, countDownAnimation[6])
    }

    @Test
    fun testSpriteAnimationEndFrameAndStartFrame() = viewsTest {
        testPlayAnimation {
            countDownSprite.playAnimation(times = 2, startFrame = 9, endFrame = 2)
        }
        assertEquals("started,stopped,completed", logs.joinToString(","))
        assertEquals(23, countDownSprite.totalFramesPlayed)
        assertEquals(countDownSprite.bitmap, countDownAnimation[2])
    }

    @Test
    fun testSpriteAnimationEndFrameAndStartFrame2() = viewsTest {
        testPlayAnimation {
            countDownSprite.playAnimation(times = 0, startFrame = 2, endFrame = 1)
        }
        assertEquals("started,stopped,completed", logs.joinToString(","))
        assertEquals(9, countDownSprite.totalFramesPlayed)
        assertEquals(countDownSprite.bitmap, countDownAnimation[1])
    }

    @Test
    fun testSpriteAnimationEndFrameAndStartFrameReversed() = viewsTest {
        testPlayAnimation {
            countDownSprite.playAnimation(times = 2, startFrame = 9, endFrame = 2, reversed = true)
        }
        assertEquals("started,stopped,completed", logs.joinToString(","))
        assertEquals(27, countDownSprite.totalFramesPlayed)
        assertEquals(countDownSprite.bitmap, countDownAnimation[2])
    }

    @Test
    fun testSpriteAnimationEndFrameAndStartFrameReversed2() = viewsTest {
        testPlayAnimation {
            countDownSprite.playAnimation(times = 2, startFrame = 1, endFrame = 9, reversed = true)
        }
        assertEquals("started,stopped,completed", logs.joinToString(","))
        assertEquals(22, countDownSprite.totalFramesPlayed)
        assertEquals(countDownSprite.bitmap, countDownAnimation[9])
    }
}

// Workaround for getting Bitmap-Copy out of BmpSlice
fun BmpSlice.extract(): Bitmap = bmpBase.extract(left, top, width, height)
