package com.soywiz.korge.view

import com.soywiz.kds.*
import com.soywiz.klock.*
import com.soywiz.klock.hr.*
import com.soywiz.kmem.convertRange
import com.soywiz.korge.bitmapfont.*
import com.soywiz.korge.render.*
import com.soywiz.korim.color.*
import com.soywiz.korma.math.*

internal fun ViewsContainer.installFpsDebugOverlay() {
    val longWindow = TimeSlidingWindow(240)
    val mediumWindow = TimeSlidingWindow(60)
    val shortWindow = TimeSlidingWindow(10)

    var previousTime = PerformanceCounter.hr
    var frames = 0

    views.debugHandlers.add { ctx ->
        val scale = ctx.ag.devicePixelRatio

        val fontSize = 8.0 * scale
        val currentTime = PerformanceCounter.hr
        val elapsedTime = (currentTime - previousTime)

        if (frames > 3) {
            // @TODO: We are discarding for now too low values. We have to check why this happening. Maybe because vsync is near?
            if (elapsedTime > 4.hrMilliseconds) {
                longWindow.add(elapsedTime)
                mediumWindow.add(elapsedTime)
                shortWindow.add(elapsedTime)
            }
        }

        frames++
        previousTime = currentTime

        fun drawTextWithShadow(text: String, x: Int, y: Int) {
            ctx.drawText(Fonts.defaultFont, fontSize, text, x = x + 1, y = y + 1, colMul = Colors.BLACK)
            ctx.drawText(Fonts.defaultFont, fontSize, text, x = x, y = y, colMul = Colors.WHITE)
        }

        drawTextWithShadow("FPS: " +
            "${shortWindow.avgFps.roundDecimalPlaces(1)}, range: [" +
            "${mediumWindow.minFps.roundDecimalPlaces(1)}-" +
            "${mediumWindow.maxFps.roundDecimalPlaces(1)}]",
            0, 0
        )

        val graphLeft = 1f
        val graphTop = (fontSize * 2).toFloat()
        val overlayLines = 60
        val overlayWidth = 120f * scale.toFloat()
        val overlayHeight = 30 * scale
        val overlayHeightGap = 5.0

        // y-axis
        renderContext.debugLineRenderContext.line(
            graphLeft, graphTop,
            graphLeft, graphTop + overlayHeight.toFloat()
        )
        // x-axis
        renderContext.debugLineRenderContext.line(
            graphLeft, graphTop + overlayHeight.toFloat(),
            graphLeft + overlayWidth, graphTop + overlayHeight.toFloat()
        )

        val ratio = longWindow.size.toDouble() / longWindow.capacity.toDouble()
        val totalOverlayLines = (overlayLines * ratio).toInt().coerceAtLeast(1)
        if (longWindow.size > 0) {
            var previousX = 0f
            var previousY = 0f
            val minFps = longWindow.minFps
            val maxFps = longWindow.maxFps
            for (n in 0 until totalOverlayLines) {
                // Compute fps sample
                val fps = run {
                    val p0 = n.convertRange(0, totalOverlayLines, 0, longWindow.size.coerceAtLeast(1))
                    val p1 = (n + 1).convertRange(0, totalOverlayLines, 0, longWindow.size.coerceAtLeast(1))
                    var plen = 0
                    var timeSum = 0.hrMicroseconds
                    for (m in p0 until p1.coerceAtMost(longWindow.size)) {
                        timeSum += longWindow[m]
                        plen++
                    }
                    if (plen == 0) {
                        timeSum += longWindow[p0]
                        plen++
                    }
                    val time = (timeSum.timeSpan / plen.toDouble())
                    val fps = time.toFrequency().hertz
                    //print("$fps[$p0,$p1]{$minFps,$maxFps},")
                    fps
                }
                val scaledFreq = fps.convertRange(
                    minFps, maxFps,
                    overlayHeightGap, overlayHeight
                )
                val y = (graphTop + overlayHeight - scaledFreq).toFloat()
                val x = graphLeft + (n.toFloat() * overlayWidth / overlayLines.toFloat())
                if (n > 0) {
                    renderContext.debugLineRenderContext.line(previousX, previousY, x, y)
                }
                previousY = y
                previousX = x
            }
            //println()
        }
    }
}

private class TimeSlidingWindow(val capacity: Int) {
    private val deque = IntDeque(capacity)
    private var totalMicroseconds = 0

    val size get() = deque.size

    val avg: HRTimeSpan get() = (totalMicroseconds.toDouble() / deque.size).hrMicroseconds
    // @TODO: Can we compute this incrementally?
    val min: HRTimeSpan get() = deque.min()?.hrMicroseconds ?: 1.hrMicroseconds
    val max: HRTimeSpan get() = deque.max()?.hrMicroseconds ?: 1.hrMicroseconds

    val avgFps: Double get() = 1.hrSeconds / avg
    val minFps: Double get() = 1.hrSeconds / max
    val maxFps: Double get() = 1.hrSeconds / min

    operator fun get(index: Int): HRTimeSpan = deque[index].hrMicroseconds

    fun add(value: HRTimeSpan) {
        val intValue = value.microsecondsInt
        deque.add(intValue)
        totalMicroseconds += intValue
        if (deque.size > capacity) {
            totalMicroseconds -= deque.removeFirst()
        }
    }
}
