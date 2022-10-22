package com.soywiz.korge

import com.soywiz.klock.*
import com.soywiz.korag.shader.ShaderType
import com.soywiz.korev.Key
import com.soywiz.korge.input.keys
import com.soywiz.korge.input.mouse
import com.soywiz.korge.input.touch
import com.soywiz.korge.tests.*
import com.soywiz.korge.view.*
import com.soywiz.korge.view.filter.*
import com.soywiz.korge.view.mask.mask
import com.soywiz.korge.view.vector.gpuShapeView
import com.soywiz.korim.format.*
import com.soywiz.korio.file.std.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.bezier.isConvex
import com.soywiz.korma.geom.shape.buildVectorPath
import com.soywiz.korma.geom.vector.circle
import com.soywiz.korma.geom.vector.getCurves
import com.soywiz.korma.geom.vector.getCurvesList
import com.soywiz.korma.geom.vector.lineTo
import com.soywiz.korma.geom.vector.moveTo
import com.soywiz.korma.geom.vector.roundRect
import kotlinx.coroutines.*
import kotlin.native.concurrent.*
import kotlin.test.*

class KorgeMultithreadedTest {
    @Test
    fun test() {
        val result = runInWorker {
            println("[1]")
            val log = arrayListOf<String>()
            println("[2]")
            runBlocking {
                val imageInfo = resourcesVfs["Exif5-2x.png"].readImageInfo(PNG)
                log.add("orientationSure=${imageInfo?.orientationSure}")
                imageInfo?.orientation = ImageOrientation.MIRROR_HORIZONTAL
                log.add("orientationSure=${imageInfo?.orientationSure}")
            }
            log
        }
        assertEquals(
            """
                orientationSure=ImageOrientation(rotation=R270, flipX=true, flipY=false)
                orientationSure=ImageOrientation(rotation=R0, flipX=true, flipY=false)
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
                val path = buildVectorPath { circle(0, 0, 100) }
                path.getCurvesList()
                path.moveTo(200, 200)
                path.lineTo(300, 300)
                path.getCurvesList()
                val curves = path.getCurves()

                assertEquals(Unit, Mesh().getLocalBoundsInternal(Rectangle()), "Doesn't throw with mutability exception")

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
