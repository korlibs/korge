package korlibs.korge.issues

import korlibs.korge.*
import korlibs.korge.input.*
import korlibs.korge.view.*
import korlibs.image.color.*
import korlibs.image.format.*
import korlibs.io.file.std.*
import korlibs.math.geom.*
import kotlinx.coroutines.*

object Issue83Sample {
    @JvmStatic
    fun main(args: Array<String>) {
        runBlocking {
            main()
        }
    }

    suspend fun main() = Korge(windowSize = SizeInt(512, 512), backgroundColor = Colors["#2b2b2b"]).start {
        val image = image(resourcesVfs["korge.png"].readBitmap()) {
            anchor(.5, .5)
            scale(.8)
            position(256, 256)
        }

        onClick {
            println("onClick:" + it.currentPosLocal)
        }
        onDown {
            println("onDown:" + it.downPosGlobal)
        }

    }
}
