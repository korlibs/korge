package korlibs.korge.ui

import korlibs.korge.input.onClick
import korlibs.korge.style.*
import korlibs.korge.tests.ViewsForTesting
import korlibs.korge.view.position
import korlibs.image.font.readBitmapFont
import korlibs.io.file.std.resourcesVfs
import korlibs.math.geom.*
import kotlin.test.Test

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
        uiOldScrollBar(256.0, 32.0, 0.0, 32.0, 64.0) {
            position(64, 64)
            onChange {
                println(it.ratio)
            }
        }
        uiOldScrollBar(32.0, 256.0, 0.0, 16.0, 64.0) {
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
            current = 0.5
        }

    }
}