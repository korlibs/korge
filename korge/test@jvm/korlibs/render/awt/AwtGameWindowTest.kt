package korlibs.render.awt

import korlibs.concurrent.thread.*
import korlibs.concurrent.thread.NativeThread.Companion.sleep
import korlibs.datastructure.thread.*
import korlibs.datastructure.thread.NativeThread
import korlibs.image.color.*
import korlibs.image.format.*
import korlibs.io.async.*
import korlibs.io.file.std.*
import korlibs.korge.view.*
import korlibs.time.*
import kotlinx.coroutines.*
import kotlin.test.*

class AwtGameWindowTest {
    @Test
    @Ignore
    fun test() {
        val gameWindow = NewAwtGameWindow()

        //gameWindow.icon = runBlocking { resourcesVfs["korge.png"].readBitmap() }

        val views = Views(gameWindow)
        gameWindow.onRenderEvent {
            views.renderNew()
        }
        val stopwatch = Stopwatch()
        gameWindow.onUpdateEvent {
            //println(NativeThread.currentThreadName)
            views.update(stopwatch.getElapsedAndRestart())
            //println("UPDATE EVENT!")
        }
        gameWindow.launchUnscoped {
            views.stage.image(resourcesVfs["korge.png"].readBitmap())
            views.stage.solidRect(100, 100, Colors.RED).addUpdater {
                this.x++
            }
            println("[1]")
            delay(1.seconds)
            println("[2]")
        }
        //gameWindow.eventQueueLater { println("[0]") }
        //gameWindow.coroutineDispatcher.queue { println("[1]") }
        println("gameWindow=$gameWindow")
        //gameWindow.coroutineDispatcher.queue { println("[2]") }
        //gameWindow.show()
        //gameWindow.mainLoop { println("HELLO") }
        gameWindow.show()
        NativeThread.sleep(100.seconds)
    }
}
