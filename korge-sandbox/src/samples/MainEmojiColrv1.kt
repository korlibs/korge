package samples

import korlibs.image.color.*
import korlibs.image.font.*
import korlibs.image.vector.*
import korlibs.io.file.std.*
import korlibs.korge.scene.*
import korlibs.korge.view.*
import korlibs.korge.view.vector.*
import korlibs.math.geom.*
import korlibs.time.*

class MainEmojiColrv1 : Scene() {
    override suspend fun SContainer.sceneMain() {
        val font = resourcesVfs["twemoji-glyf_colr_1.ttf"].readTtfFont().asFallbackOf(DefaultTtfFont)
        val font2 = measureTime({resourcesVfs["noto-glyf_colr_1.ttf"].readTtfFont().asFallbackOf(DefaultTtfFont)}) {
            println("Read font in... $it")
        }
        val font3 = SystemFont.getEmojiFont().asFallbackOf(DefaultTtfFont)

        println("font=$font, font2=$font2, font3=$font3")

        //val font = DefaultTtfFont.withFallback()
        val str = "HELLO! 😀😁🤤👨‍🦳👨🏻‍🦳👨🏻‍🦳👩🏽‍🦳⛴🔮🤹‍♀️😇🥹🍦💩🥜🥝🌄🏞"
        //val str = "😶"
        //val str = "🌄"

        fun Context2d.buildText() {
            fillText(str, font = font, size = 50.0, pos = Point(22, 0), color = Colors.WHITE)
            fillText(str, font = font2, size = 50.0, pos = Point(22, 75), color = Colors.WHITE)
            //fillText(str, font = font3, textSize = 50.0, x = 22.0, y = 150.0, color = Colors.WHITE)
        }

        container {
            xy(0, 0)
            text(str, font = font, textSize = 50.0, color = Colors.WHITE).xy(x = 22.0, y = 0.0)
            text(str, font = font2, textSize = 50.0, color = Colors.WHITE).xy(x = 22.0, y = 75.0)
        }

        val shape = buildShape { buildText() }
        //println(shape.toSvg())

        println("native rendered in..." + kotlin.time.measureTime {
            cpuGraphics {
                it.xy(0, 200)
                it.useNativeRendering = true
                buildText()
            }.also {
                it.redrawIfRequired()
            }
        })
        println("non-native rendered in..." + kotlin.time.measureTime {
            cpuGraphics {
                it.xy(0, 350)
                it.useNativeRendering = false
                buildText()
            }.also {
                it.redrawIfRequired()
            }
        })
        gpuGraphics {
            it.xy(0, 500)
            buildText()
        }
        //text(str, font = font, textSize = 50.0).xy(64, 100)
        //text(str, font = font2, textSize = 50.0).xy(64, 200)
        //text(str, font = font3, textSize = 50.0).xy(64, 300)

        //text("👨‍🦳", font = font, textSize = 64.0).xy(64, 100)
        //text("\uD83D\uDC68\u200D", font = font, textSize = 64.0).xy(64, 100)
        //text("HELLO! 😀😁🤤", font = font, textSize = 64.0).xy(64, 100)
        //text("HELLO! 😀\uD83D\uDE01\uD83E\uDD24a", font = font, textSize = 64.0).xy(64, 100)
        //text("😀a", font = font, textSize = 64.0).xy(64, 100)
        //text("😀", font = font, textSize = 64.0).xy(64, 100)
        //text("HELLO! \uD83D\uDE00", font = font, textSize = 64.0).xy(50, 100)
    }
}
