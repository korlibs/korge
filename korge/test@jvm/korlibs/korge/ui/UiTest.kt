package korlibs.korge.ui

import korlibs.korge.*
import korlibs.korge.input.*
import korlibs.korge.view.*
import korlibs.math.geom.*
import korlibs.render.*
import kotlinx.coroutines.*

fun main(): Unit = runBlocking { korge() }

suspend fun korge() = Korge(quality = GameWindow.Quality.PERFORMANCE, title = "UI") {

    uiText("Some text", Size(130f, 20f)) {
        position(128, 265)
    }

    uiButton("Disabled Button", Size(256f, 32f)) {
        position(128, 128)
        onClick {
            println("CLICKED!")
        }
        disable()
    }
    uiButton("Enabled Button", Size(256f, 32f)) {
        position(128, 128 + 32)
        onClick {
            println("CLICKED!")
        }
        enable()
    }

    uiOldScrollBar(Size(256f, 32f), 0.0, 32.0, 64.0) {
        position(64, 64)
        onChange {
            //println(it.ratio)
        }
    }
    uiOldScrollBar(Size(32f, 256f), 0.0, 16.0, 64.0) {
        position(64, 128)
        onChange {
            //println(it.ratio)
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

    /*
    launchImmediately {
        while (true) {
            tween(progress::current[1.0], time = 1.secs, easing = Easing.EASE_IN_OUT)
            tween(progress::current[1.0, 0.0], time = 1.secs, easing = Easing.EASE_IN_OUT)
        }
    }
     */
}
