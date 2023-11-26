package samples

import korlibs.image.bitmap.*
import korlibs.image.font.*
import korlibs.image.format.*
import korlibs.image.text.*
import korlibs.io.async.*
import korlibs.io.file.std.*
import korlibs.io.lang.*
import korlibs.io.resources.*
import korlibs.korge.input.*
import korlibs.korge.resources.*
import korlibs.korge.scene.Scene
import korlibs.korge.view.*
import korlibs.korge.view.fast.*
import korlibs.math.geom.*
import korlibs.math.random.*
import korlibs.time.*
import kotlin.collections.random
import kotlin.random.*

/*
class MainBunnymark : Scene() {
    // @TODO: We could autogenerate this via gradle
    val ResourcesContainer.korge_png by resourceBitmap("korge.png")

    class BunnyContainer(maxSize: Int) : FSprites(maxSize) {
        val speeds = FloatArray(maxSize * Float.SIZE_BYTES * 2)
        var FSprite.speedXf: Float get() = speeds[index * 2 + 0] ; set(value) { speeds[index * 2 + 0] = value }
        var FSprite.speedYf: Float get() = speeds[index * 2 + 1] ; set(value) { speeds[index * 2 + 1] = value }
        //var FSprite.tex: BmpSlice
    }

/*
class Bunny(tex: BmpSlice) : FastSprite(tex) {
    var speedXf: Float = 0f
    var speedYf: Float = 0f
}
*/

    // bunnymark ported from PIXI.js
// https://www.goodboydigital.com/pixijs/bunnymark/
// https://www.goodboydigital.com/pixijs/bunnymark/js/bunnyBenchMark.js
    override suspend fun SContainer.sceneMain() {
        println("currentThreadId=$currentThreadId")
        delay(1.milliseconds)
        println("currentThreadId=$currentThreadId")
        println("ag.graphicExtensions=${ag.graphicExtensions}")
        println("ag.isFloatTextureSupported=${ag.isFloatTextureSupported}")
        println("ag.isInstancedSupported=${ag.isInstancedSupported}")
//suspend fun main() = Korge(width = 800, height = 600, bgcolor = Colors["#2b2b9b"]) {
        val wabbitTexture0 = resourcesVfs["bunnys.png"].readBitmap()
        val GRAYSCALE_MATRIX = Matrix4.fromColumns(
            0.33f, 0.33f, 0.33f, 0f,
            0.59f, 0.59f, 0.59f, 0f,
            0.11f, 0.11f, 0.11f, 0f,
            0f, 0f, 0f, 1f
        )
        val wabbitTexture1 = wabbitTexture0.clone().toBMP32IfRequired().also { it.applyColorMatrix(GRAYSCALE_MATRIX) }
        val wabbitTexture = wabbitTexture0

        val bunny1 = wabbitTexture.sliceWithSize(2, 47, 26, 37)
        val bunny2 = wabbitTexture.sliceWithSize(2, 86, 26, 37)
        val bunny3 = wabbitTexture.sliceWithSize(2, 125, 26, 37)
        val bunny4 = wabbitTexture.sliceWithSize(2, 164, 26, 37)
        val bunny5 = wabbitTexture.sliceWithSize(2, 2, 26, 37)

        val startBunnyCount = 2
        //val startBunnyCount = 1_000_000
        // val startBunnyCount = 200_000
        val bunnyTextures = listOf(bunny1, bunny2, bunny3, bunny4, bunny5)
        var currentTexture = bunny1

        val bunnys = BunnyContainer(800_000)
        addChild(bunnys.createView(wabbitTexture0, wabbitTexture1).also {
            it.blendMode = BlendMode.NONE
        })

        val bunnyCountText = text("", font = DefaultTtfFontAsBitmap, textSize = 16f, alignment = TextAlignment.TOP_LEFT).position(16, 16)

        val random = Random(0)

        fun addBunny(count: Int = 1) {
            for (n in 0 until kotlin.math.min(count, bunnys.available)) {
                bunnys.apply {
                    val bunny = alloc()
                    bunny.speedXf = random.nextFloat() * 1
                    bunny.speedYf = (random.nextFloat() * 1) - 5
                    bunny.setAnchor(.5f, .5f)
                    bunny.xy(random[100f, 400f], random[100f, 400f])
                    bunny.scale(1f, 1f)
                    bunny.setTexIndex(n % 2)
                    //bunny.width = 10f
                    //bunny.height = 20f
                    //bunny.alpha = 0.3f + random.nextFloat() * 0.7f
                    bunny.setTex(currentTexture)
                    //bunny.scale(0.5f + random.nextFloat() * 0.5f)
                    bunny.radiansf = (random.nextFloat() - 0.5f)
                }
            }
            bunnyCountText.text = "(WIP) KorGE Bunnymark. Bunnies: ${bunnys.size}"
        }

        addBunny(startBunnyCount)

        val maxX = widthD.toFloat()
        val minX = 0f
        val maxY = heightD.toFloat()
        val minY = 0f
        val gravity = 0.5f // 1.5f

        mouse {
            up {
                currentTexture = bunnyTextures.random(random)
            }
        }

        addUpdater {
            if (views.input.mouseButtons != 0) {
                if (bunnys.size < 200_000) {
                    addBunny(2_000)
                } else if (bunnys.size < bunnys.maxSize - 1000) {
                    addBunny(4_000)
                }
            }
            var nRandom = 0
            val randoms = Array(32) { random.nextFloat() }

            bunnys.fastForEach { bunny ->
                bunny.x += bunny.speedXf
                bunny.y += bunny.speedYf
                bunny.speedYf += gravity

                if (bunny.x > maxX) {
                    bunny.speedXf *= -1
                    bunny.x = maxX
                } else if (bunny.x < minX) {
                    bunny.speedXf *= -1
                    bunny.x = minX
                }

                if (bunny.y > maxY) {
                    bunny.speedYf *= -0.85f
                    bunny.y = maxY
                    bunny.radiansf = (randoms[nRandom++ % 32] - 0.5f) * 0.2f
                    if (randoms[nRandom++ % 32] > 0.5) {
                        bunny.speedYf -= randoms[nRandom++ % 32] * 6
                    }
                } else if (bunny.y < minY) {
                    bunny.speedYf = 0f
                    bunny.y = minY
                }
            }
            invalidate()
        }
    }


/*
suspend fun main() {
    //GLOBAL_CHECK_GL = true
    Korge(width = 512, height = 512, bgcolor = Colors["#2b2b2b"], clipBorders = false) {
        gameWindow.icon = korge_png.get().bmp.toBMP32().scaled(32, 32)
        val minDegrees = (-16).degrees
        val maxDegrees = (+16).degrees
        val image = image(korge_png) {
            //val image = image(resourcesVfs["korge.png"].readbitmapslice) {
            rotation = maxDegrees
            anchor(.5, .5)
            scale(.8)
            position(256, 256)
        }
        addChild(MyView())
        //bindLength(image::scaledWidth) { 100.vw }
        //bindLength(image::scaledHeight) { 100.vh }
        while (true) {
            image.tween(image::rotation[minDegrees], time = 1.seconds, easing = Easing.EASE_IN_OUT)
            image.tween(image::rotation[maxDegrees], time = 1.seconds, easing = Easing.EASE_IN_OUT)
        }
    }
}
*/

}
*/
