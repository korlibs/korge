package com.soywiz.korge.ui

import com.soywiz.klock.TimeSpan
import com.soywiz.klock.seconds
import com.soywiz.korge.annotations.KorgeExperimental
import com.soywiz.korge.component.detach
import com.soywiz.korge.input.onOutOnOver
import com.soywiz.korge.time.timers
import com.soywiz.korge.tween.TweenComponent
import com.soywiz.korge.tween.get
import com.soywiz.korge.tween.tweenNoWait
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.View
import com.soywiz.korge.view.ViewDslMarker
import com.soywiz.korge.view.addTo
import com.soywiz.korge.view.container
import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RGBA
import com.soywiz.korim.font.Font
import com.soywiz.korio.lang.Closeable

@KorgeExperimental
inline fun Container.uiTooltipContainer(
    font: Font = DefaultUIFont,
    block: @ViewDslMarker Container.(UITooltipContainer) -> Unit = {}
): UITooltipContainer = UITooltipContainer(font)
    .addTo(this).also { block(it.content, it) }

class UITooltipContainer(font: Font = DefaultUIFont) : Container() {
    val content = container()
    val tooltip = uiText("tooltip") {
        textFont = font
        textColor = Colors.WHITE
        bgcolor = Colors["#303030"]
        visible = false
    }

    var tooltipFont: Font ; get() = tooltip.textFont ; set(value) { tooltip.textFont = value }
    var tooltipColor: RGBA ; get() = tooltip.textColor ; set(value) { tooltip.textColor = value }
    var tooltipBackgroundColor: RGBA ; get() = tooltip.bgcolor ; set(value) { tooltip.bgcolor = value }
    var showTime: TimeSpan = 0.4.seconds
    var appearAnimationTime: TimeSpan = 0.2.seconds
    var tooltipOffsetX: Double = 0.0
    var tooltipOffsetY: Double = 4.0

    private var visibleTimer: Closeable? = null
    private var visibleAppearTween: TweenComponent? = null

    fun disappear() {
        tooltip.visible = false
        visibleTimer?.close()
        visibleTimer = null
        visibleAppearTween?.detach()
        visibleAppearTween = null
    }

    fun appear() {
        disappear()
        visibleTimer = this.timers.timeout(showTime) {
            tooltip.alpha = 0.0
            tooltip.visible = true
            visibleTimer = null
            visibleAppearTween = tooltip.tweenNoWait(tooltip::alpha[0.0, 1.0], time = appearAnimationTime)
        }
    }

    fun setPosition(view: View) {
        val bounds = view.globalBounds
        tooltip.setGlobalXY(bounds.left + tooltipOffsetX, bounds.bottom + tooltipOffsetY)
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
