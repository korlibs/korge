package com.soywiz.korge.ui

import com.soywiz.kds.fastArrayListOf
import com.soywiz.korge.debug.uiCollapsibleSection
import com.soywiz.korge.debug.uiEditableValue
import com.soywiz.korge.input.TouchEvents
import com.soywiz.korge.input.mouse
import com.soywiz.korge.input.onClick
import com.soywiz.korge.input.singleTouch
import com.soywiz.korge.render.RenderContext
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.NinePatchEx
import com.soywiz.korge.view.ViewDslMarker
import com.soywiz.korge.view.Views
import com.soywiz.korge.view.addTo
import com.soywiz.korge.view.image
import com.soywiz.korge.view.ninePatch
import com.soywiz.korge.view.position
import com.soywiz.korge.view.text
import com.soywiz.korim.bitmap.Bitmaps
import com.soywiz.korim.bitmap.BmpSlice
import com.soywiz.korim.font.Font
import com.soywiz.korio.async.Signal
import com.soywiz.korma.geom.Anchor
import com.soywiz.korma.geom.Rectangle
import com.soywiz.korui.UiContainer

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
    protected val rect: NinePatchEx = ninePatch(null, width, height)
    protected val textShadowView = text("", 16.0)
    protected val textView = text("", 16.0)
    protected val iconView = image(Bitmaps.transparent)
    private val textAndShadow = fastArrayListOf(textView, textShadowView)
	protected var bover = false
	protected var bpressing = false

    override fun renderInternal(ctx: RenderContext) {
        val skin = realUiSkin
        //println("UIButton: skin=$skin")
        rect.ninePatch = when {
            !enabled -> skin.buttonDisabled
            bpressing || forcePressed -> skin.buttonDown
            bover -> skin.buttonOver
            else -> skin.buttonNormal
        }
        rect.width = width
        rect.height = height

        fitIconInRect(iconView, icon ?: Bitmaps.transparent, rect.width, rect.height, Anchor.MIDDLE_CENTER)
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

            textAndShadow.fastForEach { textView ->
                textView.setTextBounds(Rectangle(0.0, 0.0, width, height))
                textView.visible = true
                textView.text = text
                textView.alignment = alignment
                textView.font = font
                textView.textSize = textSize
            }

            textView.color = skin.textColor
            textShadowView.color = skin.shadowColor
            textShadowView.position(skin.shadowPosition)
        } else {
            textView.visible = false
            textShadowView.visible = false
        }
        super.renderInternal(ctx)
    }

    fun simulateOver() {
		bover = true
	}

	fun simulateOut() {
		bover = false
	}

	fun simulatePressing(value: Boolean) {
		bpressing = value
	}

	fun simulateDown() {
		bpressing = true
	}

	fun simulateUp() {
		bpressing = false
	}

    val onPress = Signal<TouchEvents.Info>()

	init {
        singleTouch {
            start {
                //println("singleTouch.start")
                simulateDown()
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
