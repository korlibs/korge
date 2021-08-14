package com.soywiz.korge.ui

import com.soywiz.kds.*
import com.soywiz.korge.debug.*
import com.soywiz.korge.input.*
import com.soywiz.korge.render.*
import com.soywiz.korge.view.*
import com.soywiz.korge.view.ktree.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.font.*
import com.soywiz.korim.text.*
import com.soywiz.korio.async.*
import com.soywiz.korma.geom.*
import com.soywiz.korui.*

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
    object Serializer : KTreeSerializerExt<UICheckBox>("UICheckBox", UICheckBox::class, { UICheckBox() }, {
        add(UICheckBox::text)
        add(UICheckBox::checked)
        add(UICheckBox::width)
        add(UICheckBox::height)
    })
}

open class UIBaseCheckBox<T>(
    width: Double = UI_DEFAULT_WIDTH,
    height: Double = UI_DEFAULT_HEIGHT,
    checked: Boolean = false,
    var text: String = "CheckBox",
) : UIView(width, height), ViewLeaf {
    val thisAsT get() = this.fastCastTo<T>()
    val onChange = Signal<T>()

    open var checked: Boolean = checked
        get() = field
        set(value) {
            field = value
            onChange(thisAsT)
        }

    private val background = solidRect(width, height, Colors.TRANSPARENT_BLACK)
    private val box = ninePatch(buttonNormal, height, height)
    private val icon = image(checkBoxIcon)
    private val textView = text(text)

    private var over by uiObservable(false) { updateState() }
    private var pressing by uiObservable(false) { updateState() }

    private val textBounds = Rectangle()

    override fun renderInternal(ctx: RenderContext) {
        textView.setFormat(
            face = textFont,
            size = textSize.toInt(),
            color = textColor,
            align = TextAlignment.MIDDLE_LEFT
        )
        textView.text = text
        textView.position(height + 8.0, 0.0)
        textView.setTextBounds(textBounds)

        background.size(width, height)
        box.ninePatch = getNinePatch(this@UIBaseCheckBox.over)
        box.size(height, height)
        textBounds.setTo(0.0, 0.0, width - height - 8.0, height)

        fitIconInRect(icon, checkBoxIcon, box.width, box.height, Anchor.MIDDLE_CENTER)
        icon.visible = checked
        super.renderInternal(ctx)
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

    override fun buildDebugComponent(views: Views, container: UiContainer) {
        container.uiCollapsibleSection(this::class.simpleName!!) {
            uiEditableValue(::text)
            uiEditableValue(::checked)
        }
        super.buildDebugComponent(views, container)
    }
}
