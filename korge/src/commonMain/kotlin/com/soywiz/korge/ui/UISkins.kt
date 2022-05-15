package com.soywiz.korge.ui

import com.soywiz.kds.*
import com.soywiz.kds.iterators.fastForEach
import com.soywiz.korge.scene.debugBmpFontSync
import com.soywiz.korge.view.RenderableView
import com.soywiz.korge.view.ViewRenderer
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.bitmap.BitmapSlice
import com.soywiz.korim.bitmap.NativeImage
import com.soywiz.korim.bitmap.NinePatchBmpSlice
import com.soywiz.korim.bitmap.asNinePatchSimple
import com.soywiz.korim.bitmap.context2d
import com.soywiz.korim.bitmap.mipmaps
import com.soywiz.korim.bitmap.sliceWithSize
import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RGBA
import com.soywiz.korim.color.mix
import com.soywiz.korim.font.DefaultTtfFont
import com.soywiz.korim.font.Font
import com.soywiz.korim.paint.*
import com.soywiz.korim.text.TextAlignment
import com.soywiz.korma.geom.IPoint
import com.soywiz.korma.geom.Point
import com.soywiz.korma.geom.degrees
import com.soywiz.korma.geom.vector.LineCap
import com.soywiz.korma.geom.vector.circle
import com.soywiz.korma.geom.vector.line
import com.soywiz.korma.geom.vector.lineTo
import com.soywiz.korma.geom.vector.moveTo
import com.soywiz.korma.geom.vector.roundRect
import kotlin.native.concurrent.ThreadLocal
import kotlin.reflect.KProperty

class BoxUISkin(
    var bgColor: RGBA = Colors["#c3c3c3"],
    var borderColor: RGBA = Colors["#1f1f1f"],
    var borderSize: Double = 1.0,
    var bgColorFocused: RGBA = Colors.WHITE,
    var borderColorFocused: RGBA = Colors.BLACK,
    var bgColorOver: RGBA = bgColor.mix(bgColorFocused, 0.5),
    var borderColorOver: RGBA = borderColor.mix(borderColorFocused, 0.5),
    val outlineColor: RGBA = Colors["#621a99"],
) : ViewRenderer {
    override fun RenderableView.render() {
        if (isFocused && outlineColor.ad > 0.0) {
            ctx2d.rectOutline(x - 1, y - 1, width + 2, height + 2, borderSize, outlineColor, false)
        }
        ctx2d.rect(0.0, 0.0, width, height, when {
            isFocused -> bgColorFocused
            isOver -> bgColorOver
            else -> bgColor
        }, false)
        ctx2d.rectOutline(x, y, width, height, borderSize, when {
            isFocused -> borderColorFocused
            isOver -> borderColorOver
            else -> borderColor
        }, false)
    }
}

interface UISkinable {
    fun <T> setSkinProperty(property: String, value: T)
    fun <T> getSkinPropertyOrNull(property: String): T?
}

fun <T> UISkinable.getSkinProperty(property: String, default: UISkinable.() -> T): T {
    val res = getSkinPropertyOrNull<T>(property)
    if (res == null) {
        val value = default()
        //println("Property[$property] doesn't exists in $this. Computed new value $value")
        setSkinProperty(property, value)
        return value
    }
    return res
}

open class UISkin(val name: String? = null, val skins: List<UISkinable> = listOf(), val parent: UISkinable? = null) : UISkinable {
    val skinProps = FastStringMap<Any?>()

    override fun <T> setSkinProperty(property: String, value: T) {
        skinProps[property] = value
    }

    override fun <T> getSkinPropertyOrNull(property: String): T? {
        skinProps[property]?.let { return it.fastCastTo() }
        skins.fastForEach { it.getSkinPropertyOrNull<T>(property)?.let { return it } }
        return parent?.getSkinPropertyOrNull(property)
    }

    fun copy(): UISkin = UISkin(name, skins, parent).also { it.skinProps.putAll(this.skinProps) }

    fun child() = UISkin(parent = this)

    override fun toString(): String = "UISkin($name)"
}

@Deprecated("This causes all properties, delegates and dependencies to be included in the JS output even if skins are not required")
open class UISkinableProperty<T>(val default: UISkinable.() -> T) {
    inline operator fun setValue(skin: UISkinable, property: KProperty<*>, value: T) = skin.setSkinProperty(property.name, value)
    inline operator fun getValue(skin: UISkinable, property: KProperty<*>): T = skin.getSkinProperty(property.name, default)
}

