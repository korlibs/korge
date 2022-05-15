package com.soywiz.korge.time

import com.soywiz.klock.measure
import com.soywiz.klock.milliseconds
import com.soywiz.klock.timesPerSecond
import com.soywiz.korge.tests.ViewsForTesting
import kotlin.test.Test
import kotlin.test.assertEquals

class FrameBlockTest : ViewsForTesting() {
    @Test
    fun test() = viewsTest {
        assertEquals(2000.milliseconds, views.timeProvider.measure {
            frameBlock(fps = 5.timesPerSecond) {
                for (n in 0 until 10) {
                    frame()
                }
            }
        })
        assertEquals(1000.milliseconds, views.timeProvider.measure {
            frameBlock(fps = 10.timesPerSecond) {
                for (n in 0 until 10) {
                    frame()
                }
            }
        })
        assertEquals(500.milliseconds, views.timeProvider.measure {
            frameBlock(fps = 20.timesPerSecond) {
                for (n in 0 until 10) {
                    frame()
                }
            }
        })
        assertEquals(250.milliseconds, views.timeProvider.measure {
            frameBlock(fps = 40.timesPerSecond) {
                for (n in 0 until 10) {
                    frame()
                }
            }
        })
    }
}
