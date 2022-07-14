package samples

import com.soywiz.klock.*
import com.soywiz.korev.*
import com.soywiz.korge.input.*
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.view.*
import com.soywiz.korge.view.filter.DropshadowFilter
import com.soywiz.korge.view.filter.filter
import com.soywiz.korge.view.vector.*
import com.soywiz.korim.color.Colors
import com.soywiz.korim.vector.*
import com.soywiz.korim.vector.format.*
import com.soywiz.korio.file.std.*

class MainSvgAnimation : Scene() {
    override suspend fun SContainer.sceneMain() {
        var svgScale = 1.4 * 1.4 * 3.0
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
                //scale(3.0 * svgScale)
                //rotation(15.degrees)
                keys {
                    down(Key.N9) { debugDrawOnlyAntialiasedBorder = !debugDrawOnlyAntialiasedBorder }
                    down(Key.N0) { antialiased = !antialiased }
                    down(Key.A) { antialiased = !antialiased }
                    down(Key.UP) { svgScale *= 1.1 }
                    down(Key.DOWN) { svgScale *= 0.9 }
                }
                filter = DropshadowFilter(shadowColor = Colors.RED)
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
}
