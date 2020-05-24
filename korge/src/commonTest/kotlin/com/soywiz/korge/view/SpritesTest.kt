package com.soywiz.korge.view

import com.soywiz.klock.*
import com.soywiz.korge.tests.*
import com.soywiz.korge.tiled.*
import com.soywiz.korge.view.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.format.readBitmap
import com.soywiz.korim.format.writeBitmap
import com.soywiz.korio.async.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.lang.*
import com.soywiz.korma.geom.*
import kotlinx.coroutines.*
import kotlin.jvm.*
import kotlin.test.*

class SpritesTest : ViewsForTesting() {
    lateinit var countDownSpriteMap : Bitmap32
    lateinit var digits: Array<Bitmap>
    lateinit var countDownAnimation : SpriteAnimation
    lateinit var countDownSprite : Sprite

    // MUST BE CALLED BEFORE EACH TEST
    suspend fun setup(){
        async(Dispatchers.Default){
            countDownSpriteMap = resourcesVfs["gfx/countDown.png"].readBitmap().toBMP32()
            digits = Array<Bitmap>(10) {
                countDownSpriteMap.extract(it * 24, 0, 24, 36)
            }

            countDownAnimation = SpriteAnimation(countDownSpriteMap, 24, 36, 0, 0, 10, 1, 0, 0)
            countDownSprite = Sprite(countDownAnimation)
        }.await()
    }

    @Test
    fun testSpriteAnimationPlay1Times() = viewsTest {
        setup()
        countDownSprite.playAnimation(1)
        assert(countDownSprite.bitmap.extract().contentEquals(digits[0]))
    }

    @Test
    fun testSpriteAnimationPlay346Times() = viewsTest {
        setup()
        countDownSprite.playAnimation(346)
        assert(countDownSprite.bitmap.extract().contentEquals(digits[0]))
    }

    @Test
    fun testSpriteAnimationPlay() = viewsTest {
        setup()
        countDownSprite.playAnimation()
        assert(countDownSprite.bitmap.extract().contentEquals(digits[0]))
    }

    @Test
    fun testSpriteAnimationStartFrame() = viewsTest {
        setup()
        countDownSprite.playAnimation(startFrame = 3, times = 10)
        assert(countDownSprite.bitmap.extract().contentEquals(digits[3]))
    }

    @Test
    fun testSpriteAnimationReversed() = viewsTest {
        setup()
        countDownSprite.playAnimation(reversed = true, times = 1000)
        assert(countDownSprite.bitmap.extract().contentEquals(digits[0]))
    }

    @Test
    fun testSpriteAnimationReversedStartFrame() = viewsTest {
        setup()
        countDownSprite.playAnimation(reversed = true, times = 1000, startFrame = 7)
        assert(countDownSprite.bitmap.extract().contentEquals(digits[7]))
    }

    @Test
    fun testSpriteAnimationTimesAndSpriteTimeDisplay() = viewsTest {
        setup()
        countDownSprite.playAnimation(times = 1, spriteDisplayTime = 100.milliseconds)
        assert(countDownSprite.bitmap.extract().contentEquals(digits[0]))
    }
}

// Workaround for getting Bitmap-Copy out of BmpSlice
fun BmpSlice.extract(): Bitmap = bmp.extract(left, top, width, height)
