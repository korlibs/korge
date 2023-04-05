package korlibs.korge.ui

import korlibs.image.font.*
import korlibs.io.file.std.*
import korlibs.korge.input.*
import korlibs.korge.style.*
import korlibs.korge.tests.*
import korlibs.korge.view.position
import korlibs.math.geom.*
import kotlin.test.*

class UiTest : ViewsForTesting() {
    @Test
    fun test() = viewsTest {
        //uiSkin(OtherUISkin()) {
        styles {
            textFont = resourcesVfs["uifont.fnt"].readBitmapFont()
        }
        uiButton(size = Size(256.0, 32.0)) {
            text = "Disabled Button"
            position(128, 128)
            onClick {
                println("CLICKED!")
            }
            disable()
        }
        uiButton(size = Size(256.0, 32.0)) {
            text = "Enabled Button"
            position(128, 128 + 32)
            onClick {
                println("CLICKED!")
                views.gameWindow.close()
            }
            enable()
        }
        uiOldScrollBar(256f, 32f, 0f, 32f, 64f) {
            position(64, 64)
            onChange {
                println(it.ratio)
            }
        }
        uiOldScrollBar(32f, 256f, 0f, 16f, 64f) {
            position(64, 128)
            onChange {
                println(it.ratio)
            }
        }

        uiCheckBox {
            position(128, 128 + 64)
        }

        uiComboBox(items = listOf("ComboBox", "World", "this", "is", "a", "list", "of", "elements")) {
            position(128, 128 + 64 + 32)
        }

        uiScrollableArea(config = {
            position(480, 128)
        }) {

            for (n in 0 until 16) {
                uiButton("HELLO $n").position(0, n * 64)
            }
        }

        val progress = uiProgressBar {
            position(64, 32)
            current = 0.5f
        }

    }
}
