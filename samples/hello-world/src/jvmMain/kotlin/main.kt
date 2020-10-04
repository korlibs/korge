/*
import com.soywiz.klock.*
import com.soywiz.korge.*
import com.soywiz.korge.render.*
import com.soywiz.korge.time.*
import com.soywiz.korge.tween.*
import com.soywiz.korge.view.*
import com.soywiz.korgw.osx.*
import com.soywiz.korim.color.*
import com.soywiz.korim.format.*
import com.soywiz.korio.file.std.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.vector.*
import com.soywiz.korma.interpolation.*
import com.sun.jna.*

class MacosGameController {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            println(NSClass("GCController").msgSend("controllers").msgSend("count"))
        }
    }
}

suspend fun main() {
    val lib = Native.load("/System/Library/Frameworks/GameController.framework/Versions/A/GameController", FrameworkInt::class.java)
    Korge {
        while (true) {
            println(NSClass("GCController").msgSend("controllers").msgSend("count"))
            delay(1.seconds)
        }
        //com.soywiz.korgw.osx.MacosGameController.main(arrayOf())
        //MacosGameController.main(arrayOf())
    }
}
*/
