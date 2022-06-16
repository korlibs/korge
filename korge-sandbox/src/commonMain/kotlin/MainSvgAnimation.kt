import com.soywiz.klock.*
import com.soywiz.klogger.*
import com.soywiz.korev.*
import com.soywiz.korge.annotations.*
import com.soywiz.korge.input.*
import com.soywiz.korge.view.*
import com.soywiz.korge.view.vector.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.font.*
import com.soywiz.korim.format.*
import com.soywiz.korim.paint.*
import com.soywiz.korim.text.*
import com.soywiz.korim.vector.*
import com.soywiz.korim.vector.format.*
import com.soywiz.korio.file.std.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.vector.*

@OptIn(KorgeExperimental::class)
suspend fun Stage.mainSvgAnimation() {
    var svgScale = 1.4
    val svg = measureTime({ resourcesVfs["sample.svg"].readSVG() }) {
    //var svgScale = 0.9
    //val svg = measureTime({ resourcesVfs["sample_no_css_strip.svg"].readSVG() }) {
        println("Elapsed $it")
    }

    fun Context2d.buildGraphics() {
        keep {
            scale(svgScale)
            draw(svg)
        }
    }

    val gpuTigger = measureTime({
        gpuShapeView({ }) {
            xy(0, 0)
            scale(3.0 * svgScale)
            //rotation(15.degrees)
            keys {
                down(Key.N9) { debugDrawOnlyAntialiasedBorder = !debugDrawOnlyAntialiasedBorder }
                down(Key.N0) { antialiased = !antialiased }
                down(Key.A) { antialiased = !antialiased }
                down(Key.UP) { svgScale *= 1.1 }
                down(Key.DOWN) { svgScale *= 0.9 }
            }
        }
    }) {
        println("GPU SHAPE: $it")
    }

    addUpdater { dt ->
        svg.updateStyles(dt)
        gpuTigger.updateShape {
            buildGraphics()
        }
    }
}
