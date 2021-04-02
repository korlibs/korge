import com.soywiz.kds.FastArrayList
import com.soywiz.kds.iterators.fastForEach
import com.soywiz.korge.Korge
import com.soywiz.korge.input.mouse
import com.soywiz.korge.render.BatchBuilder2D
import com.soywiz.korge.view.addUpdater
import com.soywiz.korge.view.fast.FastSprite
import com.soywiz.korge.view.fast.alpha
import com.soywiz.korge.view.fast.fastSpriteContainer
import com.soywiz.korge.view.position
import com.soywiz.korge.view.text
import com.soywiz.korim.bitmap.BmpSlice
import com.soywiz.korim.bitmap.effect.BitmapEffect
import com.soywiz.korim.bitmap.sliceWithSize
import com.soywiz.korim.color.Colors
import com.soywiz.korim.font.DefaultTtfFont
import com.soywiz.korim.font.toBitmapFont
import com.soywiz.korim.format.readBitmap
import com.soywiz.korio.file.std.resourcesVfs
import kotlin.random.Random

class Bunny(tex: BmpSlice) : FastSprite(tex) {
    var speedXf: Float = 0f
    var speedYf: Float = 0f
}

// bunnymark ported from PIXI.js
// https://www.goodboydigital.com/pixijs/bunnymark/
// https://www.goodboydigital.com/pixijs/bunnymark/js/bunnyBenchMark.js
suspend fun main() = Korge(width = 800, height = 600, bgcolor = Colors["#2b2b9b"], batchMaxQuads = BatchBuilder2D.MAX_BATCH_QUADS) {
//suspend fun main() = Korge(width = 800, height = 600, bgcolor = Colors["#2b2b9b"]) {
    val wabbitTexture = resourcesVfs["bunnys.png"].readBitmap()

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

    val container = fastSpriteContainer(useRotation = true, smoothing = false)
    val font = DefaultTtfFont.toBitmapFont(fontSize = 16.0, effect = BitmapEffect(dropShadowX = 1, dropShadowY = 1, dropShadowRadius = 1))
    val bunnyCountText = text("", font = font, textSize = 16.0, alignment = com.soywiz.korim.text.TextAlignment.TOP_LEFT).position(16.0, 16.0)

    val bunnys = FastArrayList<Bunny>()

    val random = Random

    fun addBunny(count: Int = 1) {
        for (n in 0 until count) {
            val bunny = Bunny(currentTexture)
            bunny.speedXf = random.nextFloat() * 1
            bunny.speedYf = (random.nextFloat() * 1) - 5
            bunny.anchorXf = .5f
            bunny.anchorYf = 1f
            bunny.alpha = 0.3f + random.nextFloat() * 0.7f
            bunny.scale(0.5f + random.nextFloat() * 0.5f)
            bunny.rotationRadiansf = (random.nextFloat() - 0.5f)
            container.addChild(bunny)
            bunnys.add(bunny)
        }
        bunnyCountText.text = "(WIP) KorGE Bunnymark. Bunnies: ${bunnys.size}"
    }

    addBunny(startBunnyCount)

    val maxX = width.toFloat()
    val minX = 0f
    val maxY = height.toFloat()
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
                addBunny(200)
            } else if (bunnys.size < 400_000) {
                addBunny(1000)
            }
        }
        bunnys.fastForEach { bunny ->
            bunny.xf += bunny.speedXf
            bunny.yf += bunny.speedYf
            bunny.speedYf += gravity

            if (bunny.xf > maxX) {
                bunny.speedXf *= -1
                bunny.xf = maxX
            } else if (bunny.xf < minX) {
                bunny.speedXf *= -1
                bunny.xf = minX
            }

            if (bunny.yf > maxY) {
                bunny.speedYf *= -0.85f
                bunny.yf = maxY
                bunny.rotationRadiansf = (random.nextFloat() - 0.5f) * 0.2f
                if (random.nextFloat() > 0.5) {
                    bunny.speedYf -= random.nextFloat() * 6
                }
            } else if (bunny.yf < minY) {
                bunny.speedYf = 0f
                bunny.yf = minY
            }
        }
    }
}
