package com.soywiz.korgw

import com.soywiz.korag.*
import com.soywiz.korag.gl.*

class IosGameWindow : GameWindow() {
    override val dialogInterface = DialogInterfaceIos()

    override val ag: AG = object : AGNative(gles = true) {
        override val gl: com.soywiz.kgl.KmlGl = com.soywiz.kgl.CheckErrorsKmlGlProxy(com.soywiz.kgl.KmlGlNative())
    }

    //override var fps: Int get() = 60; set(value) = Unit
    //override var title: String get() = ""; set(value) = Unit
    //override val width: Int get() = 512
    //override val height: Int get() = 512
    //override var icon: Bitmap? get() = null; set(value) = Unit
    //override var fullscreen: Boolean get() = false; set(value) = Unit
    //override var visible: Boolean get() = false; set(value) = Unit
    //override var quality: Quality get() = Quality.AUTOMATIC; set(value) = Unit

    override suspend fun loop(entry: suspend GameWindow.() -> Unit) {
        //println("loop[0]")
        try {
            entry(this)
            //println("loop[1]")
        } catch (e: Throwable) {
            println("ERROR IosGameWindow.loop:")
            e.printStackTrace()
        }
    }

    companion object {
        fun getGameWindow() = MyIosGameWindow
    }
}

val MyIosGameWindow = IosGameWindow() // Creates instance everytime

actual fun CreateDefaultGameWindow(): GameWindow = MyIosGameWindow
