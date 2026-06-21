package korlibs.korge.view

import korlibs.graphics.*
import korlibs.graphics.log.*
import korlibs.image.color.*
import korlibs.korge.tests.*
import korlibs.math.geom.*
import kotlin.test.*

class ViewFixedSizeContainerTest : ViewsForTesting(
    windowSize = Size(1280, 720),
    virtualSize = Size(640, 480)
) {
    @Test
    fun testClipContainerScissors() = viewsTest {
        clipContainer(Size(100, 100)) {
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
