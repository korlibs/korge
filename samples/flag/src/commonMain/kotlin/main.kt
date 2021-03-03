import com.soywiz.klock.*
import com.soywiz.korge.*
import com.soywiz.korge.view.*
import com.soywiz.korge.view.filter.*
import com.soywiz.korim.color.*
import com.soywiz.korim.format.*
import com.soywiz.korio.file.std.*

suspend fun main() = Korge(width = 592, height = 592, bgcolor = Colors["#2b2b2b"]) {
	val bitmap = resourcesVfs["korge.png"].readBitmap()
	val flagFilter = FlagFilter()

    // "Flag Pole"
    solidRect(10, 582, Colors.BLACK) {
        position(30, 30)
    }
    solidRect(20, 5, Colors.BLACK) {
        position(25, 25)
    }

    // Flag
    image(bitmap) {
        position(40, 40)
        filter = flagFilter
    }

    // Propagates the wave over time
    addUpdater { dt: TimeSpan ->
        flagFilter.time = flagFilter.time.plus(dt)
    }
}
