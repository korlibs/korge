package korlibs.render.awt

import korlibs.datastructure.thread.*
import korlibs.image.color.*
import korlibs.image.format.*
import korlibs.io.file.std.*
import korlibs.korge.*
import korlibs.korge.view.*
import korlibs.logger.*
import korlibs.time.*
import kotlin.test.*

class AwtGameWindowTest {
    @Test
    //@Ignore
    fun test() {
        Korge.logger.level = Logger.Level.DEBUG
        val gameWindow = NewAwtGameWindow()

        gameWindow.configureKorge {
            image(resourcesVfs["korge.png"].readBitmap())
            solidRect(100, 100, Colors.RED).addUpdater {
                this.x++
            }
        }

        gameWindow.show()
        NativeThread.sleep(100.seconds)
    }
}
