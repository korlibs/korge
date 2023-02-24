package com.soywiz.korge.ui

import com.soywiz.klock.*
import com.soywiz.korev.*
import com.soywiz.korge.animate.*
import com.soywiz.korge.input.*
import com.soywiz.korge.tween.*
import com.soywiz.korge.view.*
import com.soywiz.korge.view.property.*
import com.soywiz.korgw.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.font.*
import com.soywiz.korim.text.*
import com.soywiz.korio.async.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.interpolation.*
import com.soywiz.korma.length.*
import com.soywiz.korma.length.LengthExtensions.Companion.pt
import kotlin.math.*

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

open class UIToggleableButton(
    width: Double = 128.0,
    height: Double = 32.0,
    text: String = "",
    icon: BmpSlice? = null,
    richText: RichTextData? = null,
) : UIButton(width, height, text, icon, richText) {
    var pressed: Boolean = false
}

open class UIButton(
	width: Double = 128.0,
	height: Double = 32.0,
    text: String = "",
    icon: BmpSlice? = null,
    richText: RichTextData? = null,
) : UIFocusableView(width, height) {
    companion object {
        const val DEFAULT_WIDTH = UI_DEFAULT_WIDTH
        const val DEFAULT_HEIGHT = UI_DEFAULT_HEIGHT
    }

    @Deprecated("Use uiSkin instead")
    var skin: UISkin? get() = uiSkin ; set(value) { uiSkin = value }

	var forcePressed = false
    var radius: Length = 6.pt
        set(value) {
            field = value
            setInitialState()
        }
    //var radius = 100.percent

    fun radiusPoints(): Double {
        return radius.calc(Length.Context().setSize(min(width, height).toInt() / 2)).toDouble()
    }

    //val radiusRatioHalf get() = radiusRatio * 0.5
    @ViewProperty
    var bgColorOut = MaterialColors.BLUE_700
        set(value) {
            field = value
            background.bgColor = value
        }
    @ViewProperty
    var bgColorOver = MaterialColors.BLUE_800
        set(value) {
            field = value
            updatedUIButton(immediate = true)
        }
    @ViewProperty
    var bgColorSelected = MaterialColors.BLUE_900
        set(value) {
            field = value
            updatedUIButton(immediate = true)
        }
    @ViewProperty
    var bgColorDisabled = Colors["#777777ff"]
        set(value) {
            field = value
            updatedUIButton(immediate = true)
        }
    @ViewProperty
    var elevation = true
        set(value) {
            field = value
            setInitialState()
        }

    //protected val rect: NinePatchEx = ninePatch(null, width, height)
    //protected val background = roundRect(
    //    width, height, radiusWidth(width), radiusHeight(height), bgColorOut)
    //    .also { it.renderer = GraphicsRenderer.SYSTEM }
    //    //.filters(DropshadowFilter(0.0, 3.0, shadowColor = Colors.BLACK.withAd(0.126)))
    //    .also { it.mouseEnabled = false }

    //var newSkin: NewUIButtonSkin = DefaultUISkin

    val background = uiMaterialLayer(width, height) {
        radius = IRectCorners(5.0)
    }
    //internal val background = FastMaterialBackground(width, height).addTo(this)
    //    .also { it.colorMul = bgColorOut }
    //    .also { it.mouseEnabled = false }

    //protected val textShadowView = text("", 16.0)
    @ViewProperty(min = 1.0, max = 300.0)
    var textSize: Double
        get() = richText.defaultStyle.textSize
        set(value) {
            richText = richText.withStyle(richText.defaultStyle.copy(textSize = value))
        }

    val textView = textBlock(richText ?: RichTextData(text, font = DefaultTtfFontAsBitmap), align = TextAlignment.MIDDLE_CENTER)

    @ViewProperty
    @ViewPropertyProvider(TextAlignment.Provider::class)
    var textAlignment: TextAlignment by textView::align

    protected val iconView = image(Bitmaps.transparent)
	protected var bover = false
	protected var bpressing = false
    val animator = animator(parallel = true, defaultEasing = Easing.LINEAR)
    val animatorEffects = animator(parallel = true, defaultEasing = Easing.LINEAR)

    @ViewProperty()
    var richText: RichTextData by textView::text

    @ViewProperty()
    var text: String
        get() = textView.text.text
        set(value) {
            richText = richText.withText(value)
        }

    var textColor: RGBA
        get() = richText.defaultStyle.color ?: Colors.WHITE
        set(value) {
            richText = richText.withStyle(style = richText.defaultStyle.copy(color = value))
        }

    private fun setInitialState() {
        val width = width
        val height = height
        background.setSize(width, height)
        //background.setSize(width, height)
        background.radius = IRectCorners(this.radiusPoints())
        background.shadowRadius = if (elevation) 10.0 else 0.0
        //textView.setSize(width, height)

        textView.setSize(width, height)

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
        updatedUIButton(over = true)
	}

	fun simulateOut() {
        if (!bover) return
		bover = false
        updatedUIButton(over = false)
	}

	fun simulatePressing(value: Boolean) {
        if (bpressing == value) return
		bpressing = value
	}

	fun simulateDown(x: Double = 0.5, y: Double = 0.5) {
        if (bpressing) return
		bpressing = true
        updatedUIButton(down = true, px = x, py = y)
	}

	fun simulateUp() {
        if (!bpressing) return
		bpressing = false
        updatedUIButton(down = false)
	}

    val onPress = Signal<TouchEvents.Info>()

	init {
        singleTouch {
            start {
                //println("singleTouch.start")

                simulateDown(it.localX / width, it.localY / height)
                if (isFocusable) focused = true
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

    fun simulateClick(views: Views) {
        touch.simulateTapAt(views, localToGlobal(MPoint(width * 0.5, height * 0.5)))
    }

    open fun updatedUIButton(down: Boolean? = null, over: Boolean? = null, px: Double = 0.0, py: Double = 0.0, immediate: Boolean = false) {
        val button = this
        if (!button.enabled) {
            //button.animStateManager.set(
            //    AnimState(
            //        button::bgcolor[button.bgColorDisabled]
            //))
            //button.animatorEffects.cancel()
            background.bgColor = button.bgColorDisabled
            button.invalidateRender()
            return
        }
        //println("UPDATED: down=$down, over=$over, px=$px, py=$py")
        if (down == true) {
            //button.animStateManager.set(
            //    AnimState(
            //        button::highlightRadius[0.0, 1.0],
            //        button::highlightAlpha[1.0],
            //        button::highlightPos[Point(px / button.width, py / button.height), Point(px / button.width, py / button.height)],
            //    ))
            background.addHighlight(MPoint(px, py))
                /*
            button.highlightPos.setTo(px / button.width, py / button.height)
            button.animatorEffects.tween(
                button::highlightRadius[0.0, 1.0],
                button::highlightColor[Colors.WHITE.withAd(0.5), Colors.WHITE.withAd(0.5)],
                time = 0.5.seconds, easing = Easing.EASE_IN
            )
            */
        }
        if (down == false) {
            //button.animStateManager.set(
            //    AnimState(button::highlightAlpha[0.0])
            //)
            background.removeHighlights()
            //button.animatorEffects.tween(button::highlightColor[Colors.TRANSPARENT_BLACK], time = 0.2.seconds)
        }
        if (over != null) {
            val bgcolor = when {
                !button.enabled -> button.bgColorDisabled
                over -> button.bgColorOver
                selected -> button.bgColorSelected
                else -> button.bgColorOut
            }
            //button.animStateManager.set(
            //    AnimState(
            //        button::bgcolor[bgcolor]
            //    )
            //)
            if (immediate) {
                background.bgColor = bgcolor
            } else {
                button.animator.tween(background::bgColor[bgcolor], time = 0.25.seconds)
            }
        }
    }

    var selected: Boolean = false
        set(value) {
            field = value
            updatedUIButton(immediate = true, over = false)
        }

    override fun updateState() {
        super.updateState()
        updatedUIButton(immediate = true)
    }

    init {
        updatedUIButton(down = false, over = false, immediate = true)
    }

    var focusRatio: Double = 0.0; private set(value) { field = value; invalidateRender() }
    override fun focusChanged(value: Boolean) {
        //simpleAnimator.tween(this::focusRatio[value.toInt().toDouble()], time = 0.2.seconds)
        updatedUIButton(over = value)
    }

    init {
        keys {
            down(Key.SPACE, Key.RETURN) { if (this@UIButton.focused) simulateDown(0.5, 0.5) }
            up(Key.SPACE, Key.RETURN) {
                if (bpressing) {
                    simulateUp()
                    simulateClick(views)
                }
            }
        }
    }
}

fun <T : UIButton> T.clicked(block: (T) -> Unit): T {
    onClick { block(this) }
    return this
}
