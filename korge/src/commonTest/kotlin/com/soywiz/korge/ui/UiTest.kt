package com.soywiz.korge.ui

import com.soywiz.korge.input.*
import com.soywiz.korge.tests.*
import com.soywiz.korge.view.*
import com.soywiz.korgw.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.font.*
import com.soywiz.korim.format.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.net.*
import com.soywiz.korio.util.*
import kotlin.test.*

class UiTest : ViewsForTesting() {
    @Test
    fun test() = viewsTest {
        val nativeProcess = NativeProcess(views)

        //uiSkin(OtherUISkin()) {
        uiSkin = UISkin {
            textFont = resourcesVfs["uifont.fnt"].readBitmapFont()
        }
        uiButton(256.0, 32.0) {
            text = "Disabled Button"
            position(128, 128)
            onClick {
                println("CLICKED!")
            }
            disable()
        }
        uiButton(256.0, 32.0) {
            text = "Enabled Button"
            position(128, 128 + 32)
            onClick {
                println("CLICKED!")
                nativeProcess.close()
            }
            enable()
        }
        uiScrollBar(256.0, 32.0, 0.0, 32.0, 64.0) {
            position(64, 64)
            onChange {
                println(it.ratio)
            }
        }
        uiScrollBar(32.0, 256.0, 0.0, 16.0, 64.0) {
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
                uiButton(text = "HELLO $n").position(0, n * 64)
            }
        }

        val progress = uiProgressBar {
            position(64, 32)
            current = 0.5
        }

    }

    private class NativeProcess(views: Views) : NativeProcessBase(views) {
    }

    private open class NativeProcessBase(val views: Views) : DialogInterface by views.gameWindow {
    }
}
