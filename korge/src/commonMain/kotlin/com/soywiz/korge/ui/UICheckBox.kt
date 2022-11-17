package com.soywiz.korge.ui

import com.soywiz.kds.*
import com.soywiz.klock.*
import com.soywiz.korge.animate.*
import com.soywiz.korge.input.*
import com.soywiz.korge.render.*
import com.soywiz.korge.tween.*
import com.soywiz.korge.view.*
import com.soywiz.korge.view.property.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.text.*
import com.soywiz.korio.async.*
import com.soywiz.korma.geom.*

inline fun Container.uiCheckBox(
    width: Double = UI_DEFAULT_WIDTH,
    height: Double = UI_DEFAULT_HEIGHT,
    checked: Boolean = false,
    text: String = "CheckBox",
    block: @ViewDslMarker UICheckBox.() -> Unit = {}
): UICheckBox = UICheckBox(width, height, checked, text).addTo(this).apply(block)

open class UICheckBox(
    width: Double = UI_DEFAULT_WIDTH,
    height: Double = UI_DEFAULT_HEIGHT,
    checked: Boolean = false,
    text: String = "CheckBox",
) : UIBaseCheckBox<UICheckBox>(width, height, checked, text, UIBaseCheckBoxSkin.Kind.CHECKBOX) {
}

open class UIBaseCheckBox<T : UIBaseCheckBox<T>>(
    width: Double = UI_DEFAULT_WIDTH,
    height: Double = UI_DEFAULT_HEIGHT,
    checked: Boolean = false,
    @ViewProperty
    var text: String = "CheckBox",
    var skinKind: UIBaseCheckBoxSkin.Kind,
) : UIView(width, height), ViewLeaf {
    val thisAsT get() = this.fastCastTo<T>()
    val onChange = Signal<T>()

    @ViewProperty
    open var checked: Boolean = checked
        get() = field
        set(value) {
            field = value
            onChange(thisAsT)
            invalidate()
            simpleAnimator.tween(this::checkedRatio[if (value) 1.0 else 0.0], time = 0.1.seconds)
        }

    var skin: UIBaseCheckBoxSkin = UIBaseCheckBoxSkinMaterial
    private val background = solidRect(width, height, Colors.TRANSPARENT_BLACK)
    val canvas = renderableView {
        skin.render(ctx2d, width, height, this@UIBaseCheckBox, skinKind)
    }
    private val textView = textBlock(RichTextData(text))
    var checkedRatio: Double = 0.0; private set
    var overRatio: Double = 0.0; private set

    private var over by uiObservable(false) {
        updateState()
        simpleAnimator.tween(this::overRatio[if (it) 1.0 else 0.0], time = 0.1.seconds)
    }
    private var pressing by uiObservable(false) { updateState() }

    private val textBounds = Rectangle()

    override fun renderInternal(ctx: RenderContext) {
        updateState()
        super.renderInternal(ctx)
    }

    override fun onSizeChanged() {
        super.onSizeChanged()
        updateState()
    }

    override fun updateState() {
        super.updateState()
        val width = this.width
        val height = this.height

        textView.text = RichTextData(textView.text.text, RichTextData.Style(
            font = textFont,
            textSize = textSize,
            color = textColor,
        ))
        textView.align = TextAlignment.MIDDLE_LEFT
        textView.position(height + 8.0, 0.0)
        textView.setSize(width - height - 8.0, height)

        background.size(width, height)
    }

    open fun getNinePatch(over: Boolean): NinePatchBmpSlice {
        return when {
            over -> buttonOver
            else -> buttonNormal
        }
    }

    val highlights = MaterialLayerHighlights(this)

    init {
        mouse {
            onOver { this@UIBaseCheckBox.over = true }
            onOut { this@UIBaseCheckBox.over = false }
            onDown {
                this@UIBaseCheckBox.pressing = true
                highlights.addHighlight(Point(0.5, 0.5))
            }
            onUpAnywhere {
                this@UIBaseCheckBox.pressing = false
                highlights.removeHighlights()
            }
            onClick {
                if (!it.views.editingMode) this@UIBaseCheckBox.onComponentClick()
            }
        }
    }

    protected open fun onComponentClick() {
        this@UIBaseCheckBox.checked = !this@UIBaseCheckBox.checked
    }
}
