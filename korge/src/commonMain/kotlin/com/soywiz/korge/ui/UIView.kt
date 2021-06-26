package com.soywiz.korge.ui

import com.soywiz.kds.*
import com.soywiz.klock.TimeSpan
import com.soywiz.korge.baseview.*
import com.soywiz.korge.component.*
import com.soywiz.korge.input.*
import com.soywiz.korge.internal.*
import com.soywiz.korge.render.*
import com.soywiz.korge.view.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korma.geom.*
import kotlin.math.*
import kotlin.reflect.*

private val View._defaultUiSkin: UISkin by extraProperty { UISkin {  } }

var View.uiSkin: UISkin? by extraProperty { null }
val View.realUiSkin: UISkin get() = uiSkin ?: parent?.realUiSkin ?: root._defaultUiSkin

open class UIView(
	width: Double = 90.0,
	height: Double = 32.0
) : FixedSizeContainer(width, height), UISkinable {
    private val skinProps = LinkedHashMap<KProperty<*>, Any?>()
    override fun <T> setSkinProperty(property: KProperty<*>, value: T) { skinProps[property] = value }
    override fun <T> getSkinPropertyOrNull(property: KProperty<*>): T? = (skinProps[property] as? T?) ?: realUiSkin.getSkinPropertyOrNull(property)

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
