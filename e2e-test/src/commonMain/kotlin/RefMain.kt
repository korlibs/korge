import korlibs.time.*
import korlibs.korge.*
import korlibs.korge.time.*
import korlibs.korge.view.*
import korlibs.korge.view.filter.*
import korlibs.image.bitmap.Bitmap32
import korlibs.image.bitmap.resized
import korlibs.image.color.*
import korlibs.image.format.*
import korlibs.io.file.std.*
import korlibs.io.lang.*
import korlibs.io.serialization.json.Json
import korlibs.math.geom.*

suspend fun main() = Korge(windowSize = Size(768, 512), backgroundColor = Colors["#2b2b2b"]) {
    val exceptions = arrayListOf<Throwable>()
    try {
        // Test cases
        val cases = listOf(
            EmptyE2ETestCase,
            FiltersE2ETestCase,
            IdentityFilterE2ETestCase,
            //DirectionalBlurE2ETestCase,
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
        val bitmap = KR.gfx.korge.korge.read()
        val bitmap2 = resourcesVfs["gfx/korge/korge.png"].readBitmap()
        println("PREPARING VIEWS...")
        image(bitmap).scale(.5).position(0, 0).addFilter(WaveFilter(time = 0.5.seconds, crestDistance = Vector2(256f, 128f)))
        image(bitmap).scale(.5).position(256, 0).addFilter(BlurFilter(radius = 6f))
        image(bitmap).scale(.5).position(512, 0).addFilter(TransitionFilter(TransitionFilter.Transition.SWEEP, reversed = false, spread = 1f, ratio = 0.5f))
        image(bitmap2).scale(.5).position(0, 256).addFilter(PageFilter(hratio = 0.5f, hamplitude1 = 20f))
        image(bitmap2).scale(.5).position(256, 256).addFilter(Convolute3Filter(Convolute3Filter.KERNEL_SHARPEN))
        image(bitmap2).scale(.5).position(512, 256).addFilter(SwizzleColorsFilter("bgga"))
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

object DirectionalBlurE2ETestCase : E2ETestCase() {
    override suspend fun Container.run() {
        solidRect(size, Colors.WHITE)
        circle(32f, Colors.RED)
            .centered
            .dockedTo(Anchor.CENTER, ScaleMode.NO_SCALE)
            .filters(DirectionalBlurFilter(angle = 0.degrees, radius = 16f, expandBorder = true))
    }
}
