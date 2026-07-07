package org.korge.e2e.jvm

import korlibs.image.bitmap.Bitmap32
import korlibs.image.color.Colors
import korlibs.image.format.PNG
import korlibs.image.format.writeBitmap
import korlibs.io.file.std.localVfs
import korlibs.io.lang.Environment
import korlibs.io.serialization.json.Json
import korlibs.korge.Korge
import korlibs.korge.time.delayFrame
import korlibs.korge.view.renderToBitmap
import korlibs.math.geom.Size2D
import org.korge.e2e.EmptyE2ETestCase
import org.korge.e2e.FiltersE2ETestCase
import org.korge.e2e.IdentityFilterE2ETestCase

suspend fun main() = Korge(windowSize = Size2D(768, 512), backgroundColor = Colors["#2b2b2b"]) {
    val exceptions = arrayListOf<Throwable>()
    try {
        // Test cases
        val cases = listOf(
            EmptyE2ETestCase,
            FiltersE2ETestCase,
            IdentityFilterE2ETestCase,
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
                fun Bitmap32.scaled(): Bitmap32 = when (case.scale) {
                    1.0 -> this
                    else -> this.scaleLinear(case.scale, case.scale)
                }

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
