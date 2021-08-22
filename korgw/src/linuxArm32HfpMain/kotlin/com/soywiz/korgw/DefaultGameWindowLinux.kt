package com.soywiz.korgw

import GL.getenv
import com.soywiz.korag.*
import com.soywiz.korev.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korio.file.*
import com.soywiz.korio.net.*
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import kotlin.coroutines.*

//actual fun CreateDefaultGameWindow(): GameWindow = glutGameWindow
actual fun CreateDefaultGameWindow(): GameWindow {
    val engine = getenv("KORGW_NATIVE_ENGINE")?.toKStringFromUtf8()
        ?: "default"
    println("Engine: $engine")
    return when (engine) {
        "sdl" -> SdlGameWindowNative()
        else -> X11GameWindow()
    }
}