var UISkinable.uiSkinBitmap: Bitmap32 get() = getSkinProperty("uiSkinBitmap") { DEFAULT_UI_SKIN_IMG } ; set(value) { setSkinProperty("uiSkinBitmap", value) }
var UISkinable.textFont: Font get() = getSkinProperty("textFont") { DefaultUIFont } ; set(value) { setSkinProperty("textFont", value) }
var UISkinable.textSize: Double get() = getSkinProperty("textSize") { 16.0 } ; set(value) { setSkinProperty("textSize", value) }
var UISkinable.textColor: RGBA get() = getSkinProperty("textColor") { Colors.WHITE } ; set(value) { setSkinProperty("textColor", value) }
var UISkinable.textAlignment: TextAlignment get() = getSkinProperty("textAlignment") { TextAlignment.LEFT } ; set(value) { setSkinProperty("textAlignment", value) }
var UISkinable.shadowColor: RGBA get() = getSkinProperty("shadowColor") { Colors.BLACK.withAd(0.3) } ; set(value) { setSkinProperty("shadowColor", value) }
var UISkinable.shadowPosition: IPoint get() = getSkinProperty("shadowPosition") { Point(1, 1) } ; set(value) { setSkinProperty("shadowPosition", value) }

var UISkinable.buttonNormal: NinePatchBmpSlice get() = getSkinProperty("buttonNormal") { uiSkinBitmap.sliceWithSize(0, 0, 64, 64).asNinePatchSimple(16, 16, 48, 48) }; set(value) { setSkinProperty("buttonNormal", value) }
var UISkinable.buttonOver: NinePatchBmpSlice get() = getSkinProperty("buttonOver") { uiSkinBitmap.sliceWithSize(64, 0, 64, 64).asNinePatchSimple(16, 16, 48, 48) }; set(value) { setSkinProperty("buttonOver", value) }
var UISkinable.buttonDown: NinePatchBmpSlice get() = getSkinProperty("buttonDown") { uiSkinBitmap.sliceWithSize(128, 0, 64, 64).asNinePatchSimple(16, 16, 48, 48) }; set(value) { setSkinProperty("buttonDown", value) }
var UISkinable.buttonDisabled: NinePatchBmpSlice get() = getSkinProperty("buttonDisabled") { uiSkinBitmap.sliceWithSize(192, 0, 64, 64).asNinePatchSimple(16, 16, 48, 48) }; set(value) { setSkinProperty("buttonDisabled", value) }

var UISkinable.radioNormal: NinePatchBmpSlice get() = getSkinProperty("radioNormal") { uiSkinBitmap.sliceWithSize(256 + 0, 0, 64, 64).asNinePatchSimple(0, 0, 64, 64) }; set(value) { setSkinProperty("radioNormal", value) }
var UISkinable.radioOver: NinePatchBmpSlice get() = getSkinProperty("radioOver") { uiSkinBitmap.sliceWithSize(256 + 64, 0, 64, 64).asNinePatchSimple(0, 0, 64, 64) }; set(value) { setSkinProperty("radioOver", value) }
var UISkinable.radioDown: NinePatchBmpSlice get() = getSkinProperty("radioDown") { uiSkinBitmap.sliceWithSize(256 + 128, 0, 64, 64).asNinePatchSimple(0, 0, 64, 64) }; set(value) { setSkinProperty("radioDown", value) }
var UISkinable.radioDisabled: NinePatchBmpSlice get() = getSkinProperty("radioDisabled") { uiSkinBitmap.sliceWithSize(256 + 192, 0, 64, 64).asNinePatchSimple(0, 0, 64, 64) }; set(value) { setSkinProperty("radioDisabled", value) }

var UISkinable.buttonBackColor: RGBA get() = getSkinProperty("buttonBackColor") { Colors.DARKGREY }; set(value) { setSkinProperty("buttonBackColor", value) }
var UISkinable.buttonTextAlignment: TextAlignment get() = getSkinProperty("buttonTextAlignment") { TextAlignment.MIDDLE_CENTER }; set(value) { setSkinProperty("buttonTextAlignment", value) }

fun UISkinable.getUiIconFromSkinBitmap(index: Int, kind: Int = 0) = uiSkinBitmap.sliceWithSize(64 * kind, 64 * (index + 1), 64, 64)

