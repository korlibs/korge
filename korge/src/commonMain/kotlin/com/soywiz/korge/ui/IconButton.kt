package com.soywiz.korge.ui

import com.soywiz.korge.view.*

inline fun Container.iconButton(
    width: Number = 128,
    height: Number = 64,
    skin: UISkin = defaultUISkin,
    iconSkin: IconSkin = DefaultCheckSkin,
    block: @ViewsDslMarker UIButton.() -> Unit = {}
): IconButton = IconButton(width.toDouble(), height.toDouble(), skin, iconSkin).addTo(this).apply(block)

open class IconButton(
    width: Double = 128.0,
    height: Double = 64.0,
    skin: UISkin = DefaultUISkin,
    iconSkin: IconSkin = DefaultCheckSkin
) : UIButton(width, height, skin) {

    var iconSkin: IconSkin by uiObservable(iconSkin) { updateState() }
    protected val icon = ninePatch(
        iconSkin.normal,
        width - iconSkin.paddingLeft - iconSkin.paddingRight,
        height - iconSkin.paddingTop - iconSkin.paddingBottom,
        0.0, 0.0, 0.0, 0.0
    ).also { it.position(iconSkin.paddingLeft, iconSkin.paddingTop) }

    override fun updateState() {
        super.updateState()
        when {
            !enabled -> {
                icon.tex = iconSkin.disabled
            }
            bpressing || forcePressed -> {
                icon.tex = iconSkin.down
            }
            bover -> {
                icon.tex = iconSkin.over
            }
            else -> {
                icon.tex = iconSkin.normal
            }
        }
    }

    override fun onSizeChanged() {
        super.onSizeChanged()
        icon.width = width - iconSkin.paddingLeft - iconSkin.paddingRight
        icon.height = height - iconSkin.paddingTop - iconSkin.paddingBottom
        updateState()
    }
}
