package com.soywiz.korge.ui

import com.soywiz.kds.iterators.*
import com.soywiz.klock.*
import com.soywiz.korge.animate.*
import com.soywiz.korge.debug.uiCollapsibleSection
import com.soywiz.korge.debug.uiEditableValue
import com.soywiz.korge.input.*
import com.soywiz.korge.render.RenderContext
import com.soywiz.korge.tween.*
import com.soywiz.korge.view.*
import com.soywiz.korge.view.filter.*
import com.soywiz.korgw.*
import com.soywiz.korim.bitmap.Bitmaps
import com.soywiz.korim.bitmap.BmpSlice
import com.soywiz.korim.color.*
import com.soywiz.korim.font.Font
import com.soywiz.korim.paint.*
import com.soywiz.korim.text.*
import com.soywiz.korio.async.Signal
import com.soywiz.korma.geom.Anchor
import com.soywiz.korma.geom.Rectangle
import com.soywiz.korma.interpolation.*
import com.soywiz.korui.UiContainer
import com.soywiz.korui.layout.*
import com.soywiz.korui.layout.HorizontalUiLayout.percent
import com.soywiz.korui.layout.HorizontalUiLayout.pt
import kotlin.math.*
import kotlin.reflect.*

inline fun Container.uiButton(
    label: String,
    icon: BmpSlice? = null,
    width: Double = UI_DEFAULT_WIDTH,
    height: Double = UI_DEFAULT_HEIGHT,
    block: @ViewDslMarker UIButton.() -> Unit = {}
): UIButton = UIButton(width, height, label, icon).addTo(this).apply(block)

@Deprecated("Use uiButton instead")
inline fun Container.uiButton(
    width: Double = UI_DEFAULT_WIDTH,
    height: Double = UI_DEFAULT_HEIGHT,
    text: String = "",
    icon: BmpSlice? = null,
    block: @ViewDslMarker UIButton.() -> Unit = {}
): UIButton = UIButton(width, height, text, icon).addTo(this).apply(block)

@Deprecated("Use uiButton instead")
inline fun Container.iconButton(
    width: Double = UI_DEFAULT_WIDTH,
    height: Double = UI_DEFAULT_HEIGHT,
    icon: BmpSlice? = null,
    block: @ViewDslMarker UIButton.() -> Unit = {}
): UIButton = UIButton(width, height, icon = icon).addTo(this).apply(block)

@Deprecated("Use uiButton instead")
inline fun Container.uiTextButton(
    width: Double = UI_DEFAULT_WIDTH,
    height: Double = UI_DEFAULT_HEIGHT,
    text: String = "Button",
    textFont: Font? = null,
    textSize: Double? = null,
    block: @ViewDslMarker UIButton.() -> Unit = {}
): UIButton = UIButton(width, height, text).apply {
    if (textFont != null) this.textFont = textFont
    if (textSize != null) this.textSize = textSize
}.addTo(this).apply(block)

typealias UITextButton = UIButton
typealias IconButton = UIButton

