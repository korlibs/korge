package com.soywiz.korge.ui

import com.soywiz.klock.*
import com.soywiz.korge.animate.*
import com.soywiz.korge.annotations.*
import com.soywiz.korge.input.*
import com.soywiz.korge.style.*
import com.soywiz.korge.time.*
import com.soywiz.korge.tween.*
import com.soywiz.korge.view.*
import com.soywiz.korim.color.*
import com.soywiz.korim.font.*
import com.soywiz.korio.lang.*
import com.soywiz.korma.geom.*

@KorgeExperimental
inline fun Container.uiTooltipContainer(
    block: @ViewDslMarker Container.(UITooltipContainer) -> Unit = {}
): UITooltipContainer = UITooltipContainer()
    .addTo(this).also { block(it.content, it) }

class UITooltipContainer() : Container() {
    val content = container()
    val tooltip = uiText("tooltip") {
        bgcolor = Colors["#303030"]
        visible = false
    }

    var tooltipFont: Font ; get() = tooltip.styles.textFont ; set(value) { tooltip.styles.textFont = value }
    var tooltipColor: RGBA ; get() = tooltip.styles.textColor ; set(value) { tooltip.styles.textColor = value }
    var tooltipBackgroundColor: RGBA ; get() = tooltip.bgcolor ; set(value) { tooltip.bgcolor = value }
    var showTime: TimeSpan = 0.4.seconds
    var appearAnimationTime: TimeSpan = 0.2.seconds
    var tooltipOffsetX: Double = 0.0
    var tooltipOffsetY: Double = 4.0

    private var visibleTimer: Closeable? = null

    fun disappear() {
        tooltip.visible = false
        visibleTimer?.close()
        visibleTimer = null
    }

    fun appear() {
        disappear()
        visibleTimer = this.timers.timeout(showTime) {
            tooltip.alphaF = 0.0f
            tooltip.visible = true
            visibleTimer = null
            tooltip.simpleAnimator.tween(tooltip::alphaF[0.0f, 1.0f], time = appearAnimationTime)
        }
    }

    fun setPosition(view: View) {
        val bounds = view.globalBounds
        tooltip.globalPos(Point(bounds.left + tooltipOffsetX, bounds.bottom + tooltipOffsetY))
    }

    fun setText(text: String) {
        tooltip.text = text
    }

    fun show(view: View, text: String) {
        appear()
        setPosition(view)
        setText(text)
    }
}

fun <T : View> T.tooltip(tooltips: UITooltipContainer, text: String): T {
    this.onOutOnOver(
        out = { tooltips.disappear() },
        over = { tooltips.show(this, text) }
    )
    return this
}
