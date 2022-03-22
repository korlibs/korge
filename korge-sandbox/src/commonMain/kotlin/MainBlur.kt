import com.soywiz.kmem.*
import com.soywiz.kmem.clamp
import com.soywiz.korge.input.*
import com.soywiz.korge.ui.*
import com.soywiz.korge.view.*
import com.soywiz.korge.view.filter.*
import com.soywiz.korim.color.*
import com.soywiz.korim.format.*
import com.soywiz.korim.text.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.std.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.math.*

suspend fun Stage.mainBlur() {
    solidRect(stage.width, stage.height, Colors.WHITE)
    val bitmap = resourcesVfs["korim.png"].readBitmap()

    val initialBlur = 6.0
    var filterScale = 1.0

    val blur0a = DirectionalBlurFilter(angle = 0.degrees, radius = initialBlur)
    val blur0b = DirectionalBlurFilter(angle = 90.degrees, radius = initialBlur)
    val blur0c = DirectionalBlurFilter(angle = 45.degrees, radius = initialBlur)
    val blur0d = DirectionalBlurFilter(angle = 45.degrees, radius = initialBlur, expandBorder = false)
    val blur1 = BlurFilter(initialBlur)
    //val blur1 = DirectionalBlurFilter(angle = 0.degrees, radius = 32.0)
    //val blur1 = DirectionalBlurFilter(angle = 90.degrees, radius = 32.0)
    //val blur2 = OldBlurFilter(initialBlur)
    val blur2 = BlurFilter(initialBlur)

    val image0b = image(bitmap).xy(700, 100).filters(blur0b)
    val image0a = image(bitmap).xy(700, 400).filters(blur0a)
    val image0c = image(bitmap).xy(900, 100).filters(blur0c)
    val image0d = image(bitmap).xy(1100, 100).filters(blur0d)

    val image1 = image(bitmap)
        .xy(100, 100)
        .filters(blur1)

    val rotatedBitmap = image(bitmap)
        .xy(150, 300)
        .scale(0.75)
        .anchor(Anchor.CENTER)
        .rotation(45.degrees)
        .filters(blur1)

    val image2 = image(bitmap)
    //solidRect(128, 128, Colors.RED)
        .xy(300, 100)
        .filters(blur2)
        //.visible(false)

    val dropshadowFilter = DropshadowFilter(blurRadius = 1.0, shadowColor = Colors.RED.withAd(0.3))
    val image3 = image(bitmap).xy(500, 100).filters(dropshadowFilter)

    val colorMatrixFilter = ColorMatrixFilter(ColorMatrixFilter.SEPIA_MATRIX, blendRatio = 0.5)
    val image4 = image(bitmap).xy(500, 250).filters(colorMatrixFilter)

    val transitionFilter = TransitionFilter(TransitionFilter.Transition.CIRCULAR, reversed = false, ratio = 0.5)
    val image4b = image(bitmap).xy(370, 250).filters(transitionFilter)

    val pageFilter = PageFilter()
    val image5 = image(bitmap).xy(500, 450).filters(pageFilter)

    val waveFilter = WaveFilter()
    val image6 = image(bitmap).xy(500, 600).filters(waveFilter)

    val flagFilter = FlagFilter()
    val image7 = image(bitmap).xy(700, 600).filters(flagFilter)

    val image8 = image(bitmap).xy(900, 600).filters(blur1, waveFilter, blur1, pageFilter)

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

    addUpdater {
        //blur2.radius = blur1.radius
        blur2.radius = blur1.radius / 2.0
        blur0a.radius = blur1.radius
        blur0b.radius = blur1.radius
        blur0c.radius = blur1.radius
        blur0d.radius = blur1.radius

        image0a.filterScale = filterScale
        image0b.filterScale = filterScale
        image0c.filterScale = filterScale
        image0d.filterScale = filterScale
        image1.filterScale = filterScale
        rotatedBitmap.filterScale = filterScale
        image2.filterScale = filterScale
        image3.filterScale = filterScale
        image4.filterScale = filterScale
        image5.filterScale = filterScale
        image6.filterScale = filterScale
        image4b.filterScale = filterScale
        image7.filterScale = filterScale
        image8.filterScale = filterScale

        //println(blur1.radius)
    }

    /*
    while (true) {
        tween(blur1::radius[0.0], time = 1.seconds)
        tween(blur1::radius[32.0], time = 1.seconds)
    }
     */
}
