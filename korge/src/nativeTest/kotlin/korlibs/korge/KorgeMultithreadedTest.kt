package korlibs.korge

import korlibs.time.*
import korlibs.logger.*
import korlibs.graphics.shader.*
import korlibs.event.*
import korlibs.korge.input.*
import korlibs.korge.tests.*
import korlibs.korge.view.*
import korlibs.korge.view.filter.*
import korlibs.korge.view.mask.*
import korlibs.korge.view.vector.*
import korlibs.image.format.*
import korlibs.io.file.std.*
import korlibs.math.geom.*
import korlibs.math.geom.bezier.*
import korlibs.math.geom.shape.*
import korlibs.math.geom.vector.*
import kotlinx.coroutines.*
import kotlin.native.concurrent.*
import kotlin.test.*

class KorgeMultithreadedTest {
    val logger = Logger("KorgeMultithreadedTest")

    @Test
    fun test() {
        val result = runInWorker {
            logger.info { "[1]" }
            val log = arrayListOf<String>()
            logger.info { "[2]" }
            runBlocking {
                val imageInfo = resourcesVfs["Exif5-2x.png"].readImageInfo(PNG)
                log.add("orientationSure=${imageInfo?.orientationSure}")
                imageInfo?.orientation = ImageOrientation.MIRROR_HORIZONTAL_ROTATE_0
                log.add("orientationSure=${imageInfo?.orientationSure}")
            }
            log
        }
        assertEquals(
            """
                orientationSure=MIRROR_HORIZONTAL_ROTATE_270
                orientationSure=MIRROR_HORIZONTAL_ROTATE_0
            """.trimIndent(),
            result.joinToString("\n")
        )
    }

    @Test
    fun test2() {
        ColorMatrixFilter.getProgram()
        val log = runInWorker {
            val log = arrayListOf<String>()
            val viewsForTesting = ViewsForTesting()
            viewsForTesting.viewsTest(timeout = 5.seconds, cond = { true }) {
                val rect = solidRect(10, 10)
                    .filters(BlurFilter(), filterScale = 0.1)
                    .mask(solidRect(5, 5))
                val gpuShapeView = gpuShapeView({ roundRect(0, 0, 200, 100, 10, 10) })
                this.views.render()
                rect.keys {
                    down(Key.UP) { }
                    down(Key.DOWN) { }
                }
                rect.mouse {
                    onDown {  }
                }
                rect.touch {
                    start { }
                    end { }
                }
                ColorMatrixFilter.getProgram()
                this.views.render()
                val path = buildVectorPath { circle(Point(0, 0), 100f) }
                path.getCurvesList()
                path.moveTo(Point(200, 200))
                path.lineTo(Point(300, 300))
                path.getCurvesList()
                val curves = path.getCurves()

                //assertEquals(Rectangle.INFINITE, Mesh().getLocalBoundsInternal(), "Doesn't throw with mutability exception")

                log += "rect.filterScale=${rect.filterScale}"
                log += "rect.mask=${rect.mask != null}"
                log += "program=${ColorMatrixFilter.getProgram().fragment.type == ShaderType.FRAGMENT}"
                log += "curves=${curves.beziers.size}"
                log += "convex=${curves.isConvex}"
            }
            log
        }
        ColorMatrixFilter.getProgram()
        assertEquals(
            """
                rect.filterScale=0.125
                rect.mask=true
                program=true
                curves=5
                convex=false
            """.trimIndent(),
            log.joinToString("\n")
        )
    }

    fun <T> runInWorker(block: () -> T): T {
        block.freeze()
        block()

        val worker = Worker.start()
        return try {
            worker.execute(TransferMode.SAFE, { block.freeze() }) { it().freeze() }.result
        } finally {
            worker.requestTermination(processScheduledJobs = false)
        }
    }
}
