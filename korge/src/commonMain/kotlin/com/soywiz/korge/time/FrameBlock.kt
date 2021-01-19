package com.soywiz.korge.time

import com.soywiz.klock.*
import com.soywiz.korge.view.*

/**
 * Introduces a block, where a `frame()` function is available
 *
 * Example:
 *
 * ```
 * frameBlock(144.timesPerSecond) {
 *   while (true) {
 *     image.x++
 *     frame()
 *   }
 * }
 * ```
 */
suspend fun <T> View.frameBlock(fps: Frequency = 60.timesPerSecond, block: suspend FrameBlock.() -> T): T =
    block(FrameBlock(this, views(), fps))

class FrameBlock(private val view: View, private val views: Views, private val fps: Frequency) {
    private val frameTime = fps.timeSpan
    private var accumulatedWaitedTime = 0.milliseconds

    suspend fun frame() {
        while (accumulatedWaitedTime < frameTime) {
            accumulatedWaitedTime += views.timeProvider.measure {
                view.timers.waitFrame()
            }
        }
        if (accumulatedWaitedTime >= frameTime) {
            accumulatedWaitedTime -= frameTime
        }
    }
}

