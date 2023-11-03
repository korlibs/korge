package samples

import korlibs.image.color.*
import korlibs.image.format.*
import korlibs.io.async.*
import korlibs.io.file.std.*
import korlibs.korge.scene.*
import korlibs.korge.view.*
import korlibs.korge.view.filter.*
import korlibs.korge.view.vector.*
import korlibs.math.geom.*
import korlibs.math.interpolation.*

class MainClipping : Scene() {
    override suspend fun SContainer.sceneMain() {
        image(resourcesVfs["korge.png"].readBitmap()).xy(-50, 0)

        clipContainer(Size(100, 100)) {
            xy(50, 70)
            solidRect(20, 20, Colors.RED).xy(-10, -10)
        }

        container {
            xy(200, 200)
            filter = IdentityFilter.Linear
            clipContainer(Size(100, 100)) {
                xy(150, 70)
                clipContainer(Size(40, 40)) {
                    filter = IdentityFilter.Linear
                    solidRect(20, 20, Colors.GREEN).xy(-10, -10)
                }
            }
        }

        val tv = TransitionView()
        tv.xy(400, 100)
        tv.startNewTransition(DummyView())
        tv.startNewTransition(Container().apply {
            clipContainer(Size(512, 512)) {
                solidRect(512, 512, Colors.BLUE)
            }
        }, MaskTransition(TransitionFilter.Transition.CIRCULAR))
        tv.ratio = Ratio.HALF
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
                clipContainer(Size(150, 100)) {
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
                    launch {
                        val bitmap = container.renderToBitmap(views).also {
                            it.updateColors { if (it.a == 0) Colors.RED else it }
                        }
                        container2.removeChildren()
                        container2.addChild(image(bitmap).also {
                            it.x = 300.0
                            it.y = views.virtualHeightDouble - it.bitmap.height - 50 * escale
                        })
                    }
                }
            }
        }
    }
}
