package korlibs.korge.view

import korlibs.graphics.log.AGLog
import korlibs.korge.render.RenderContext
import korlibs.korge.render.testRenderContext
import korlibs.math.geom.MSizeInt
import kotlin.test.*

class FixedSizeContainerTest {
    @Test
    @Ignore
    fun test() {
        testRenderContext {
            val windowSize = MSizeInt(640, 480)
            val virtualSize = MSizeInt(512, 512)
            val ag: AGLog = AGLog(windowSize.width, windowSize.height)
            val bp: BoundsProvider = BoundsProvider.Base()
            bp.setBoundsInfo(virtualSize.width, virtualSize.height, windowSize)
            val container = ClipContainer(30.0, 40.0).xy(110, 120)
            val log = arrayListOf<String>()
            container.addChild(object : View() {
                override fun renderInternal(ctx: RenderContext) {
                    log += "${ctx.batch.scissor}"
                }
            })
            testRenderContext(ag, bp) { ctx ->
                container.render(ctx)
                ctx.renderToTexture(400, 400, {
                    container.render(ctx)
                }) {
                }
            }
            assertEquals(
                """
                    Scissor(x=138, y=112, width=38, height=38)
                    Scissor(x=110, y=120, width=30, height=40)
                """.trimIndent(),
                log.joinToString("\n")
            )
        }
    }
}