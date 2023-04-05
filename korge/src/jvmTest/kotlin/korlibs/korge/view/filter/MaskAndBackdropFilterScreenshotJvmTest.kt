package korlibs.korge.view.filter

import korlibs.image.color.*
import korlibs.korge.testing.*
import korlibs.korge.view.*
import korlibs.korge.view.mask.*
import korlibs.math.geom.*
import org.junit.*

class MaskAndBackdropFilterScreenshotJvmTest {
    @Test
    fun test() = korgeScreenshotTest(Size(100, 100)) {
        solidRect(widthD, heightD, Colors.GREEN)

        //val fill1 = LinearGradientPaint(0, 0, 100, 100).add(0.0, Colors.RED).add(1.0, Colors.BLUE)
        val maskView = fastEllipse(Size(100, 100)).xy(25, 25).visible(false)
        val circle1 = fastEllipse(Size(100, 100), color = Colors.RED)
            //val circle1 = solidRect(200, 200, Colors.PURPLE)
            //.filters(DropshadowFilter())
            //.filters(BlurFilter())
            //.filters(ColorMatrixFilter(ColorMatrixFilter.GRAYSCALE_MATRIX))
            .mask(maskView)
            //.filters(ColorMatrixFilter(ColorMatrixFilter.GRAYSCALE_MATRIX))
        //.mask(solidRect(100, 100, Colors.WHITE).xy(50, 50).visible(false))

        fastRoundRect(Size(40, 40), RectCorners(.5f)).xy(15, 15)
            .backdropFilters(ColorMatrixFilter(ColorMatrixFilter.GRAYSCALE_MATRIX))
            //.backdropFilters(BlurFilter())

        assertScreenshot(posterize = 4)
    }
}
