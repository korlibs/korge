package samples

import korlibs.image.format.*
import korlibs.io.file.std.*
import korlibs.korge.scene.*
import korlibs.korge.view.*
import korlibs.korge.view.filter.*
import korlibs.math.geom.*
import korlibs.math.interpolation.*
import korlibs.time.*

class MainFiltersRenderToBitmap : Scene() {
    override suspend fun SContainer.sceneMain() {
        println("LOADING IMAGE...")
        val bitmap = resourcesVfs["korge.png"].readBitmap()
        val container = FixedSizeContainer(Size(width, height)).apply {
            //scale(2.0, 2.0)
            println("PREPARING VIEWS...")
            image(bitmap).scale(.5).position(0, 0).addFilter(WaveFilter(time = 0.5.seconds, crestDistance = Vector2D(200, 200), amplitude = Vector2D(24, 24)))
            //image(bitmap).scale(.5).position(256, 0).addFilter(DirectionalBlurFilter(radius = 32.0))
            image(bitmap).scale(.5).position(256, 0).addFilter(BlurFilter(radius = 32.0))
            image(bitmap).scale(.5).position(512, 0).addFilter(TransitionFilter(TransitionFilter.Transition.SWEEP, reversed = false, spread = 1.0, ratio = Ratio.HALF))
            image(bitmap).scale(.5).position(0, 256).addFilter(PageFilter(hratio = 0.5, hamplitude1 = 20.0))
            image(bitmap).scale(.5).position(256, 256).addFilter(Convolute3Filter(Convolute3Filter.KERNEL_SHARPEN))
            image(bitmap).scale(.5).position(512, 256).addFilter(SwizzleColorsFilter("bgga"))
            println("VIEWS PREPARED")
        }

        //image(stage.renderToBitmap(views)).scale(0.4).xy(800, 50)
        addChild(container)
        image(container.renderToBitmap(views)).scale(1.0).xy(50, 50)
        //container.visible = false
    }
}
