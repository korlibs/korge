import com.soywiz.kds.iterators.fastForEach
import com.soywiz.klock.*
import com.soywiz.klogger.Console
import com.soywiz.korge.*
import com.soywiz.korge.render.*
import com.soywiz.korge.tween.*
import com.soywiz.korge.view.*
import com.soywiz.korim.color.*
import com.soywiz.korim.format.*
import com.soywiz.korio.file.std.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.vector.*
import com.soywiz.korma.interpolation.*
import com.soywiz.korge.input.*
import com.soywiz.korge.service.storage.NativeStorage
import com.soywiz.korge.time.delay
import com.soywiz.korge.view.fast.FastSprite
import com.soywiz.korge.view.fast.FastSpriteContainer
import com.soywiz.korge.view.fast.fastSpriteContainer
import com.soywiz.korim.bitmap.BmpSlice
import com.soywiz.korim.bitmap.sliceWithSize
import kotlin.random.Random

//class BunnyFastSprite(tex: BmpSlice) : FastSprite(tex) {
//    var speedX: Float = 0f
//    var speedY: Float = 0f
//}

class Bunny(tex: BmpSlice) : FastSprite(tex) {
    var speedXf: Float = 0f
    var speedYf: Float = 0f
    var spinf: Float = 0f
}

// bunnymark ported from PIXI.js
// https://www.goodboydigital.com/pixijs/bunnymark/js/bunnyBenchMark.js
suspend fun main() = Korge(width = 512, height = 512, bgcolor = Colors["#2b2b9b"]) {
    val wabbitTexture = resourcesVfs["bunnys.png"].readBitmap()

    val bunny1 = wabbitTexture.sliceWithSize(2, 47, 26, 37)
    val bunny2 = wabbitTexture.sliceWithSize(2, 86, 26, 37)
    val bunny3 = wabbitTexture.sliceWithSize(2, 125, 26, 37)
    val bunny4 = wabbitTexture.sliceWithSize(2, 164, 26, 37)
    val bunny5 = wabbitTexture.sliceWithSize(2, 2, 26, 37)

    //val startBunnyCount = 2
    //val startBunnyCount = 1_000_000
    val startBunnyCount = 200_000
    val bunnyTextures = listOf(bunny1, bunny2, bunny3, bunny4, bunny5)
    val currentTexture = bunny1

    val container = fastSpriteContainer()
    //val container = container()

    val bunnys = arrayListOf<Bunny>()

    val random = Random

    for (i in 0 until startBunnyCount) {
        //val bunny = BunnyFastSprite(currentTexture)
        val bunny = Bunny(currentTexture)
        bunny.speedXf = random.nextFloat() * 10
        bunny.speedYf = (random.nextFloat() * 10) - 5
        //bunny.anchorX = 0.5f
        //bunny.anchorY = 1f
        bunny.anchorXf = 0.5f
        bunny.anchorYf = 1.0f
        bunnys.add(bunny)
        //	bunny.filters = [filter];
        //	bunny.position.x = Math.random() * 800;
        //	bunny.position.y = Math.random() * 600;
        container.addChild(bunny)
    }

    val maxX = width.toFloat()
    val minX = 0f
    val maxY = height.toFloat()
    val minY = 0f
    val gravity = 0.5f // 1.5f

    addUpdater {
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
                bunny.spinf = (random.nextFloat() - 0.5f) * 0.2f
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
