package korlibs.korge.ui

import korlibs.event.*
import korlibs.image.bitmap.*
import korlibs.image.color.*
import korlibs.image.font.*
import korlibs.image.text.*
import korlibs.io.async.*
import korlibs.korge.animate.*
import korlibs.korge.input.*
import korlibs.korge.tween.*
import korlibs.korge.view.*
import korlibs.korge.view.property.*
import korlibs.math.geom.*
import korlibs.math.interpolation.*
import korlibs.render.*
import korlibs.time.*
import kotlin.math.*

inline fun Container.uiButton(
    label: String = "",
    size: Size = UIButton.DEFAULT_SIZE,
    icon: BmpSlice? = null,
    block: @ViewDslMarker UIButton.() -> Unit = {}
): UIButton = UIButton(size, label, icon).addTo(this).apply(block)

open class UIToggleableButton(
    size: Size = UIButton.DEFAULT_SIZE,
    text: String = "",
    icon: BmpSlice? = null,
    richText: RichTextData? = null,
) : UIButton(size, text, icon, richText) {
    var pressed: Boolean = false
}

open class UIButton(
    size: Size = DEFAULT_SIZE,
    text: String = "",
    icon: BmpSlice? = null,
    richText: RichTextData? = null,
) : UIFocusableView(size) {
    companion object {
        val DEFAULT_SIZE = UI_DEFAULT_SIZE
    }

	var forcePressed = false

    private var _radiusRatio: Ratio = Ratio.NaN
    private var _radius: Double = 6.0

    private val halfSide: Int get() = min(width, height).toInt() / 2

    var radiusRatio: Ratio
        get() = if (!_radiusRatio.isNaN()) _radiusRatio else Ratio(_radius, halfSide.toDouble())
        set(value) {
            _radiusRatio = value
            _radius = Double.NaN
            setInitialState()
        }

    var radius: Double
        get() = if (!_radius.isNaN()) _radius else _radiusRatio.value * halfSide
        set(value) {
            _radius = value
            _radiusRatio = Ratio.NaN
            setInitialState()
        }
    //var radius = 100.percent

    //val radiusRatioHalf get() = radiusRatio * 0.5
    @ViewProperty
    var bgColorOut = MaterialColors.BLUE_700
        set(value) {
            field = value
            background.bgColor = value
        }
    @ViewProperty
    var bgColorOver = MaterialColors.BLUE_800
        set(value) {
            field = value
            updatedUIButton(immediate = true)
        }
    @ViewProperty
    var bgColorSelected = MaterialColors.BLUE_900
        set(value) {
            field = value
            updatedUIButton(immediate = true)
        }
    @ViewProperty
    var bgColorDisabled = Colors["#777777ff"]
        set(value) {
            field = value
            updatedUIButton(immediate = true)
        }
    @ViewProperty
    var elevation = true
        set(value) {
            field = value
            setInitialState()
        }

    override var enabled: Boolean
        get() = super.enabled
        set(value) {
            if (value == mouseEnabled) return

            mouseEnabled = value
            updatedUIButton(enable = value)
        }

    //protected val rect: NinePatchEx = ninePatch(null, width, height)
    //protected val background = roundRect(
    //    width, height, radiusWidth(width), radiusHeight(height), bgColorOut)
    //    .also { it.renderer = GraphicsRenderer.SYSTEM }
    //    //.filters(DropshadowFilter(0.0, 3.0, shadowColor = Colors.BLACK.withAd(0.126)))
    //    .also { it.mouseEnabled = false }

    //var newSkin: NewUIButtonSkin = DefaultUISkin

    val background = uiMaterialLayer(size) {
        radius = RectCorners(5f)
    }
    //internal val background = FastMaterialBackground(width, height).addTo(this)
    //    .also { it.colorMul = bgColorOut }
    //    .also { it.mouseEnabled = false }

    //protected val textShadowView = text("", 16.0)
    @ViewProperty(min = 1.0, max = 300.0)
    var textSize: Double
        get() = richText.defaultStyle.textSize
        set(value) {
            richText = richText.withStyle(richText.defaultStyle.copy(textSize = value))
        }

    val textView = textBlock(richText ?: RichTextData(text, font = DefaultTtfFontAsBitmap), align = TextAlignment.MIDDLE_CENTER)

    @ViewProperty
    @ViewPropertyProvider(TextAlignmentProvider::class)
    var textAlignment: TextAlignment by textView::align

    protected val iconView = image(Bitmaps.transparent)
	protected var bover = false
	protected var bpressing = false
    val animator = animator(parallel = true, defaultEasing = Easing.LINEAR)
    val animatorEffects = animator(parallel = true, defaultEasing = Easing.LINEAR)

    @ViewProperty()
    var richText: RichTextData by textView::text

    @ViewProperty()
    var text: String
        get() = textView.text.text
        set(value) {
            richText = richText.withText(value)
        }

    var textColor: RGBA
        get() = richText.defaultStyle.color ?: Colors.WHITE
        set(value) {
            if (textColor != value) {
                richText = richText.withStyle(style = richText.defaultStyle.copy(color = value))
            }
        }

    private fun setInitialState() {
        val width = this.width
        val height = this.height
        background.size(width, height)
        //background.setSize(width, height)
        background.radius = RectCorners(this.radius)
        background.shadowRadius = if (elevation) 10.0 else 0.0
        //textView.setSize(width, height)

        textView.size(width, height)

        fitIconInRect(iconView, icon ?: Bitmaps.transparent, width, height, Anchor.MIDDLE_CENTER)
        iconView.alphaF = when {
            !enabled -> 0.5f
            bover -> 1.0f
            else -> 1.0f
        }
        invalidateRender()
    }

    var icon: BmpSlice? = icon
        set(value) {
            field = value
            setInitialState()
        }

    override fun onSizeChanged() {
        setInitialState()
    }

    fun simulateOver() {
        if (bover) return
		bover = true
        updatedUIButton(over = true)
	}

	fun simulateOut() {
        if (!bover) return
		bover = false
        updatedUIButton(over = false)
	}

	fun simulatePressing(value: Boolean) {
        if (bpressing == value) return
		bpressing = value
	}

    fun simulateDown(x: Double = 0.5, y: Double = 0.5) = simulateDown(Point(x, y))
	fun simulateDown(p: Point) {
        if (bpressing) return
		bpressing = true
        updatedUIButton(down = true, pos = p)
	}

	fun simulateUp() {
        if (!bpressing) return
		bpressing = false
        updatedUIButton(down = false)
	}

    val onPress = Signal<TouchEvents.Info>()

	init {
        singleTouch {
            start {
                //println("singleTouch.start")

                simulateDown(it.localX / width, it.localY / height)
                if (isFocusable) focused = true
            }
            endAnywhere {
                //println("singleTouch.endAnywhere")
                simulateUp()
            }
            tap {
                //println("singleTouch.tap")
                onPress(it)
            }
        }
		mouse {
			onOver {
                //if (!it.lastEmulated) {
                run {
                    //println("mouse.onOver: ${input.mouse}, ${input.activeTouches}")
                    simulateOver()
                }
			}
			onOut {
                //if (!it.lastEmulated) {
                run {
                    //println("mouse.onOut")
                    simulateOut()
                }
			}
		}
        this.cursor = Cursor.HAND
        setInitialState()
    }

    fun simulateClick(views: Views) {
        touch.simulateTapAt(views, localToGlobal(Point(width * 0.5f, height * 0.5f)))
    }

    open fun updatedUIButton(down: Boolean? = null, over: Boolean? = null, enable: Boolean? = null, pos: Point = Point.ZERO, immediate: Boolean = false) {
        if (enabled && down != null) {
            if (down) {
                background.addHighlight(pos)
            } else {
                background.removeHighlights()
            }
        }

        val bgColor = when {
            enable == false || !enabled -> bgColorDisabled
            over == true -> bgColorOver
            selected -> bgColorSelected
            else -> bgColorOut
        }

        if (immediate) {
            background.bgColor = bgColor
        } else {
            animator.tween(background::bgColor[bgColor], time = 0.25.seconds)
        }
    }

    var selected: Boolean = false
        set(value) {
            field = value
            updatedUIButton(immediate = true, over = false)
        }

    override fun updateState() {
        super.updateState()
        updatedUIButton(immediate = true)
    }

    init {
        updatedUIButton(down = false, over = false, immediate = true)
    }

    var focusRatio: Double = 0.0; private set(value) { field = value; invalidateRender() }
    override fun focusChanged(value: Boolean) {
        //simpleAnimator.tween(this::focusRatio[value.toInt().toDouble()], time = 0.2.seconds)
        updatedUIButton(over = value)
    }

    init {
        keys {
            down(Key.SPACE, Key.RETURN) { if (this@UIButton.focused) simulateDown(0.5, 0.5) }
            up(Key.SPACE, Key.RETURN) {
                if (bpressing) {
                    simulateUp()
                    simulateClick(views)
                }
            }
        }
    }
}

fun <T : UIButton> T.clicked(block: (T) -> Unit): T {
    onClick { block(this) }
    return this
}
