import com.soywiz.klock.*
import com.soywiz.korge.*
import com.soywiz.korge.time.*
import com.soywiz.korge.view.*
import com.soywiz.korge.view.filter.*
import com.soywiz.korim.color.*
import com.soywiz.korim.format.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.lang.*

suspend fun main() = Korge(width = 768, height = 512, bgcolor = Colors["#2b2b2b"]) {
    val exceptions = arrayListOf<Throwable>()
    try {
        // Test cases
        val cases = listOf(
            FiltersE2ETestCase,
        )

        val screenshotsVfs = localVfs(Environment["OUTPUT_DIR"] ?: ".").also { it.mkdir() }.jail()
        println("vfs: $screenshotsVfs")

        for (case in cases) {
            println("STARTING ${case.name}...")
            try {
                case.run(this)
            } catch (e: Throwable) {
                e.printStackTrace()
                exceptions.add(e)
            }
            delayFrame()
            println("TAKING SCREENSHOT for ${case.name}")
            screenshotsVfs["${case.name}.png"].writeBitmap(stage.renderToBitmap(views), PNG)
            println("SCREENSHOT TAKEN")
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
    val name get() = this::class.portableSimpleName

    open suspend fun run(stage: Stage) {
        stage.run()
    }

    open suspend fun Container.run() {
    }
}

object FiltersE2ETestCase : E2ETestCase() {
    override suspend fun Container.run() {
        println("LOADING IMAGE...")
        val bitmap = resourcesVfs["korge.png"].readBitmap()
        println("PREPARING VIEWS...")
        image(bitmap).scale(.5).position(0, 0).addFilter(WaveFilter(time = 0.5.seconds))
        image(bitmap).scale(.5).position(256, 0).addFilter(BlurFilter(initialRadius = 6.0))
        image(bitmap).scale(.5).position(512, 0).addFilter(TransitionFilter(TransitionFilter.Transition.SWEEP, reversed = false, smooth = true, ratio = 0.5))
        image(bitmap).scale(.5).position(0, 256).addFilter(PageFilter(hratio = 0.5, hamplitude1 = 20.0))
        image(bitmap).scale(.5).position(256, 256).addFilter(Convolute3Filter(Convolute3Filter.KERNEL_SHARPEN))
        image(bitmap).scale(.5).position(512, 256).addFilter(SwizzleColorsFilter("bgga"))
        println("VIEWS PREPARED")
    }
}

