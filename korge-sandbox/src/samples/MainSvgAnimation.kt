package samples

import korlibs.time.*
import korlibs.event.*
import korlibs.korge.input.*
import korlibs.korge.scene.Scene
import korlibs.korge.view.*
import korlibs.korge.view.filter.DropshadowFilter
import korlibs.korge.view.filter.filter
import korlibs.korge.view.vector.*
import korlibs.image.color.Colors
import korlibs.image.vector.*
import korlibs.image.vector.format.*
import korlibs.io.file.std.*

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

        val shape = measureTime({
            //graphics({}, renderer = GraphicsRenderer.SYSTEM) {
            graphics({}, renderer = GraphicsRenderer.GPU) {
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
            shape.updateShape {
                buildGraphics()
            }
        }
    }
}
