import com.soywiz.klock.*
import com.soywiz.klogger.Console
import com.soywiz.korge.*
import com.soywiz.korge.render.*
import com.soywiz.korge.tween.*
import com.soywiz.korge.view.*
import com.soywiz.korim.color.*
import com.soywiz.korim.format.*
import com.soywiz.korio.file.std.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.vector.*
import com.soywiz.korma.interpolation.*
import com.soywiz.korge.input.*
import com.soywiz.korge.service.storage.NativeStorage
import com.soywiz.korge.time.delay

suspend fun main() = Korge(width = 512, height = 512, bgcolor = Colors["#2b2b9b"]) {
	val minDegrees = (-16).degrees
	val maxDegrees = (+16).degrees

    this.mouse {
        down { println(it.button) }
        up { println(it.button) }
    }

	val image = image(resourcesVfs["korge.png"].readBitmap()) {
		rotation = maxDegrees
		anchor(.5, .5)
		scale(.8)
		position(256, 256)
	}

    var i = 0
	while (true) {
        // Add the below three lines:
        i++
        val key = "key$i"
        val storage = NativeStorage(views)
        storage[key] = DateTime.now().toString()
        println("Storage: " + storage + ", $key: " + storage.getOrNull(key))
        delay(1.seconds)
	}
}
