package korlibs.korge.view

import korlibs.graphics.*
import korlibs.graphics.log.AGBaseLog
import korlibs.korge.tests.ViewsForTesting
import korlibs.image.color.Colors
import korlibs.math.geom.*
import kotlin.test.Test
import kotlin.test.assertEquals

class ViewFixedSizeContainerTest : ViewsForTesting(
    windowSize = SizeInt(1280, 720),
    virtualSize = SizeInt(640, 480)
) {
    @Test
    fun testClipContainerScissors() = viewsTest {
        clipContainer(100, 100) {
            xy(50, 70)
            solidRect(20, 20, Colors.RED).xy(-10, -10)
        }
        delayFrame()
        val log = arrayListOf<Any?>()
        testRenderContext(object : AGBaseLog() {
            override fun execute(command: AGCommand) {
                super.execute(command)
                when (command) {
                    is AGBatch -> {
                        log += command.scissor.toRectOrNull()
                    }
                    else -> Unit
                }
            }
        }) {
            stage.render(it)
        }

        assertEquals(listOf<Any?>(Rectangle(234, 105, 150, 150)), log)
    }
}
