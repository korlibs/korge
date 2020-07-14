package com.soywiz.korge.view

import com.soywiz.kds.*
import com.soywiz.klock.*
import com.soywiz.kmem.*
import com.soywiz.korge.bitmapfont.*
import com.soywiz.korge.render.*
import com.soywiz.korim.color.*

internal fun ViewsContainer.installFpsDebugOverlay() {
    val frequencies = Deque<Int>()
    var previousTime = DateTime.now()
    //var globalMaxFps = 0
    var frames = 0
    val MAX_FREQUENCY_VALUES = 100
    views.debugHandlers.add { ctx ->
        val scale = ctx.ag.devicePixelRatio
        //val scale = 2.0

        val fontSize = 8.0 * scale
        val currentTime = DateTime.now()
        val elapsedTime = currentTime - previousTime
        val elapsedFrequency = elapsedTime.toFrequency().hertz.toIntRound()
        if (frames > 3) {
            frequencies += elapsedFrequency
            if (frequencies.size > MAX_FREQUENCY_VALUES) {
                frequencies.removeFirst()
            }
        }

        //println(frequencies.toList() + ", $elapsedTime, ${currentTime.unixMillisLong}, ${previousTime.unixMillisLong}")

        frames++
        previousTime = currentTime

        val minFps = frequencies.min() ?: 0
        val maxFps = frequencies.max() ?: 0

        fun drawTextWithShadow(text: String, x: Int, y: Int) {
            ctx.drawText(Fonts.defaultFont, fontSize, text, x = x + 1, y = y + 1, colMul = Colors.BLACK)
            ctx.drawText(Fonts.defaultFont, fontSize, text, x = x, y = y, colMul = Colors.WHITE)
        }

        drawTextWithShadow("${elapsedFrequency}fps : [${minFps}-${maxFps}]", 0, 0)

        val graphLeft = 1f
        val graphTop = (fontSize * 2).toFloat()
        val overlayWidth = 100 * scale
        val overlayHeight = 30 * scale
        val overlayHeightGap = 5f

        //globalMaxFps = kotlin.math.max(globalMaxFps, maxFps)
        var previousX = 0f
        var previousY = 0f
        renderContext.debugLineRenderContext.line(
            graphLeft, graphTop,
            graphLeft, graphTop + overlayHeight.toFloat()
        )
        renderContext.debugLineRenderContext.line(
            graphLeft, graphTop + overlayHeight.toFloat(),
            graphLeft + overlayWidth.toFloat(), graphTop + overlayHeight.toFloat()
        )

        for (n in 0 until frequencies.size) {
            val y = (overlayHeight - frequencies[n].toDouble().convertRange(
                0.0, maxFps.toDouble(),
                overlayHeightGap.toDouble(), overlayHeight.toDouble()
            ).toFloat() + graphTop).toFloat()
            val x = graphLeft + (n.toFloat() * overlayWidth.toFloat() / MAX_FREQUENCY_VALUES.toFloat())
            if (n > 0) {
                renderContext.debugLineRenderContext.line(previousX, previousY, x, y)
            }
            previousY = y
            previousX = x
        }
    }
}
