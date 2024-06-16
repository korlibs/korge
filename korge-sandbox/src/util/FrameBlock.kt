package util

import korlibs.korge.time.*
import korlibs.korge.view.View
import korlibs.korge.view.Views
import korlibs.korge.view.views
import korlibs.time.*

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
    private val frameTime = fps.fastDuration
    private var accumulatedWaitedTime = 0.fastMilliseconds

    suspend fun frame() {
        while (accumulatedWaitedTime < frameTime) {
            accumulatedWaitedTime += views.timeProvider.measureFast {
                view.timers.waitFrame()
            }
        }
        if (accumulatedWaitedTime >= frameTime) {
            accumulatedWaitedTime -= frameTime
        }
    }
}
