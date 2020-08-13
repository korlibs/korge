package com.soywiz.korge.ui

import com.soywiz.korge.debug.*
import com.soywiz.korge.html.*
import com.soywiz.korge.input.*
import com.soywiz.korge.view.*
import com.soywiz.korge.view.ktree.*
import com.soywiz.korim.color.*
import com.soywiz.korio.async.*
import com.soywiz.korma.geom.*
import com.soywiz.korui.*

@Deprecated("Kotlin/Native boxes inline+Number")
inline fun Container.uiCheckBox(
    width: Number,
    height: Number,
    checked: Boolean = false,
    text: String = "CheckBox",
    textFont: Html.FontFace = defaultUIFont,
    skin: UISkin = defaultUISkin,
    checkIcon: IconSkin = defaultCheckSkin,
    block: @ViewDslMarker UICheckBox.() -> Unit = {}
): UICheckBox = uiCheckBox(width.toDouble(), height.toDouble(), checked, text, textFont, skin, checkIcon, block)

inline fun Container.uiCheckBox(
    width: Double = 120.0,
    height: Double = 32.0,
    checked: Boolean = false,
    text: String = "CheckBox",
    textFont: Html.FontFace = defaultUIFont,
    skin: UISkin = defaultUISkin,
    checkIcon: IconSkin = defaultCheckSkin,
    block: @ViewDslMarker UICheckBox.() -> Unit = {}
): UICheckBox = UICheckBox(width, height, checked, text, textFont, skin, checkIcon).addTo(this).apply(block)

open class UICheckBox(
    width: Double = 120.0,
    height: Double = 32.0,
    checked: Boolean = false,
    text: String = "CheckBox",
    textFont: Html.FontFace = DefaultUIFont,
    private val skin: UISkin = DefaultUISkin,
    private val checkIcon: IconSkin = DefaultCheckSkin
) : UIView(width, height), ViewLeaf {

    var checked by uiObservable(checked) { updateState() }
    var text by uiObservable(text) { updateText() }
    var textFont by uiObservable(textFont) { updateText() }
    var textSize by uiObservable(16) { updateText() }
    var textColor by uiObservable(Colors.WHITE) { updateText() }

    val onChange = Signal<UICheckBox>()

    private val background = solidRect(width, height, Colors.TRANSPARENT_BLACK)
    private val box = ninePatch(skin.normal, height, height, 10.0 / 64.0, 10.0 / 64.0, 54.0 / 64.0, 54.0 / 64.0)
    private val icon = ninePatch(
        checkIcon.normal,
        checkIcon.calculateWidth(height),
        checkIcon.calculateHeight(height),
        0.0, 0.0, 0.0, 0.0
    ).position(checkIcon.paddingLeft(height), checkIcon.paddingTop(height)).also { it.visible = false }
    private val textView = text(text)

    private var over by uiObservable(false) { updateState() }
    private var pressing by uiObservable(false) { updateState() }

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
        updateText()
    }

    override fun updateState() {
        super.updateState()
        box.tex = when {
            !enabled -> skin.disabled
            pressing -> skin.down
            over -> skin.over
            else -> skin.normal
        }
        icon.tex = when {
            !enabled -> checkIcon.disabled
            pressing -> checkIcon.down
            over -> checkIcon.over
            else -> checkIcon.normal
        }
        icon.visible = checked
        onChange(this)
    }

    private fun updateText() {
        textView.format = Html.Format(
            face = textFont,
            size = textSize,
            color = textColor,
            align = Html.Alignment.MIDDLE_LEFT
        )
        textView.setTextBounds(Rectangle(0, 0, width - height, height))
        textView.setText(text)
        textView.position(height + 8.0, 0)
    }

    override fun onSizeChanged() {
        super.onSizeChanged()
        background.size(width, height)
        box.size(height, height)
        icon.width = checkIcon.calculateWidth(height)
        icon.height = checkIcon.calculateHeight(height)
        textView.position(height + 8.0, 0)
        textView.setTextBounds(Rectangle(0, 0, width - height - 8.0, height))
    }

    override fun buildDebugComponent(views: Views, container: UiContainer) {
        container.uiCollapsableSection(UICheckBox::class.simpleName!!) {
            uiEditableValue(::text)
            uiEditableValue(::checked)
        }
        super.buildDebugComponent(views, container)
    }

    object Serializer : KTreeSerializerExt<UICheckBox>("UICheckBox", UICheckBox::class, { UICheckBox() }, {
        add(UICheckBox::text)
        add(UICheckBox::checked)
    })
}
