import com.soywiz.klock.*
import com.soywiz.korge.*
import com.soywiz.korge.tween.*
import com.soywiz.korge.view.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.format.*
import com.soywiz.korio.*
import com.soywiz.korio.file.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.lang.SystemProperties
import com.soywiz.korma.geom.*
import com.soywiz.korma.interpolation.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.*
import java.util.*
import kotlin.jvm.*
import kotlin.system.*

object HelloWorld {
    @JvmStatic
    fun main(args: Array<String>) = runBlocking {
        for ((k, v) in SystemProperties.getAll()) {
            println("$k=$v")
        }
        //System.setProperty("jna.library.path", File("").absolutePath)
        Korge(width = 512, height = 512, bgcolor = Colors["#2b2b2b"]) {
            val minDegrees = (-16).degrees
            val maxDegrees = (+16).degrees

            val image = image(resourcesVfs["korge.png"].readBitmap()) {
                rotation = maxDegrees
                anchor(.5, .5)
                scale(.8)
                position(256, 256)
            }

            while (true) {
                image.tween(image::rotation[minDegrees], time = 1.seconds, easing = Easing.EASE_IN_OUT)
                image.tween(image::rotation[maxDegrees], time = 1.seconds, easing = Easing.EASE_IN_OUT)
            }
        }
    }
}