var UISkinable.iconCheck: BitmapSlice<Bitmap32> get() = getSkinProperty("iconCheck") { getUiIconFromSkinBitmap(0) }; set(value) { setSkinProperty("iconCheck", value) }
var UISkinable.iconUp: BitmapSlice<Bitmap32> get() = getSkinProperty("iconUp") { getUiIconFromSkinBitmap(1) }; set(value) { setSkinProperty("iconUp", value) }
var UISkinable.iconRight: BitmapSlice<Bitmap32> get() = getSkinProperty("iconRight") { getUiIconFromSkinBitmap(2) }; set(value) { setSkinProperty("iconRight", value) }
var UISkinable.iconDown: BitmapSlice<Bitmap32> get() = getSkinProperty("iconDown") { getUiIconFromSkinBitmap(3) }; set(value) { setSkinProperty("iconDown", value) }
var UISkinable.iconLeft: BitmapSlice<Bitmap32> get() = getSkinProperty("iconLeft") { getUiIconFromSkinBitmap(4) }; set(value) { setSkinProperty("iconLeft", value) }

var UISkinable.comboBoxShrinkIcon: BitmapSlice<Bitmap32> get() = getSkinProperty("comboBoxShrinkIcon") { iconUp }; set(value) { setSkinProperty("comboBoxShrinkIcon", value) }
var UISkinable.comboBoxExpandIcon: BitmapSlice<Bitmap32> get() = getSkinProperty("comboBoxExpandIcon") { iconDown }; set(value) { setSkinProperty("comboBoxExpandIcon", value) }

var UISkinable.checkBoxIcon: BitmapSlice<Bitmap32> get() = getSkinProperty("checkBoxIcon") { iconCheck }; set(value) { setSkinProperty("checkBoxIcon", value) }

var UISkinable.scrollbarIconLeft: BitmapSlice<Bitmap32> get() = getSkinProperty("scrollbarIconLeft") { iconLeft }; set(value) { setSkinProperty("scrollbarIconLeft", value) }
var UISkinable.scrollbarIconRight: BitmapSlice<Bitmap32> get() = getSkinProperty("scrollbarIconRight") { iconRight }; set(value) { setSkinProperty("scrollbarIconRight", value) }
var UISkinable.scrollbarIconUp: BitmapSlice<Bitmap32> get() = getSkinProperty("scrollbarIconUp") { iconUp }; set(value) { setSkinProperty("scrollbarIconUp", value) }
var UISkinable.scrollbarIconDown: BitmapSlice<Bitmap32> get() = getSkinProperty("scrollbarIconDown") { iconDown }; set(value) { setSkinProperty("scrollbarIconDown", value) }

inline fun UISkin(name: String? = null, block: UISkin.() -> Unit): UISkin = UISkin(name).apply(block)

/*
@Deprecated("")
fun UISkin(
    normal: BmpSlice,
    over: BmpSlice = normal,
    down: BmpSlice = normal,
    disabled: BmpSlice = normal,
    backColor: RGBA = Colors.DARKGREY
): UISkin {
    fun createPatch(bmp: BmpSlice) = NinePatchBmpSlice.createSimple(
        bmp,
        (bmp.width * (10.0 / 64.0)).toInt(), (bmp.height * (10.0 / 64.0)).toInt(),
        (bmp.width * (54.0 / 64.0)).toInt(), (bmp.height * (54.0 / 64.0)).toInt()
    )

    return UISkin {
        buttonNormal = createPatch(normal)
        buttonOver = createPatch(over)
        buttonDown = createPatch(down)
        buttonDisabled = createPatch(disabled)
        buttonBackColor = backColor
    }
}
*/

//////////////////


val DefaultUIFont get() = DefaultUIVectorFont
val DefaultUIBitmapFont get() = debugBmpFontSync
val DefaultUIVectorFont get() = DefaultTtfFont

@ThreadLocal
private var DEFAULT_UI_SKIN_IMG_OR_NULL: Bitmap32? = null

