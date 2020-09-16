package com.soywiz.korge.ui

import com.soywiz.korge.view.*

inline fun Container.iconButton(
    width: Double = 128.0,
    height: Double = 64.0,
    skin: UISkin = defaultUISkin,
    iconSkin: IconSkin = defaultCheckSkin,
    block: @ViewDslMarker UIButton.() -> Unit = {}
): IconButton = IconButton(width, height, skin, iconSkin).addTo(this).apply(block)

open class IconButton(
    width: Double = 128.0,
    height: Double = 64.0,
    skin: UISkin = DefaultUISkin,
    iconSkin: IconSkin = DefaultCheckSkin
) : UIButton(width, height, skin) {

    var iconSkin: IconSkin by uiObservable(iconSkin) { updateState() }

    protected val icon = ninePatch(
        iconSkin.normal,
        iconSkin.calculateWidth(width),
        iconSkin.calculateHeight(height),
        0.0, 0.0, 0.0, 0.0
    ).also { it.position(iconSkin.paddingLeft(width), iconSkin.paddingTop(height)) }

    override fun updateState() {
        super.updateState()
        icon.tex = when {
            !enabled -> iconSkin.disabled
            bpressing || forcePressed -> iconSkin.down
            bover -> iconSkin.over
            else -> iconSkin.normal
        }
    }

    override fun onSizeChanged() {
        super.onSizeChanged()
        icon.width = iconSkin.calculateWidth(width)
        icon.height = iconSkin.calculateHeight(height)
        icon.position(iconSkin.paddingLeft(width), iconSkin.paddingTop(height))
        updateState()
    }
}
