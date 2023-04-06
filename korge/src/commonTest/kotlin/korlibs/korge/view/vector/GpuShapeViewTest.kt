package korlibs.korge.view.vector

import korlibs.image.color.*
import korlibs.image.vector.*
import korlibs.korge.annotations.*
import korlibs.korge.render.*
import kotlin.test.*

@OptIn(KorgeExperimental::class)
class GpuShapeViewTest {
    @Test
    fun testShapeIsUpdated() {
        val view = GpuShapeView(EmptyShape)
        assertIs<EmptyShape>(view.shape)
        view.updateShape {
            stroke(createLinearGradient(0, 0, 0, 100).add(0.0, Colors.WHITE).add(1.0, Colors.RED), lineWidth = 10f) {
                rect(0, 0, 100, 100)
            }
        }
        assertIs<PolylineShape>(view.shape)
    }

    @Test
    @Ignore
    fun testShapeWithoutApplyScissorDisableScissoring() {
        val view = GpuShapeView(EmptyShape)
        view.applyScissor = false
        view.updateShape { stroke(Colors.RED, lineWidth = 2f) { rect(0, 0, 100, 100) } }
        val ag = testRenderContext {
            view.render(it)
        }
        assertTrue { ag.log.contains("disable: SCISSOR") }
    }
}
