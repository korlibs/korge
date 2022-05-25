import com.soywiz.korge.scene.MaskTransition
import com.soywiz.korge.scene.TransitionView
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.DummyView
import com.soywiz.korge.view.Stage
import com.soywiz.korge.view.clipContainer
import com.soywiz.korge.view.container
import com.soywiz.korge.view.filter
import com.soywiz.korge.view.filter.IdentityFilter
import com.soywiz.korge.view.filter.TransitionFilter
import com.soywiz.korge.view.image
import com.soywiz.korge.view.solidRect
import com.soywiz.korge.view.xy
import com.soywiz.korim.color.Colors
import com.soywiz.korim.format.readBitmap
import com.soywiz.korio.file.std.resourcesVfs

suspend fun Stage.mainClipping() {
    image(resourcesVfs["korge.png"].readBitmap()).xy(-50, 0)

    clipContainer(100, 100) {
        xy(50, 70)
        solidRect(20, 20, Colors.RED).xy(-10, -10)
    }

    container {
        xy(200, 200)
        filter = IdentityFilter.Linear
        clipContainer(100, 100) {
            xy(150, 70)
            clipContainer(40, 40) {
                filter = IdentityFilter.Linear
                solidRect(20, 20, Colors.GREEN).xy(-10, -10)
            }
        }
    }

    val tv = TransitionView()
    tv.xy(400, 100)
    tv.startNewTransition(DummyView())
    tv.startNewTransition(Container().apply {
        clipContainer(512, 512) {
            solidRect(512, 512, Colors.BLUE)
        }
    }, MaskTransition(TransitionFilter.Transition.CIRCULAR))
    tv.ratio = 0.5
    addChild(tv)
}
