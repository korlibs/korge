package com.soywiz.korge.ui

import com.soywiz.korev.ISoftKeyboardConfig
import com.soywiz.korev.SoftKeyboardConfig
import com.soywiz.korge.annotations.KorgeExperimental
import com.soywiz.korge.text.TextEditController
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.ViewDslMarker
import com.soywiz.korge.view.ViewRenderer
import com.soywiz.korge.view.addTo
import com.soywiz.korge.view.bounds
import com.soywiz.korge.view.clipContainer
import com.soywiz.korge.view.renderableView
import com.soywiz.korge.view.text
import com.soywiz.korim.color.Colors
import com.soywiz.korim.font.Font
import com.soywiz.korio.async.Signal
import com.soywiz.korma.geom.Margin
import com.soywiz.korma.geom.Rectangle
import com.soywiz.korma.geom.without

@KorgeExperimental
inline fun Container.uiTextInput(
    initialText: String = "",
    width: Double = 128.0,
    height: Double = 24.0,
    skin: ViewRenderer = BoxUISkin(),
    block: @ViewDslMarker UITextInput.() -> Unit = {}
): UITextInput = UITextInput(initialText, width, height, skin)
    .addTo(this).also { block(it) }

/**
 * Simple Single Line Text Input
 */
@KorgeExperimental
class UITextInput(initialText: String = "", width: Double = 128.0, height: Double = 24.0, skin: ViewRenderer = BoxUISkin()) :
    UIView(width, height),
    //UIFocusable,
    ISoftKeyboardConfig by SoftKeyboardConfig() {

    //private val bg = ninePatch(NinePatchBmpSlice.createSimple(Bitmap32(3, 3) { x, y -> if (x == 1 && y == 1) Colors.WHITE else Colors.BLACK }.slice(), 1, 1, 2, 2), width, height).also { it.smoothing = false }
    private val bg = renderableView(width, height, skin)
    var skin by bg::viewRenderer
    private val container = clipContainer(0.0, 0.0)
    //private val container = fixedSizeContainer(width - 4.0, height - 4.0).position(2.0, 3.0)
    private val textView = container.text(initialText, 16.0, color = Colors.BLACK)
    val controller = TextEditController(textView, textView, this, bg)

    //init { uiScrollable {  } }

    var text: String by controller::text
    var textSize: Double by controller::textSize
    var font: Font by controller::font
    val onReturnPressed: Signal<TextEditController> by controller::onReturnPressed
    val onEscPressed: Signal<TextEditController> by controller::onEscPressed
    val onFocusLost: Signal<TextEditController> by controller::onFocusLost
    fun focus() = controller.focus()
    fun blur() = controller.blur()
    fun selectAll() = controller.selectAll()

    var padding: Margin = Margin(3.0, 2.0, 2.0, 2.0)
        set(value) {
            field = value
            onSizeChanged()
        }

    override fun onSizeChanged() {
        bg.setSize(width, height)
        container.bounds(Rectangle(0.0, 0.0, width, height).without(padding))
    }

    init {
        onSizeChanged()
    }

    //override val UIFocusManager.focusView: View get() = this@UITextInput
    //override var tabIndex: Int
    //    get() = TODO("Not yet implemented")
    //    set(value) {}
    //override var focused: Boolean
    //    get() = TODO("Not yet implemented")
    //    set(value) {}
}
