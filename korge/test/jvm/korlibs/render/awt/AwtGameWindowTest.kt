package korlibs.render.awt

import korlibs.datastructure.thread.*
import korlibs.event.*
import korlibs.image.color.*
import korlibs.image.format.*
import korlibs.io.file.std.*
import korlibs.korge.*
import korlibs.korge.input.*
import korlibs.korge.view.*
import korlibs.logger.*
import korlibs.render.*
import korlibs.time.*
import kotlinx.coroutines.*
import kotlin.test.*

class AwtGameWindowTest {
    @Test
    @Ignore
    fun test() {
        Korge.logger.level = Logger.Level.DEBUG
        val gameWindow = AwtGameWindow(GameWindowCreationConfig.DEFAULT.copy(title = "hello"))

        gameWindow.bgcolor = Colors.BLACK

        gameWindow.icon = runBlocking { resourcesVfs["korge.png"].readBitmap() }
        gameWindow.configureKorge(KorgeConfig(backgroundColor = Colors.BLACK)) {
            solidRect(width, height, Colors.SADDLEBROWN.withAd(0.2))
            println(measureTime {
                image(resourcesVfs["korge.png"].readBitmap())
            })
            solidRect(100, 100, Colors.RED) {
                mouse {
                    over { alpha = 0.5 }
                    out { alpha = 1.0 }
                }
            }.addUpdater {
                //println(views.globalToWindowMatrix)
                this.x++
            }
            mouse.click {
                println("CLICK!")
            }
            mouse.scroll {
                println("SCROLL: $it")
            }
            gestures {
                magnify { println("MAGNITIFY! $it") }
            }
            onEvent(DestroyEvent) {
                println("DESTROY!")
            }

            //mouse { moveAnywhere { println("MOUSE MOVE $it") } }
        }

        gameWindow.show()
        NativeThread.sleep(100.seconds)
    }
}
