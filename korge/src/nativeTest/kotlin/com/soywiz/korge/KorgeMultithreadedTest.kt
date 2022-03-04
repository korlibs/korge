package com.soywiz.korge

import com.soywiz.klock.*
import com.soywiz.korge.tests.*
import com.soywiz.korge.view.*
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
        val worker = Worker.start()
        val result = try {
            worker.execute(TransferMode.SAFE, { Unit.freeze() }) {
                println("[1]")
                val log = arrayListOf<String>()
                println("[2]")
                runBlocking {
                    val imageInfo = resourcesVfs["Exif5-2x.png"].readImageInfo(PNG)
                    log.add("orientationSure=${imageInfo?.orientationSure}")
                    imageInfo?.orientation = ImageOrientation.MIRROR_HORIZONTAL
                    log.add("orientationSure=${imageInfo?.orientationSure}")
                }
                /*
                val viewsForTesting = ViewsForTesting()
                viewsForTesting.viewsTest(timeout = 5.seconds, cond = { true }) {
                    println("[3]")
                    try {
                        val imageInfo = resourcesVfs["Exif5-2x.png"].readImageInfo(PNG)
                        println("[4]")
                        log.add("orientationSure=${imageInfo?.orientationSure}")
                        println("[5]")
                    } catch (e: Throwable) {
                        println("[6e]")
                        e.printStackTrace()
                    }
                }
                 */
                log.freeze()
            }.result
        } finally {
            worker.requestTermination(processScheduledJobs = false)
        }
        assertEquals(
            """
                orientationSure=ImageOrientation(rotation=R270, flipX=true, flipY=false)
                orientationSure=ImageOrientation(rotation=R0, flipX=true, flipY=false)
            """.trimIndent(),
            result.joinToString("\n")
        )
    }
}
