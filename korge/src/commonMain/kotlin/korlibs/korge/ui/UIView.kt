package korlibs.korge.ui

import korlibs.datastructure.*
import korlibs.image.bitmap.*
import korlibs.korge.input.*
import korlibs.korge.render.*
import korlibs.korge.view.*
import korlibs.math.geom.*
import kotlin.math.*

open class UIView(
	width: Float = 90f,
	height: Float = 32f,
    cache: Boolean = false
) : FixedSizeCachedContainer(width, height, cache = cache) {
    private var _width: Float = width
    private var _height: Float = height

    override var width: Float
        get() = _width
        set(value) { if (_width != value) { _width = value; onSizeChanged() } }
    override var height: Float
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
        val (width, height) = size
        if (width == this._width && height == this._height) return
        _width = width
        _height = height
        onSizeChanged()
    }

    override fun getLocalBoundsInternal(): Rectangle = Rectangle(0.0, 0.0, widthD, heightD)

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
		stage.keys {}
        //stage.addUpdaterWithViews { views, dt ->
        //}
	}

    companion object {
        fun fitIconInRect(iconView: Image, bmp: BmpSlice, width: Double, height: Double, anchor: Anchor) {
            val iconScaleX = width / bmp.width
            val iconScaleY = height / bmp.height
            val iconScale = min(iconScaleX, iconScaleY)

            iconView.bitmap = bmp
            iconView.anchor(anchor)
            iconView.position(width * anchor.doubleX, height * anchor.doubleY)
            iconView.scale(iconScale, iconScale)
        }
    }
}

open class UIFocusableView(
    width: Float = 90f,
    height: Float = 32f,
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
