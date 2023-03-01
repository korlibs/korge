package com.soywiz.korge.ui

import com.soywiz.kds.*
import com.soywiz.klock.*
import com.soywiz.korge.baseview.*
import com.soywiz.korge.component.*
import com.soywiz.korge.input.*
import com.soywiz.korge.render.*
import com.soywiz.korge.view.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korma.geom.*
import kotlin.math.*

open class UIView(
	width: Double = 90.0,
	height: Double = 32.0,
    cache: Boolean = false
) : FixedSizeCachedContainer(width, height, cache = cache) {
    private var _width: Double = width
    private var _height: Double = height
	override var width: Double
        get() = _width
        set(value) { if (_width != value) { _width = value; onSizeChanged() } }
	override var height: Double
        get() = _height
        set(value) { if (_height != value) { _height = value; onSizeChanged() } }

    //var preferredWidth: Double = 100.0
    //var preferredHeight: Double = 100.0
    //var minWidth: Double = 100.0
    //var minHeight: Double = 100.0
    //var maxWidth: Double = 100.0
    //var maxHeight: Double = 100.0

    fun <T : View> (RenderContext2D.(T) -> Unit).render(ctx: RenderContext, view: T = this@UIView as T) {
        this@UIView.renderCtx2d(ctx) {
            this@render(it, view)
        }
    }

    override fun setSize(size: Size) {
        val width = size.widthD
        val height = size.heightD
        if (width == this._width && height == this._height) return
        _width = width
        _height = height
        onSizeChanged()
    }

    override fun getLocalBoundsInternal(out: MRectangle) {
        out.setTo(0.0, 0.0, width, height)
    }

    open var enabled: Boolean
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
        parent?.onChildChangedSize(this)
	}

    override fun onParentChanged() {
        updateState()
    }

    open fun updateState() {
        invalidate()
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

open class UIFocusableView(
    width: Double = 90.0,
    height: Double = 32.0,
    cache: Boolean = false
) : UIView(width, height, cache), UIFocusable {
    override val UIFocusManager.Scope.focusView: View get() = this@UIFocusableView
    override var tabIndex: Int = 0
    override var isFocusable: Boolean = true
    override fun focusChanged(value: Boolean) {
    }

    //init {
    //    keys {
    //        down(Key.UP, Key.DOWN) {
    //            if (focused) views.stage.uiFocusManager.changeFocusIndex(if (it.key == Key.UP) -1 else +1)
    //        }
    //    }
    //}
}

internal class DummyUpdateComponentWithViews(override val view: BaseView) : UpdateComponentWithViews {
    override fun update(views: Views, dt: TimeSpan) {
    }
}
