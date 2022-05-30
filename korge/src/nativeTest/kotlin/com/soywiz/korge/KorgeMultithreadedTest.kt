package com.soywiz.korge

import com.soywiz.klock.*
import com.soywiz.korag.shader.ShaderType
import com.soywiz.korev.Key
import com.soywiz.korge.input.keys
import com.soywiz.korge.input.mouse
import com.soywiz.korge.input.touch
import com.soywiz.korge.tests.*
import com.soywiz.korge.view.*
import com.soywiz.korge.view.filter.BlurFilter
import com.soywiz.korge.view.filter.ColorMatrixFilter
import com.soywiz.korge.view.filter.renderToTextureWithBorder
import com.soywiz.korim.format.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.std.*
import kotlinx.coroutines.*
import kotlin.native.concurrent.*
import kotlin.native.internal.test.*
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
        ColorMatrixFilter.getProgram(true)
        val log = runInWorker {
            val log = arrayListOf<String>()
            val viewsForTesting = ViewsForTesting()
            viewsForTesting.viewsTest(timeout = 5.seconds, cond = { true }) {
                val rect = solidRect(10, 10)
                    .filters(BlurFilter())
                    .filterScale(0.1)
                    .mask(solidRect(5, 5))
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
                ColorMatrixFilter.getProgram(true)
                ColorMatrixFilter.getProgram(false)
                this.views.render()
                log += "rect.filterScale=${rect.filterScale}"
                log += "rect.mask=${rect.mask != null}"
                log += "program=${ColorMatrixFilter.getProgram(true).fragment.type == ShaderType.FRAGMENT}"
            }
            log
        }
        ColorMatrixFilter.getProgram(false)
        assertEquals(
            """
                rect.filterScale=0.125
                rect.mask=true
                program=true
            """.trimIndent(),
            log.joinToString("\n")
        )
    }

    fun <T> runInWorker(block: () -> T): T {
        val worker = Worker.start()
        return try {
            worker.execute(TransferMode.SAFE, { block.freeze() }) { it().freeze() }.result
        } finally {
            worker.requestTermination(processScheduledJobs = false)
        }
    }
}
