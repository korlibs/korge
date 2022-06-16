package samples

import com.soywiz.kds.fastArrayListOf
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.ui.*
import com.soywiz.korge.view.*
import com.soywiz.korge.view.filter.*
import com.soywiz.korim.color.Colors
import com.soywiz.korim.format.readBitmap
import com.soywiz.korio.file.std.resourcesVfs
import com.soywiz.korma.geom.Anchor
import com.soywiz.korma.geom.degrees
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KMutableProperty0

class MainBlur : Scene() {
    override suspend fun Container.sceneMain() {
        solidRect(views.stage.width, views.stage.height, Colors.WHITE)
        val bitmap = resourcesVfs["korim.png"].readBitmap()

        val initialBlur = 6.0
        var filterScale = 1.0
        fun <T : View> T.bindScale(): T {
            addUpdater { this.filterScale = filterScale }
            return this
        }

        val blur1 = BlurFilter(initialBlur)
        val blur2 = BlurFilter(initialBlur)
        val radiusProps = fastArrayListOf<KMutableProperty0<Double>>()

        addUpdater {
            //blur2.radius = blur1.radius
            blur2.radius = blur1.radius / 2.0
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

        val transitionFilter = TransitionFilter(TransitionFilter.Transition.CIRCULAR, reversed = false, ratio = 0.5)
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
            uiHorizontalFill {
                uiText("Blur radius").apply { textColor = Colors.BLACK }
                uiSlider(value = initialBlur, max = 32, step = 0.1).changed { blur1.radius = it.toDouble()  }
            }
            uiHorizontalFill {
                uiText("Drop radius").apply { textColor = Colors.BLACK }
                uiSlider(value = dropshadowFilter.blurRadius.toInt(), max = 32).changed { dropshadowFilter.blurRadius = it.toDouble() }
            }
            uiHorizontalFill {
                uiText("Drop X").apply { textColor = Colors.BLACK }
                uiSlider(value = dropshadowFilter.dropX.toInt(), min = -32, max = +32).changed { dropshadowFilter.dropX = it.toDouble() }
            }
            uiHorizontalFill {
                uiText("Drop Y").apply { textColor = Colors.BLACK }
                uiSlider(value = dropshadowFilter.dropY.toInt(), min = -32, max = +32).changed { dropshadowFilter.dropY = it.toDouble() }
            }
            uiHorizontalFill {
                uiButton("black").clicked { dropshadowFilter.shadowColor = Colors.BLACK.withAd(dropshadowFilter.shadowColor.ad) }
                uiButton("red").clicked { dropshadowFilter.shadowColor = Colors.RED.withAd(dropshadowFilter.shadowColor.ad) }
                uiButton("green").clicked { dropshadowFilter.shadowColor = Colors.GREEN.withAd(dropshadowFilter.shadowColor.ad) }
                uiButton("blue").clicked { dropshadowFilter.shadowColor = Colors.BLUE.withAd(dropshadowFilter.shadowColor.ad) }
            }
            uiHorizontalFill {
                uiText("Drop Alpha").apply { textColor = Colors.BLACK }
                uiSlider(value = dropshadowFilter.shadowColor.a, min = 0, max = 255).changed { dropshadowFilter.shadowColor = dropshadowFilter.shadowColor.withA(it.toInt()) }
            }
            uiHorizontalFill {
                uiText("Rotation").apply { textColor = Colors.BLACK }
                uiSlider(value = rotatedBitmap.rotation.degrees.toInt(), min = 0, max = 360).changed { rotatedBitmap.rotation = it.degrees }
            }
            uiHorizontalFill {
                uiButton("circular").clicked { transitionFilter.transition = TransitionFilter.Transition.CIRCULAR }
                uiButton("diagonal1").clicked { transitionFilter.transition = TransitionFilter.Transition.DIAGONAL1 }
                uiButton("diagonal2").clicked { transitionFilter.transition = TransitionFilter.Transition.DIAGONAL2 }
                uiButton("sweep").clicked { transitionFilter.transition = TransitionFilter.Transition.SWEEP }
                uiButton("horizontal").clicked { transitionFilter.transition = TransitionFilter.Transition.HORIZONTAL }
                uiButton("vertical").clicked { transitionFilter.transition = TransitionFilter.Transition.VERTICAL }
            }
            uiHorizontalFill {
                uiText("Blend").apply { textColor = Colors.BLACK }
                uiSlider(value = 0.5, min = 0.0, max = 1.0, step = 0.0, decimalPlaces = 2).changed {
                    colorMatrixFilter.blendRatio = it
                    pageFilter.hamplitude0 = it
                    transitionFilter.ratio = it
                    pageFilter.hratio = it
                    waveFilter.timeSeconds = it
                    flagFilter.timeSeconds = it
                }
            }
            uiHorizontalFill {
                uiText("Filter Scale").apply { textColor = Colors.BLACK }
                uiSlider(value = 1.0, min = 0.2, max = 2.0, step = 0.1).changed {
                    filterScale = it
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
