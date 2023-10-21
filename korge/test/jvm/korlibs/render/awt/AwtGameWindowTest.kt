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
import kotlin.test.*

class AwtGameWindowTest {
    @Test
    //@Ignore
    fun test() {
        Korge.logger.level = Logger.Level.DEBUG
        val gameWindow = AwtGameWindow(GameWindowCreationConfig.DEFAULT.copy(title = "hello"))

        gameWindow.bgcolor = Colors.BLACK

        gameWindow.configureKorge(KorgeConfig(backgroundColor = Colors.BLACK)) {
            println(measureTime {
                image(resourcesVfs["korge.png"].readBitmap())
            })
            solidRect(100, 100, Colors.RED).addUpdater {
                this.x++
            }
            mouse.click {
                println("CLICK!")
                showContextMenu {
                    item("Hello")
                    item("World")
                }

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

            setMainMenu {
                item("Hello") {
                    item("&Exit")
                }
                item("World") {
                    item("Hello")
                }
            }

            //mouse { moveAnywhere { println("MOUSE MOVE $it") } }
        }

        gameWindow.show()
        NativeThread.sleep(100.seconds)
    }
}
