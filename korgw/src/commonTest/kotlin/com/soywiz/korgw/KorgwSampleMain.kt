package com.soywiz.korgw

import com.soywiz.korag.*
import com.soywiz.korev.*
import com.soywiz.korim.color.*
import com.soywiz.korio.*
import com.soywiz.korma.geom.*

fun main(args: Array<String>) = Korio {
    CreateDefaultGameWindow().loop {
        configure(SizeInt(640, 480), "hello", fullscreen = false)
        onEvent(MouseEvent.Type.CLICK) { e ->
            toggleFullScreen()
            //    //fullscreen = !fullscreen
            //    configure(1280, 720, "KORGW!", fullscreen = false)
            //}
            //println(e)
        }
        var n = 0
        onRenderEvent {
            //println("render")
            ag.clear(ag.mainFrameBuffer, if (n % 2 == 0) Colors.GREEN else Colors.RED)
            n++
            //ag.flip()
        }
    }
}
