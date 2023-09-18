package korlibs.korge.ui

import korlibs.datastructure.*
import korlibs.event.*
import korlibs.image.color.*
import korlibs.image.text.*
import korlibs.io.async.*
import korlibs.korge.animate.*
import korlibs.korge.input.*
import korlibs.korge.render.*
import korlibs.korge.style.*
import korlibs.korge.tween.*
import korlibs.korge.view.*
import korlibs.korge.view.property.*
import korlibs.math.*
import korlibs.math.geom.*
import korlibs.time.*

inline fun Container.uiCheckBox(
    size: Size = UI_DEFAULT_SIZE,
    checked: Boolean = false,
    text: String = "CheckBox",
    block: @ViewDslMarker UICheckBox.() -> Unit = {}
): UICheckBox = UICheckBox(size, checked, text).addTo(this).apply(block)

open class UICheckBox(
    size: Size = UI_DEFAULT_SIZE,
    checked: Boolean = false,
    text: String = "CheckBox",
) : UIBaseCheckBox<UICheckBox>(size, checked, text, UICheckBox) {
    companion object : Kind()
}

open class UIBaseCheckBox<T : UIBaseCheckBox<T>>(
    size: Size = UI_DEFAULT_SIZE,
    checked: Boolean = false,
    @ViewProperty
    var text: String = "CheckBox",
    var kind: Kind,
) : UIFocusableView(size), ViewLeaf {
    open class Kind

    val thisAsT: T get() = this.fastCastTo<T>()
    val onChange: Signal<T> = Signal<T>()

    @ViewProperty
    open var checked: Boolean = checked
        get() = field
        set(value) {
            field = value
            onChange(thisAsT)
            invalidate()
            simpleAnimator.tween(this::checkedRatio[if (value) 1.0 else 0.0], time = 0.1.seconds)
        }

    private val background = solidRect(size, Colors.TRANSPARENT)
    val canvas = renderableView {
        styles.uiCheckboxButtonRenderer.render(ctx)
        //skin.render(ctx2d, this@UIBaseCheckBox.width, this@UIBaseCheckBox.height, this@UIBaseCheckBox, kind)
    }
    private val textView = textBlock(RichTextData(text))
    var checkedRatio: Double = if (checked) 1.0 else 0.0; private set
    var overRatio: Double = 0.0; private set

    private var over by uiObservable(false) {
        updateState()
        simpleAnimator.tween(this::overRatio[if (it) 1.0 else 0.0], time = 0.1.seconds)
    }
    private var pressing by uiObservable(false) { updateState() }

    private var textBounds = Rectangle()

    override fun renderInternal(ctx: RenderContext) {
        updateState()
        super.renderInternal(ctx)
    }

    override fun onSizeChanged() {
        super.onSizeChanged()
        updateState()
    }

    override fun updateState() {
        super.updateState()
        val width = this.widthD
        val height = this.heightD

        textView.text = RichTextData(textView.text.text, RichTextData.Style(
            font = styles.textFont,
            textSize = styles.textSize,
            color = styles.textColor,
        ))
        textView.align = TextAlignment.MIDDLE_LEFT
        textView.position(height + 4.0, 0.0)
        textView.size(width - height - 8.0, height)

        background.size(width, height)
    }

    val highlights = MaterialLayerHighlights(this)

    init {
        mouse {
            onOver { this@UIBaseCheckBox.over = true }
            onOut { this@UIBaseCheckBox.over = false }
            onDown {
                this@UIBaseCheckBox.pressing = true
                highlights.addHighlight(Point(0.5, 0.5))
            }
            onUpAnywhere {
                this@UIBaseCheckBox.pressing = false
                highlights.removeHighlights()
            }
            onClick {
                if (!it.views.editingMode) this@UIBaseCheckBox.onComponentClick()
            }
        }
    }

    protected open fun onComponentClick() {
        this@UIBaseCheckBox.checked = !this@UIBaseCheckBox.checked
        focused = true
    }

    var focusRatio: Double = 0.0; private set
    override fun focusChanged(value: Boolean) {
        //println("focusChanged=$value")
        simpleAnimator.tween(this::focusRatio[value.toInt().toDouble()], time = 0.2.seconds)
    }

    init {
        keys {
            down(Key.SPACE, Key.RETURN) { if (this@UIBaseCheckBox.focused) onComponentClick() }
        }
    }
}
