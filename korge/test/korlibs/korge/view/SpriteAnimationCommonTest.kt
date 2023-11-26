package korlibs.korge.view

import korlibs.image.bitmap.*
import korlibs.image.color.Colors
import korlibs.io.async.suspendTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SpriteAnimationCommonTest {
    @Test
    fun testSpriteAnimationGen() = suspendTest {
        val bmp = Bitmap32(32, 32, premultiplied = false).apply {
            fill(Colors.RED, 0, 0, 16, 16)
            fill(Colors.GREEN, 16, 0, 16, 16)
            fill(Colors.BLUE, 0, 16, 16, 16)
        }
        val anim1 = SpriteAnimation(bmp, spriteWidth = 16, spriteHeight = 16, rows = 2, columns = 2, numberOfFrames = 3, byRows = true)
        val anim2 = SpriteAnimation(bmp, spriteWidth = 16, spriteHeight = 16, rows = 2, columns = 2, numberOfFrames = 3, byRows = false)

        assertEquals(
            listOf(
                listOf(Colors.RED, Colors.GREEN, Colors.BLUE),
                listOf(Colors.RED, Colors.BLUE, Colors.GREEN),
            ),
            listOf(anim1, anim2).map { anim -> anim.map { slice -> slice.getRgba(0, 0) } }
        )
    }
}
