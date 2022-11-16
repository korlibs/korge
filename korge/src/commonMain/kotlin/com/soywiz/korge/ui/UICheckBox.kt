package com.soywiz.korge.ui

import com.soywiz.kds.*
import com.soywiz.korge.input.*
import com.soywiz.korge.render.*
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
) : UIBaseCheckBox<UICheckBox>(width, height, checked, text) {

}

open class UIBaseCheckBox<T : UIBaseCheckBox<T>>(
    width: Double = UI_DEFAULT_WIDTH,
    height: Double = UI_DEFAULT_HEIGHT,
    checked: Boolean = false,
    @ViewProperty
    var text: String = "CheckBox",
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
        }

    private val background = solidRect(width, height, Colors.TRANSPARENT_BLACK)
    val canvas = renderableView {
        ctx2d.materialRoundRect(
            0.0, 0.0, height, height, radius = RectCorners(height * 0.5),
        )
    }
    private val box = ninePatch(buttonNormal, height, height)
    private val icon = image(checkBoxIcon)
    private val textView = textBlock(RichTextData(text))

    private var over by uiObservable(false) { updateState() }
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
        box.ninePatch = getNinePatch(this@UIBaseCheckBox.over)
        box.size(height, height)

        fitIconInRect(icon, checkBoxIcon, box.width, box.height, Anchor.MIDDLE_CENTER)
        icon.visible = checked
    }

    open fun getNinePatch(over: Boolean): NinePatchBmpSlice {
        return when {
            over -> buttonOver
            else -> buttonNormal
        }
    }

    init {
        mouse {
            onOver { this@UIBaseCheckBox.over = true }
            onOut { this@UIBaseCheckBox.over = false }
            onDown { this@UIBaseCheckBox.pressing = true }
            onUpAnywhere { this@UIBaseCheckBox.pressing = false }
            onClick { if (!it.views.editingMode) this@UIBaseCheckBox.onComponentClick() }
        }
    }

    protected open fun onComponentClick() {
        this@UIBaseCheckBox.checked = !this@UIBaseCheckBox.checked
    }
}
