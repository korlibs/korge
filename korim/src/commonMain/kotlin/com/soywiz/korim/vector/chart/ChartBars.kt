package com.soywiz.korim.vector.chart

import com.soywiz.korim.color.*
import com.soywiz.korim.text.*
import com.soywiz.korim.vector.*
import com.soywiz.korma.geom.*
import kotlin.math.*

open class ChartBars(val list: List<DataPoint>) : Chart() {
    companion object {
        operator fun invoke(vararg items: Pair<String, Number>): ChartBars =
            ChartBars(items.map { ChartBars.DataPoint(it.first, listOf(it.second.toDouble())) })

        fun fromPoints(vararg items: Pair<String, List<Number>>): ChartBars =
            ChartBars(items.map { ChartBars.DataPoint(it.first, it.second.map { it.toDouble() }) })
    }

    data class DataPoint(val name: String, val values: List<Double>) {
        val localMaxValue = values.max() ?: 0.0
    }

    override fun draw(c: Context2d) = c.renderChart()

    val maxValue = list.map { it.localMaxValue }.max() ?: 0.0
    val chartStep = 10.0.pow(floor(log10(maxValue))) / 2.0
    val rMaxValue = ceil(maxValue / chartStep) * chartStep

    val colors = listOf(Colors["#5485ec"], Colors.GREEN, Colors.BLUE, Colors.AZURE, Colors.CHARTREUSE, Colors.CADETBLUE)

    private fun Context2d.renderLine(
        x0: Double,
        y0: Double,
        x1: Double,
        y1: Double,
        color: RGBA,
        width: Double = 1.2
    ) {
        lineWidth = width.toDouble()
        beginPath()
        moveTo(x0, y0)
        lineTo(x1, y1)
        stroke(createColor(color))
    }

    private fun Context2d.renderRefLine(rect: Rectangle, y: Double, value: String, important: Boolean) {
        val x = rect.left
        renderLine(x, y, rect.right, y, if (important) Colors.BLACK else Colors.DARKGREY)
        fillText(
            value,
            x - 2,
            y,
            halign = HorizontalAlign.RIGHT,
            valign = VerticalAlign.BOTTOM,
            color = Colors.DARKGREY
        )
    }

    val DataPoint.ratio get() = values.first() / maxValue
    val DataPoint.rRatio get() = values.first() / rMaxValue

    private fun Context2d.renderReferenceLines(rect: Rectangle) {
        for (n in 0 until 5) {
            val ratio = n.toDouble() / 4
            renderRefLine(
                rect,
                rect.bottom - 1 - (rect.height - 1) * ratio,
                "${ratio * rMaxValue}".removeSuffix(".0"),
                important = (n == 0)
            )
        }
    }

    enum class Fit(val angle: Double) { FULL(0.0), DEG45(-45.0), DEG90(-90.0) }

    fun Context2d.renderBars(rect: Rectangle) {
        val barWidth = rect.width / (list.size * 1.5 + 0.5)
        val barLeft = barWidth * 0.5
        val barSpace = barWidth * 1.5

        renderReferenceLines(rect)

        val fit = list.map {
            val bounds = getTextBounds(it.name)
            when {
                bounds.bounds.width > barWidth * 2.0 -> Fit.DEG90
                bounds.bounds.width > barWidth -> Fit.DEG45
                else -> Fit.FULL
            }
        }.max() ?: Fit.FULL

        for (n in list.indices) {
            val item = list[n]
            val rx = rect.left + barLeft + barSpace * n
            fillStyle(colors[0]) {
                fillRect(rx, rect.bottom - 1, barWidth, -rect.height * item.rRatio)
            }
            keep {
                translate(rx + barWidth * 0.5, rect.bottom + 4)
                rotateDeg(fit.angle)
                //fillText(item.name, rx + barWidth * 0.5, rect.bottom, halign = HorizontalAlign.CENTER, valign = VerticalAlign.TOP)
                fillText(
                    item.name,
                    0,
                    0,
                    halign = if (fit == Fit.FULL) HorizontalAlign.CENTER else HorizontalAlign.RIGHT,
                    valign = VerticalAlign.MIDDLE,
                    color = Colors.DARKSLATEGRAY
                )
            }
        }
    }

    var count = 0
    override fun Context2d.renderChart() {
        //println("Context2d.renderChart:$width,$height")
        val hpadding = min(64.0, width * 0.1)
        val vpadding = min(64.0, height * 0.1)
        renderBars(Rectangle.fromBounds(hpadding, vpadding, width - hpadding, height - vpadding))
    }
}
