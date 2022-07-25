package com.soywiz.korge.ui

import com.soywiz.kds.*
import com.soywiz.klock.TimeSpan
import com.soywiz.korge.baseview.BaseView
import com.soywiz.korge.component.UpdateComponentWithViews
import com.soywiz.korge.input.keys
import com.soywiz.korge.render.RenderContext
import com.soywiz.korge.view.FixedSizeContainer
import com.soywiz.korge.view.Image
import com.soywiz.korge.view.View
import com.soywiz.korge.view.Views
import com.soywiz.korge.view.anchor
import com.soywiz.korge.view.position
import com.soywiz.korge.view.scale
import com.soywiz.korim.bitmap.BmpSlice
import com.soywiz.korma.geom.Anchor
import com.soywiz.korma.geom.Rectangle
import kotlin.math.min

private val View._defaultUiSkin: UISkin get() = extraCache("_defaultUiSkin") { UISkin("defaultUiSkin") {  } }

var View.uiSkin: UISkin?
    get() = getExtra("uiSkin") as? UISkin?
    set(value) { setExtra("uiSkin", value) }

var View.uiSkinSure: UISkin
    get() {
        if (!hasExtra("uiSkin")) setExtra("uiSkin", UISkin())
        return uiSkin!!
    }
    set(value) { uiSkin = value }

val View.realUiSkin: UISkin get() = uiSkin ?: parent?.realUiSkin ?: root._defaultUiSkin

open class UIView(
	width: Double = 90.0,
	height: Double = 32.0
) : FixedSizeContainer(width, height), UISkinable {
    override fun <T> setSkinProperty(property: String, value: T) {
        uiSkinSure.setSkinProperty(property, value)
    }
    override fun <T> getSkinPropertyOrNull(property: String): T? = (uiSkin?.getSkinPropertyOrNull(property) as? T?) ?: realUiSkin.getSkinPropertyOrNull(property)

	override var width: Double by uiObservable(width) { onSizeChanged() }
	override var height: Double by uiObservable(height) { onSizeChanged() }

    override fun getLocalBoundsInternal(out: Rectangle) {
        out.setTo(0.0, 0.0, width, height)
    }

    var enabled
		get() = mouseEnabled
		set(value) {
			mouseEnabled = value
			updateState()
		}

	fun enable(set: Boolean = true) {
		enabled = set
	}

	fun disable() {
		enabled = false
	}

	protected open fun onSizeChanged() {
	}

	open fun updateState() {
	}

	override fun renderInternal(ctx: RenderContext) {
		registerUISupportOnce()
		super.renderInternal(ctx)
	}

	private var registered = false
	private fun registerUISupportOnce() {
		if (registered) return
		val stage = stage ?: return
		registered = true
		if (stage.getExtra("uiSupport") == true) return
		stage.setExtra("uiSupport", true)
		stage.keys {
		}
		stage.getOrCreateComponentUpdateWithViews<DummyUpdateComponentWithViews> { stage ->
            DummyUpdateComponentWithViews(stage)
		}
	}

    companion object {
        fun fitIconInRect(iconView: Image, bmp: BmpSlice, width: Double, height: Double, anchor: Anchor) {
            val iconScaleX = width / bmp.width
            val iconScaleY = height / bmp.height
            val iconScale = min(iconScaleX, iconScaleY)

            iconView.bitmap = bmp
            iconView.anchor(anchor)
            iconView.position(width * anchor.sx, height * anchor.sy)
            iconView.scale(iconScale, iconScale)

        }
    }
}

internal class DummyUpdateComponentWithViews(override val view: BaseView) : UpdateComponentWithViews {
    override fun update(views: Views, dt: TimeSpan) {
    }
}
