package samples

import com.soywiz.korge.component.onStageResized
import com.soywiz.korge.scene.MaskTransition
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.scene.TransitionView
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.DummyView
import com.soywiz.korge.view.SContainer
import com.soywiz.korge.view.clipContainer
import com.soywiz.korge.view.container
import com.soywiz.korge.view.filter.filter
import com.soywiz.korge.view.filter.IdentityFilter
import com.soywiz.korge.view.filter.TransitionFilter
import com.soywiz.korge.view.image
import com.soywiz.korge.view.solidRect
import com.soywiz.korge.view.unsafeRenderToBitmapSync
import com.soywiz.korge.view.vector.gpuShapeView
import com.soywiz.korge.view.xy
import com.soywiz.korim.color.Colors
import com.soywiz.korim.format.readBitmap
import com.soywiz.korio.file.std.resourcesVfs
import com.soywiz.korma.geom.vector.rect

class MainClipping : Scene() {
    override suspend fun SContainer.sceneMain() {
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

        gpuShapeView({
            stroke(Colors.GREEN, lineWidth = 2.0) {
                rect(0.0, 0.0, views.virtualWidthDouble, views.virtualHeightDouble)
            }
        })

        run {
            //val escale = 1.1
            val escale = 1.0
            val container = Container().apply {
                y = views.virtualHeightDouble; scaleY = -1.0 * escale; scaleX = escale
                clipContainer(150, 100) {
                    xy(75, 50)
                    solidRect(300, 400)
                }
            }
            addChild(container)
            container {
                val container2 = this
                // This shouldn't be needed since Stage.localMatrix is always the identity
                onStageResized { width, height ->
                    //println("resized=$width, $height")
                    container2.removeChildren()
                    container2.addChild(image(container.unsafeRenderToBitmapSync(views.renderContext).also {
                        it.updateColors { if (it.a == 0) Colors.RED else it }
                    }).also {
                        it.x = 300.0
                        it.y = views.virtualHeightDouble - it.bitmap.height - 50 * escale
                    })
                }
            }
        }
    }
}
