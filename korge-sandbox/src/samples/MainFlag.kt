package samples

import korlibs.event.*
import korlibs.image.color.*
import korlibs.image.format.*
import korlibs.io.file.std.*
import korlibs.korge.input.*
import korlibs.korge.scene.*
import korlibs.korge.view.*
import korlibs.korge.view.filter.*
import korlibs.time.*
import kotlin.time.*

class MainFlag : ScaledScene(592, 592) {
    override suspend fun SContainer.sceneMain() {
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
            scaleY = 0.5
            filter = flagFilter
        }

        // Propagates the wave over time
        addFastUpdater { dt ->
            //println("MainFlag.addUpdater: dt=$dt")
            flagFilter.fastTime = flagFilter.time.plus(dt)
            invalidateRender()
        }

        fun min(a: Float, b: Float): Float = if (a > b) b else a
        fun max(a: Float, b: Float): Float = if (a > b) a else b

        keys {
            down {
                when (it.key) {
                    Key.LEFT -> flagFilter.amplitude = kotlin.math.max(0.0, flagFilter.amplitude - 5)
                    Key.RIGHT -> flagFilter.amplitude = kotlin.math.min(100.0, flagFilter.amplitude + 5)
                    Key.DOWN -> flagFilter.crestCount = kotlin.math.max(0.0, flagFilter.crestCount - 0.5)
                    Key.UP -> flagFilter.crestCount = kotlin.math.min(10.0, flagFilter.crestCount + 0.5)
                    Key.PLUS, Key.RIGHT_BRACKET, Key.CLOSE_BRACKET -> flagFilter.cyclesPerSecond = kotlin.math.min(10.0, flagFilter.cyclesPerSecond + 0.5)
                    Key.MINUS, Key.LEFT_BRACKET, Key.OPEN_BRACKET -> flagFilter.cyclesPerSecond = kotlin.math.max(0.0, flagFilter.cyclesPerSecond - 0.5)
                    else -> Unit
                }
                println("amplitude = ${flagFilter.amplitude}, crestCount = ${flagFilter.crestCount}, cyclesPerSecond = ${flagFilter.cyclesPerSecond}")
            }
        }
    }
}
