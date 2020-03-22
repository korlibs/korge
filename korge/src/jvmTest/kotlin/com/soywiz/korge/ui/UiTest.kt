package com.soywiz.korge.ui

import com.soywiz.klock.*
import com.soywiz.korge.*
import com.soywiz.korge.input.*
import com.soywiz.korge.tween.*
import com.soywiz.korge.view.*
import com.soywiz.korgw.*
import com.soywiz.korio.async.*
import com.soywiz.korma.interpolation.*
import kotlinx.coroutines.*

fun main(): Unit = runBlocking { korge() }

suspend fun korge() = Korge(quality = GameWindow.Quality.PERFORMANCE, title = "UI") {

    uiText("Some text", 130, 20) {
        position(128, 265)
    }

    textButton(256.0, 32.0, "Disabled Button") {
        position(128, 128)
        onClick {
            println("CLICKED!")
        }
        disable()
    }
    textButton(256.0, 32.0, "Enabled Button") {
        position(128, 128 + 32)
        onClick {
            println("CLICKED!")
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
            textButton(text = "HELLO $n").position(0, n * 64)
        }
    }

    val progress = uiProgressBar {
        position(64, 32)
        current = 0.5
    }

    launchImmediately {
        while (true) {
            tween(progress::current[1.0], time = 1.seconds, easing = Easing.EASE_IN_OUT)
            tween(progress::current[1.0, 0.0], time = 1.seconds, easing = Easing.EASE_IN_OUT)
        }
    }
}