open class UIButton(
	width: Double = 128.0,
	height: Double = 32.0,
    var text: String = "",
    var icon: BmpSlice? = null,
) : UIView(width, height) {
    companion object {
        const val DEFAULT_WIDTH = UI_DEFAULT_WIDTH
        const val DEFAULT_HEIGHT = UI_DEFAULT_HEIGHT
    }

    @Deprecated("Use uiSkin instead")
    var skin: UISkin? get() = uiSkin ; set(value) { uiSkin = value }

	var forcePressed = false
    var radius = 5.pt
    //var radius = 100.percent

    private fun radiusWidth(width: Double): Double {
        return radius.calc(Length.Context().setSize(width.toInt() / 2)).toDouble()
    }

    private fun radiusHeight(height: Double): Double {
        return radius.calc(Length.Context().setSize(height.toInt() / 2)).toDouble()
    }

    //val radiusRatioHalf get() = radiusRatio * 0.5
    val bgColorOut = Colors["#1976d2"]
    val bgColorOver = Colors["#1B5AB3"]
    val bgColorDisabled = Colors["#00000033"]
    //protected val rect: NinePatchEx = ninePatch(null, width, height)
    protected val background = roundRect(
        width, height, radiusWidth(width), radiusHeight(height), bgColorOut)
        .filters(DropshadowFilter(0.0, 3.0, shadowColor = Colors.BLACK.withAd(0.126)))
        .also { it.mouseEnabled = false }
    var bgcolor: RGBA
        get() = background.fill as RGBA
        set(value) {
            background.fill = value
        }
    protected val effects = container()
    //protected val textShadowView = text("", 16.0)
    protected val textView = text("", 16.0)
    protected val iconView = image(Bitmaps.transparent)
	protected var bover = false
	protected var bpressing = false
    val animator = animator(parallel = true, defaultEasing = Easing.LINEAR)
    val animatorEffects = animator(parallel = true, defaultEasing = Easing.LINEAR)

    init {
        this.cursor = GameWindow.Cursor.HAND
    }

    override fun updateState() {
        super.updateState()
        val bgcolor = when {
            !enabled -> bgColorDisabled
            bover ->  bgColorOver
            else -> bgColorOut
        }
        animator.cancel().tween(this::bgcolor[bgcolor], time = 0.25.seconds)
    }

    override fun onSizeChanged() {
        val width = width
        val height = height
        background.setSize(width, height)
        background.rx = radiusWidth(width)
        background.ry = radiusHeight(height)
        textView.setSize(width, height)
        textView.alignment = TextAlignment.CENTER
        effects.forEachChild {
            it.setSize(width, height)
            if (it is RoundRect) {
                it.rx = background.rx
                it.ry = background.ry
            }
        }
    }

    fun addCircleHighlight(px: Double, py: Double) {
        val radius = hypot(width, height)
        animatorEffects.sequence(easing = Easing.EASE_IN) {
            val effect = effects.roundRect(width, height, radiusWidth(width), radiusHeight(height), fill = Colors.TRANSPARENT_BLACK)
            tween(V2Callback {
                val color = Colors.WHITE.premultiplied.mix(Colors.TRANSPARENT_BLACK.premultiplied, it.interpolate(0.4, 0.4))
                effect.fill = RadialGradientPaint(
                    px, py, 0.0, px, py, it.interpolate(0.1, radius)
                ).add(0.0, color).add(0.90, color).add(1.0, Colors.TRANSPARENT_BLACK)
            }, time = 0.3.seconds)
            //block { effect.removeFromParent() }
        }
    }

    fun removeCircleHighlights() {
        animatorEffects.sequence(easing = Easing.EASE_IN) {
            val children = effects.children.toList()
            parallel { children.fastForEach { hide(it, time = 0.2.seconds) } }
            parallel { children.fastForEach { block { it.removeFromParent() } } }
        }
    }

    override fun renderInternal(ctx: RenderContext) {
        val skin = realUiSkin
        //println("UIButton: skin=$skin")
        //rect.ninePatch = when {
        //    !enabled -> skin.buttonDisabled
        //    bpressing || forcePressed -> skin.buttonDown
        //    bover -> skin.buttonOver
        //    else -> skin.buttonNormal
        //}
        //rect.width = width
        //rect.height = height

        fitIconInRect(iconView, icon ?: Bitmaps.transparent, width, height, Anchor.MIDDLE_CENTER)
        iconView.alpha = when {
            !enabled -> 0.5
            bover -> 1.0
            else -> 1.0
        }

        if (text.isNotEmpty()) {
            val alignment = skin.buttonTextAlignment
            val font = skin.textFont
            val text = text
            val textSize = skin.textSize

            textView.setTextBounds(Rectangle(0.0, 0.0, width, height))
            textView.visible = true
            textView.text = text
            textView.alignment = alignment
            textView.font = font
            textView.textSize = textSize

            textView.color = skin.textColor
            //textShadowView.color = skin.shadowColor
            //textShadowView.position(skin.shadowPosition)
        } else {
            textView.visible = false
            //textShadowView.visible = false
        }
        super.renderInternal(ctx)
    }

    fun simulateOver() {
        if (bover) return
		bover = true
        updateState()
	}

	fun simulateOut() {
        if (!bover) return
		bover = false
        updateState()
	}

	fun simulatePressing(value: Boolean) {
        if (bpressing == value) return
		bpressing = value
        updateState()
	}

	fun simulateDown(x: Double = width * 0.5, y: Double = height * 0.5) {
        if (bpressing) return
		bpressing = true
        updateState()
        if (enabled) addCircleHighlight(x, y)
	}

	fun simulateUp() {
        if (!bpressing) return
		bpressing = false
        updateState()
        removeCircleHighlights()
	}

    val onPress = Signal<TouchEvents.Info>()

	init {
        singleTouch {
            start {
                //println("singleTouch.start")

                simulateDown(it.localX, it.localY)
            }
            endAnywhere {
                //println("singleTouch.endAnywhere")
                simulateUp()
            }
            tap {
                //println("singleTouch.tap")
                onPress(it)
            }
        }
		mouse {
			onOver {
                //if (!it.lastEmulated) {
                run {
                    //println("mouse.onOver: ${input.mouse}, ${input.activeTouches}")
                    simulateOver()
                }
			}
			onOut {
                //if (!it.lastEmulated) {
                run {
                    //println("mouse.onOut")
                    simulateOut()
                }
			}
		}
	}

    override fun buildDebugComponent(views: Views, container: UiContainer) {
        container.uiCollapsibleSection(UIButton::class.simpleName!!) {
            uiEditableValue(::text)
            uiEditableValue(::textSize, min = 1.0, max = 300.0)
        }
        super.buildDebugComponent(views, container)
    }
}

fun <T : UIButton> T.clicked(block: (T) -> Unit): T {
    onClick { block(this) }
    return this
}
