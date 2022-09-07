import com.soywiz.klock.*
import com.soywiz.korge.*
import com.soywiz.korge.component.docking.dockedTo
import com.soywiz.korge.time.*
import com.soywiz.korge.view.*
import com.soywiz.korge.view.filter.*
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.bitmap.resized
import com.soywiz.korim.color.*
import com.soywiz.korim.format.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.serialization.json.Json
import com.soywiz.korma.geom.Anchor
import com.soywiz.korma.geom.ScaleMode
import com.soywiz.korma.geom.degrees

suspend fun main() = Korge(width = 768, height = 512, bgcolor = Colors["#2b2b2b"]) {
    val exceptions = arrayListOf<Throwable>()
    try {
        // Test cases
        val cases = listOf(
            EmptyE2ETestCase,
            FiltersE2ETestCase,
            IdentityFilterE2ETestCase
        )

        println("Determining screenshotsVfs...")
        val path = Environment["OUTPUT_DIR"] ?: "."
        println("Determining screenshotsVfs... $path")
        val screenshotsVfs = localVfs(path).also { it.mkdirs() }.jail()
        println("vfs: $screenshotsVfs")

        for (case in cases) {
            println("STARTING ${case.name}...")
            try {
                removeChildren()
                case.run(this)
            } catch (e: Throwable) {
                e.printStackTrace()
                exceptions.add(e)
            }
            delayFrame()
            println("TAKING SCREENSHOT for ${case.name}")
            try {
                //val SCALE_WIDTH = 768 / 4
                //val SCALE_HEIGHT = 512 / 4
                fun Bitmap32.scaled(): Bitmap32 = when (case.scale) {
                    1.0 -> this
                    else -> this.scaleLinear(case.scale, case.scale)
                }
                //fun Bitmap32.scaled() = this.scaledFixed(SCALE_WIDTH, SCALE_HEIGHT, smooth = true)
                //fun Bitmap32.scaled() = this.resized(SCALE_WIDTH, SCALE_HEIGHT, ScaleMode.SHOW_ALL, Anchor.CENTER).toBMP32()


                screenshotsVfs["${case.name}.png"].writeBitmap(
                    //stage.renderToBitmap(views).scaleLinear(SCALE_WIDTH.toDouble(), SCALE_HEIGHT.toDouble()),
                    stage.renderToBitmap(views).scaled(),
                    PNG
                )
                screenshotsVfs["${case.name}.json"].writeString(Json.stringify(mapOf(
                    "scale" to case.scale,
                    "pixelPerfect" to case.pixelPerfect,
                )))
                println("SCREENSHOT TAKEN")
            } catch (e: Throwable) {
                e.printStackTrace()
                println("SCREENSHOT NOT TAKEN (error ocurred)")
            }
        }
    } catch (e: Throwable) {
        e.printStackTrace()
        exceptions.add(e)
    } finally {
        val exitCode = if (exceptions.isEmpty()) 0 else -1
        println("CLOSING WINDOW: exitCode=$exitCode, exceptions=${exceptions.size}")
        gameWindow.close(exitCode)
        println("WINDOW CLOSED?")
    }
}

open class E2ETestCase {
    val name get() = this::class.portableSimpleName.removeSuffix("E2ETestCase")
    open val scale: Double = 1.0
    open val pixelPerfect: Boolean = false

    open suspend fun run(stage: Stage) {
        stage.run()
    }

    open suspend fun Container.run() {
    }
}

object FiltersE2ETestCase : E2ETestCase() {
    override val scale: Double = 0.25

    override suspend fun Container.run() {
        println("LOADING IMAGE...")
        val bitmap = resourcesVfs["korge.png"].readBitmap()
        println("PREPARING VIEWS...")
        image(bitmap).scale(.5).position(0, 0).addFilter(WaveFilter(time = 0.5.seconds))
        image(bitmap).scale(.5).position(256, 0).addFilter(BlurFilter(radius = 6.0))
        image(bitmap).scale(.5).position(512, 0).addFilter(TransitionFilter(TransitionFilter.Transition.SWEEP, reversed = false, spread = 1.0, ratio = 0.5))
        image(bitmap).scale(.5).position(0, 256).addFilter(PageFilter(hratio = 0.5, hamplitude1 = 20.0))
        image(bitmap).scale(.5).position(256, 256).addFilter(Convolute3Filter(Convolute3Filter.KERNEL_SHARPEN))
        image(bitmap).scale(.5).position(512, 256).addFilter(SwizzleColorsFilter("bgga"))
        println("VIEWS PREPARED")
    }
}

object IdentityFilterE2ETestCase : E2ETestCase() {
    override val pixelPerfect: Boolean = true

    override suspend fun Container.run() {
        solidRect(768, 512, Colors.CYAN)
        image(Bitmap32(766, 510, Colors.MAGENTA.premultiplied)).xy(1, 1).filters(IdentityFilter)
    }
}

object EmptyE2ETestCase : E2ETestCase() {
    override val pixelPerfect: Boolean = true

    override suspend fun Container.run() {
    }
}