val DEFAULT_UI_SKIN_IMG: Bitmap32 get() {
    if (DEFAULT_UI_SKIN_IMG_OR_NULL == null) {
        DEFAULT_UI_SKIN_IMG_OR_NULL = //Bitmap32(64 * 3, 64).context2d {
            NativeImage(512, 512).context2d {
                for (kind in listOf(UiSkinKind.BUTTON, UiSkinKind.RADIO)) {
                    for (n in 0 until 4) {
                        drawImage(buildDefaultButton(UiSkinType(n), kind), (kind.id * 256) + (n * 64), 0)
                    }
                }
                for (n in 0 until 5) {
                    for (enabled in listOf(false, true)) {
                        drawImage(buildDefaultShape(n, enabled), 64.0 * (if (enabled) 0 else 1), 64 + 64.0 * n)
                    }
                }
            }.mipmaps(true).toBMP32IfRequired().also { image ->
                //launchImmediately(EmptyCoroutineContext) { image.writeTo("/tmp/file.png".uniVfs, PNG) }
            }
    }
    return DEFAULT_UI_SKIN_IMG_OR_NULL!!
}

enum class UiSkinKind(val id: Int) {
    BUTTON(0), RADIO(1)
}

inline class UiSkinType(val index: Int) {
    companion object {
        val NORMAL = UiSkinType(0)
        val OVER = UiSkinType(1)
        val DOWN = UiSkinType(2)
        val DISABLED = UiSkinType(3)
    }
}

private fun buildDefaultButton(index: UiSkinType, kind: UiSkinKind): Bitmap {
    return NativeImage(64, 64).context2d {
        val gradient: Paint = when (index) {
            UiSkinType.NORMAL -> ColorPaint(ColorPaint(Colors.DIMGREY))//createLinearGradient(0, 0, 0, 64).addColorStop(0.0, Colors["#F9F9F9"]).addColorStop(1.0, Colors["#6C6C6C"]) // Out
            UiSkinType.OVER -> ColorPaint(ColorPaint(Colors["#6b6b6b"]))//createLinearGradient(0, 0, 0, 64).addColorStop(0.0, Colors["#F9F9F9"]).addColorStop(1.0, Colors["#9E9E9E"]) // Over
            UiSkinType.DOWN -> ColorPaint(ColorPaint(Colors["#4f4f4f"]))//createLinearGradient(0, 0, 0, 64).addColorStop(0.0, Colors["#909090"]).addColorStop(1.0, Colors["#F5F5F5"]) // Down
            UiSkinType.DISABLED -> ColorPaint(ColorPaint(Colors["#494949"]))//createLinearGradient(0, 0, 0, 64).addColorStop(0.0, Colors["#A7A7A7"]).addColorStop(1.0, Colors["#A7A7A7"]) // Disabled
            else -> TODO()
        }

        val border: Paint = when (index) {
            UiSkinType.NORMAL -> gradient
            UiSkinType.OVER -> ColorPaint(Colors["#b4b4b4"])
            UiSkinType.DOWN -> ColorPaint(Colors["#c8c8c8"])
            UiSkinType.DISABLED -> gradient
            else -> ColorPaint(Colors["#3c3e3e"])
        }
        fill(gradient) {
            stroke(border, lineWidth = 8.0) {
                when (kind) {
                    UiSkinKind.RADIO -> circle(32.0, 32.0, 28.0)
                    UiSkinKind.BUTTON -> roundRect(4, 4, 64 - 4 * 2, 64 - 4 * 2, 6, 6)
                }
            }
        }
    }
}

private fun buildDefaultShape(index: Int, enabled: Boolean): Bitmap {
    val color = com.soywiz.korim.paint.ColorPaint(if (enabled) Colors["#484848"] else Colors["#737373"])
    val lineWidth = 8.0
    return NativeImage(64, 64).context2d {
        when (index) {
            0 -> {
                translate(27, 41)
                rotate(45.degrees)
                stroke(color, lineWidth = lineWidth, lineCap = LineCap.BUTT) {
                    moveTo(-16, 0)
                    lineTo(0, 0)
                    lineTo(0, -32)
                }
            }
            else -> {
                translate(32, 32)
                rotate((45 + 90 * (index - 1)).degrees)
                val offsetX = -8
                val offsetY = -8
                val lineLength = 20
                stroke(color, lineWidth = lineWidth, lineCap = LineCap.SQUARE) {
                    line(offsetX + 0, offsetY + 0, offsetX + 0, offsetY + lineLength)
                    line(offsetX + 0, offsetY + 0, offsetX + lineLength, offsetY + 0)
                }
            }
        }
    }
}
