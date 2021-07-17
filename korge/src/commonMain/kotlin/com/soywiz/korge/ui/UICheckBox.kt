package com.soywiz.korge.ui

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
    width: Double = 120.0,
    height: Double = 32.0,
    checked: Boolean = false,
    text: String = "CheckBox",
    block: @ViewDslMarker UICheckBox.() -> Unit = {}
): UICheckBox = UICheckBox(width, height, checked, text).addTo(this).apply(block)

open class UICheckBox(
    width: Double = 120.0,
    height: Double = 32.0,
    checked: Boolean = false,
    var text: String = "CheckBox",
) : UIView(width, height), ViewLeaf {
    val onChange = Signal<UICheckBox>()

    var checked: Boolean = checked
        get() = field
        set(value) {
            field = value
            onChange(this)
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
        box.ninePatch = when {
            this@UICheckBox.over -> buttonOver
            else -> buttonNormal
        }
        box.size(height, height)
        textBounds.setTo(0.0, 0.0, width - height - 8.0, height)

        fitIconInRect(icon, checkBoxIcon, box.width, box.height, Anchor.MIDDLE_CENTER)
        icon.visible = checked
        super.renderInternal(ctx)
    }

    init {
        mouse {
            onOver {
                this@UICheckBox.over = true
            }
            onOut {
                this@UICheckBox.over = false
            }
            onDown {
                this@UICheckBox.pressing = true
            }
            onUpAnywhere {
                this@UICheckBox.pressing = false
            }
            onClick {
                if (!it.views.editingMode) {
                    this@UICheckBox.checked = !this@UICheckBox.checked
                }
            }
        }
    }

    override fun buildDebugComponent(views: Views, container: UiContainer) {
        container.uiCollapsibleSection(UICheckBox::class.simpleName!!) {
            uiEditableValue(::text)
            uiEditableValue(::checked)
        }
        super.buildDebugComponent(views, container)
    }

    object Serializer : KTreeSerializerExt<UICheckBox>("UICheckBox", UICheckBox::class, { UICheckBox() }, {
        add(UICheckBox::text)
        add(UICheckBox::checked)
        add(UICheckBox::width)
        add(UICheckBox::height)
    })
}
