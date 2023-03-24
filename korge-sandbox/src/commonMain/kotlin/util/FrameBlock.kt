package util

import korlibs.time.Frequency
import korlibs.time.measure
import korlibs.time.milliseconds
import korlibs.time.timesPerSecond
import korlibs.korge.time.*
import korlibs.korge.view.View
import korlibs.korge.view.Views
import korlibs.korge.view.views

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
