package korlibs.korge.view

import korlibs.image.color.*
import korlibs.korge.scene.*
import korlibs.korge.testing.*
import korlibs.korge.view.filter.*
import korlibs.math.geom.*
import kotlin.test.*

class TransitionViewTest {
    @Test
    fun test() = korgeScreenshotTest(SizeInt(50, 50)) {
        val tv = TransitionView()
        tv.startNewTransition(DummyView())
        tv.startNewTransition(Container().apply {
            clipContainer(Size(50, 50)) {
                solidRect(50, 50, Colors.BLUE)
            }
        }, MaskTransition(TransitionFilter.Transition.CIRCULAR))
        tv.ratio = 0.5f
        addChild(tv)

        assertScreenshot(posterize = 5)
        tv.ratio = 0.9f
        assertScreenshot(posterize = 5)
        tv.ratio = 1.0f
        assertScreenshot(posterize = 5)
    }

}
