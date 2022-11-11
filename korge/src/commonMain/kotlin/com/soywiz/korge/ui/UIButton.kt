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
import com.soywiz.korma.geom.*
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
    text: String = "",
    icon: BmpSlice? = null,
) : UIView(width, height) {
    companion object {
        const val DEFAULT_WIDTH = UI_DEFAULT_WIDTH
        const val DEFAULT_HEIGHT = UI_DEFAULT_HEIGHT
    }

    @Deprecated("Use uiSkin instead")
    var skin: UISkin? get() = uiSkin ; set(value) { uiSkin = value }

	var forcePressed = false
    var radius = 6.pt
        set(value) {
            field = value
            setInitialState()
        }
    //var radius = 100.percent

    fun radiusPoints(): Double {
        return radius.calc(Length.Context().setSize(min(width, height).toInt() / 2)).toDouble()
    }

    //val radiusRatioHalf get() = radiusRatio * 0.5
    var bgColorOut = Colors["#1976d2"]
        set(value) {
            field = value
            bgcolor = value
        }
    var bgColorOver = Colors["#1B5AB3"]
    var elevation = true
        set(value) {
            field = value
            setInitialState()
        }
    var bgColorDisabled = Colors["#00000033"]

    var bgcolor: RGBA = bgColorOut
        set(value) {
            field = value
            invalidateRender()
        }


    //protected val rect: NinePatchEx = ninePatch(null, width, height)
    //protected val background = roundRect(
    //    width, height, radiusWidth(width), radiusHeight(height), bgColorOut)
    //    .also { it.renderer = GraphicsRenderer.SYSTEM }
    //    //.filters(DropshadowFilter(0.0, 3.0, shadowColor = Colors.BLACK.withAd(0.126)))
    //    .also { it.mouseEnabled = false }

    var newSkin: NewUIButtonSkin = DefaultUISkin

    val canvas = renderableView {
        newSkin.renderUIButton(this, this@UIButton)
    }
    //internal val background = FastMaterialBackground(width, height).addTo(this)
    //    .also { it.colorMul = bgColorOut }
    //    .also { it.mouseEnabled = false }

    //protected val textShadowView = text("", 16.0)
    protected val textView = text(text, 16.0)
    protected val iconView = image(Bitmaps.transparent)
	protected var bover = false
	protected var bpressing = false
    val animator = animator(parallel = true, defaultEasing = Easing.LINEAR)
    val animatorEffects = animator(parallel = true, defaultEasing = Easing.LINEAR)

    var textColor: RGBA by textView::color
    var text: String by textView::text

    private fun setInitialState() {
        val width = width
        val height = height
        canvas.setSize(width, height)
        //background.setSize(width, height)
        //background.radius = RectCorners(radiusWidth(width))
        //background.shadowRadius = if (elevation) 10.0 else 0.0
        //textView.setSize(width, height)

        textView.setTextBounds(Rectangle(0.0, 0.0, width, height))
        textView.text = text
        textView.alignment = TextAlignment.MIDDLE_CENTER

        fitIconInRect(iconView, icon ?: Bitmaps.transparent, width, height, Anchor.MIDDLE_CENTER)
        iconView.alpha = when {
            !enabled -> 0.5
            bover -> 1.0
            else -> 1.0
        }
        invalidateRender()
    }

    var icon = icon
        set(value) {
            field = value
            setInitialState()
        }

    override fun onSizeChanged() {
        setInitialState()
    }

    fun simulateOver() {
        if (bover) return
		bover = true
        newSkin.updatedUIButton(this, over = true)
	}

	fun simulateOut() {
        if (!bover) return
		bover = false
        newSkin.updatedUIButton(this, over = false)
	}

	fun simulatePressing(value: Boolean) {
        if (bpressing == value) return
		bpressing = value
	}

	fun simulateDown(x: Double = width * 0.5, y: Double = height * 0.5) {
        if (bpressing) return
		bpressing = true
        newSkin.updatedUIButton(this, down = true, px = x, py = y)
	}

	fun simulateUp() {
        if (!bpressing) return
		bpressing = false
        newSkin.updatedUIButton(this, down = false)
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
        this.cursor = GameWindow.Cursor.HAND
        setInitialState()
    }

    override fun updateState() {
        super.updateState()
        newSkin.updatedUIButton(this)
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
