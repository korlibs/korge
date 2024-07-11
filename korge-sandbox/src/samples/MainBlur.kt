package samples

import korlibs.datastructure.*
import korlibs.image.color.*
import korlibs.image.format.*
import korlibs.io.file.std.*
import korlibs.korge.scene.*
import korlibs.korge.style.*
import korlibs.korge.ui.*
import korlibs.korge.view.*
import korlibs.korge.view.filter.*
import korlibs.math.geom.Anchor
import korlibs.math.geom.degrees
import korlibs.math.interpolation.*
import korlibs.time.*
import kotlin.reflect.*

class MainBlur : Scene() {
    override suspend fun SContainer.sceneMain() {
        solidRect(views.stage.width, views.stage.height, Colors.WHITE)
        val bitmap = resourcesVfs["korim.png"].readBitmap()

        val initialBlur = 6.0
        var filterScale = 1.0
        fun <T : View> T.bindScale(): T {
            addFastUpdater { this.filterScale = filterScale }
            return this
        }

        val blur1 = BlurFilter(initialBlur)
        val blur2 = BlurFilter(initialBlur)
        val radiusProps = fastArrayListOf<KMutableProperty0<Double>>()

        addFastUpdater {
            //blur2.radius = blur1.radius
            blur2.radius = blur1.radius / 2f
            for (prop in radiusProps) prop.set(blur1.radius)
            //println(blur1.radius)
        }

        fun bindRadius(prop: KMutableProperty0<Double>) = radiusProps.add(prop)
        fun DirectionalBlurFilter.bindRadius() = also { bindRadius(it::radius) }
        fun BlurFilter.bindRadius() = also { bindRadius(it::radius) }

        image(bitmap).xy(700, 100).filters(DirectionalBlurFilter(angle = 0.degrees, radius = initialBlur).bindRadius()).bindScale()
        image(bitmap).xy(700, 400).filters(DirectionalBlurFilter(angle = 90.degrees, radius = initialBlur).bindRadius()).bindScale()
        image(bitmap).xy(900, 100).filters(DirectionalBlurFilter(angle = 45.degrees, radius = initialBlur).bindRadius()).bindScale()
        image(bitmap).xy(1100, 100).filters(DirectionalBlurFilter(angle = 45.degrees, radius = initialBlur, expandBorder = false).bindRadius()).bindScale()
        image(bitmap).xy(1100, 400).filters(BlurFilter(radius = initialBlur, expandBorder = false).bindRadius()).bindScale()

        image(bitmap)
            .xy(100, 100)
            .filters(blur1)
            .bindScale()

        val rotatedBitmap = image(bitmap)
            .xy(150, 300)
            .scale(0.75)
            .anchor(Anchor.CENTER)
            .rotation(45.degrees)
            .filters(blur1)
            .bindScale()

        val image2 = image(bitmap)
            //solidRect(128, 128, Colors.RED)
            .xy(300, 100)
            .filters(blur2)
            .bindScale()
        //.visible(false)

        val dropshadowFilter = DropshadowFilter(blurRadius = 1.0, shadowColor = Colors.RED.withAd(0.3))
        image(bitmap).xy(500, 100).filters(dropshadowFilter).bindScale()

        val colorMatrixFilter = ColorMatrixFilter(ColorMatrixFilter.SEPIA_MATRIX, blendRatio = 0.5)
        image(bitmap).xy(500, 250).filters(colorMatrixFilter).bindScale()

        val transitionFilter = TransitionFilter(TransitionFilter.Transition.CIRCULAR, reversed = false, ratio = Ratio.HALF, spread = 0.2)
        image(bitmap).xy(370, 250).filters(transitionFilter).bindScale()

        val pageFilter = PageFilter()
        image(bitmap).xy(500, 450).filters(pageFilter).bindScale()

        val waveFilter = WaveFilter()
        image(bitmap).xy(500, 600).filters(waveFilter).bindScale()

        val flagFilter = FlagFilter()
        image(bitmap).xy(700, 600).filters(flagFilter).bindScale()

        image(bitmap).xy(900, 600).filters(blur1, waveFilter, blur1, pageFilter).bindScale()

        uiVerticalStack(padding = 2.0, width = 370.0) {
            xy(50, 400)
            uiHorizontalFill(padding = 4.0) {
                uiText("Blur radius").styles { textColor = Colors.BLACK }
                uiSlider(value = initialBlur, max = 32, step = 0.1) {
                    styles {
                        uiSelectedColor = MaterialColors.RED_600
                        uiBackgroundColor = MaterialColors.BLUE_50
                    }
                    changed { blur1.radius = it }
                }
            }
            uiHorizontalFill(padding = 4.0) {
                uiText("Drop radius").styles { textColor = Colors.BLACK }
                uiSlider(value = dropshadowFilter.blurRadius.toInt(), max = 32).changed { dropshadowFilter.blurRadius = it }
            }
            uiHorizontalFill(padding = 4.0) {
                uiText("Drop X").styles { textColor = Colors.BLACK }
                uiSlider(value = dropshadowFilter.dropX.toInt(), min = -32, max = +32).changed { dropshadowFilter.dropX = it }
            }
            uiHorizontalFill(padding = 4.0) {
                uiText("Drop Y").styles { textColor = Colors.BLACK }
                uiSlider(value = dropshadowFilter.dropY.toInt(), min = -32, max = +32).changed { dropshadowFilter.dropY = it }
            }
            uiHorizontalFill(padding = 4.0) {
                uiButton("black").clicked { dropshadowFilter.shadowColor = Colors.BLACK.withAd(dropshadowFilter.shadowColor.ad) }
                uiButton("red").clicked { dropshadowFilter.shadowColor = Colors.RED.withAd(dropshadowFilter.shadowColor.ad) }
                uiButton("green").clicked { dropshadowFilter.shadowColor = Colors.GREEN.withAd(dropshadowFilter.shadowColor.ad) }
                uiButton("blue").clicked { dropshadowFilter.shadowColor = Colors.BLUE.withAd(dropshadowFilter.shadowColor.ad) }
            }
            uiHorizontalFill(padding = 4.0) {
                uiText("Drop Alpha").styles { textColor = Colors.BLACK }
                uiSlider(value = dropshadowFilter.shadowColor.a, min = 0, max = 255).changed { dropshadowFilter.shadowColor = dropshadowFilter.shadowColor.withA(it.toInt()) }
            }
            uiHorizontalFill(padding = 4.0) {
                uiText("Rotation").styles { textColor = Colors.BLACK }
                uiSlider(value = rotatedBitmap.rotation.degrees.toInt(), min = 0, max = 360) {
                    textTransformer = { "${it}'" }
                    changed { rotatedBitmap.rotation = it.degrees }
                }
            }
            uiHorizontalFill(padding = 4.0) {
                uiButton("circular").clicked { transitionFilter.transition = TransitionFilter.Transition.CIRCULAR }
                uiButton("diagonal1").clicked { transitionFilter.transition = TransitionFilter.Transition.DIAGONAL1 }
                uiButton("diagonal2").clicked { transitionFilter.transition = TransitionFilter.Transition.DIAGONAL2 }
                uiButton("sweep").clicked { transitionFilter.transition = TransitionFilter.Transition.SWEEP }
                uiButton("horizontal").clicked { transitionFilter.transition = TransitionFilter.Transition.HORIZONTAL }
                uiButton("vertical").clicked { transitionFilter.transition = TransitionFilter.Transition.VERTICAL }
            }
            uiHorizontalFill {
                uiText("Blend").styles { textColor = Colors.BLACK }
                uiSlider(value = transitionFilter.ratio.toDouble(), min = 0.0, max = 1.0, step = 0.0, decimalPlaces = 2).changed {
                    colorMatrixFilter.blendRatio = it
                    pageFilter.hamplitude0 = it
                    transitionFilter.ratio = it.toRatio()
                    pageFilter.hratio = it.toRatio()
                    waveFilter.time = it.seconds
                    flagFilter.time = it.seconds
                }
            }
            uiHorizontalFill {
                uiText("Spread").styles { textColor = Colors.BLACK }
                uiSlider(value = transitionFilter.spread, min = 0.0, max = 1.0, step = 0.0, decimalPlaces = 2).changed {
                    transitionFilter.spread = it
                }
            }
            uiHorizontalFill {
                uiText("Filter Scale").styles { textColor = Colors.BLACK }
                uiSlider(value = filterScale, min = 0.2, max = 2.0, step = 0.1) {
                    marks = true
                    changed {
                        filterScale = it
                    }
                }
            }
        }

        /*
        while (true) {
            tween(blur1::radius[0.0], time = 1.seconds)
            tween(blur1::radius[32.0], time = 1.seconds)
        }
         */
    }
}
