---
permalink: /views/standard/
group: views
layout: default
title: Standard Views 
title_prefix: KorGE Views 
fa-icon: fa-object-ungroup 
priority: 6
---

Each kind of standard view provides a normal constructor, plus a DSL constructor that have `Container` as receiver.

## Container & FixedSizeContainer

Container is a View that can have children:

```kotlin
inline fun Container.container(callback: @ViewsDslMarker Container.() -> Unit = {})

open class Container : View() {
    // Gets the outer container. When attached, this should be the Stage instance.
    val containerRoot: Container get() = parent?.containerRoot ?: this

    // Methods to handle its children
    val children = arrayListOf<View>()
    fun addChildAt(view: View, index: Int)
    fun swapChildren(view1: View, view2: View)
    fun getChildIndex(view: View): Int = view.index
    fun getChildAt(index: Int): View = children[index]
    fun getChildByName(name: String): View? = children.firstOrNull { it.name == name }
    fun removeChild(view: View?)
    fun removeChildren()
    fun addChild(view: View)

    // addChild, removeChild shortcuts (+= and -=)
    operator fun plusAssign(view: View)
    operator fun minusAssign(view: View)
}
```

The Container `width` and `height` methods will depend on its children.

For container that has a fixed `width` and `height` properties, you can use the `FixedSizeContainer` class:

```kotlin
inline fun Container.fixedSizeContainer(
    width: Number,
    height: Number,
    callback: @ViewsDslMarker FixedSizeContainer.() -> Unit = {}
)

open class FixedSizeContainer(override var width: Double = 100.0, override var height: Double = 100.0) : Container()
```

**Related videos on this topic.**

- [Korge Tip 7: Container overview](https://youtu.be/o4wu_WfKNRU)
- [Korge Tip 10: Rendering views in specific layers using containers](https://youtu.be/y7whzUU-HCg)

## SolidRect

`SolidRect` is a View that is a rectangle of a solid color. In the end it acts like a 1x1 white image with a tint.

```kotlin
inline fun Container.solidRect(
    width: Double,
    height: Double,
    color: RGBA,
    callback: @ViewsDslMarker SolidRect.() -> Unit = {}
)

class SolidRect(width: Double, height: Double, color: RGBA) : View {
    var width: Double
    var height: Double
}
```

## RoundRect

`RoundRect` is a View that is a rectangle of a solid color with its borders rounded. It uses the `Graphics` class under
the hood.

```kotlin
inline fun Container.roundRect(
    width: Double,
    height: Double,
    rx: Double,
    ry: Double = rx,
    color: RGBA = Colors.WHITE,
    autoScaling: Boolean = true,
    callback: @ViewsDslMarker RoundRect.() -> Unit = {}
)

class RoundRect(
    var width: Double,
    var height: Double,
    var rx: Double,
    var ry: Double = rx,
    var color: RGBA = Colors.WHITE,
    var autoScaling: Boolean = true
) : View
```

## Circle

`Circle` is a View that is a circle of a solid color. It uses the `Graphics` class under the hood.

```kotlin
inline fun Container.circle(
    radius: Double = 16.0,
    color: RGBA = Colors.WHITE,
    autoScaling: Boolean = true,
    callback: Circle.() -> Unit = {}
): Circle

open class Circle(
    var radius: Double = 16.0,
    var color: RGBA = Colors.WHITE,
    var autoScaling: Boolean = true
) : View
```

## Ellipse

`Ellipse` is a View that is an ellipse of a solid color. It uses the `Graphics` class under the hood.

```kotlin
inline fun Container.ellipse(
    radiusX: Double = 16.0,
    radiusY: Double = 16.0,
    color: RGBA = Colors.WHITE,
    autoScaling: Boolean = true,
    callback: Ellipse.() -> Unit = {}
): Ellipse

open class Ellipse(
    var radiusX: Double = 16.0,
    var radiusY: Double = 16.0,
    var color: RGBA = Colors.WHITE,
    var autoScaling: Boolean = true
) : View
```

## Image

The Image view will display an image. In addition to containers, this is the most common view in 2d games.

It can be construced from `Bitmap` and `BmpSlice` from KorIM. Internally it creates, uploads and destroy textures in the
GPU automatically so you don't have to care about it.

```kotlin
inline fun Container.image(
    texture: BmpSlice,
    anchorX: Double = 0.0,
    anchorY: Double = 0.0,
    callback: @ViewsDslMarker Image.() -> Unit = {}
): Image
inline fun Container.image(
    texture: Bitmap,
    anchorX: Double = 0.0,
    anchorY: Double = 0.0,
    callback: @ViewsDslMarker Image.() -> Unit = {}
): Image

open class Image : View() {
    constructor(
        bitmap: BmpSlice,
        anchorX: Double = 0.0,
        anchorY: Double = anchorX,
        hitShape: VectorPath? = null,
        smoothing: Boolean = true
    )
    constructor(
        bitmap: Bitmap,
        anchorX: Double = 0.0,
        anchorY: Double = anchorX,
        hitShape: VectorPath? = null,
        smoothing: Boolean = true
    )

    var bitmap: BmpSlice get() = baseBitmap; set(v) = run { baseBitmap = v }
    var texture: BmpSlice get() = baseBitmap; set(v) = run { baseBitmap = v }
}
```

All the views extending `BaseRect` like `Image`, has a property called `smoothing`, to configure how sampling works.
When smoothing is true (its default value), the texture is sampled using linear interpolation, and when the smoothing is
false, it uses a nearest neighborhood sampling approach.

## SceneContainer

See the [Scenes page](/scenes/#SceneContainer) for more information.

## Graphics

The Graphics view allows to place vector graphics on it. The current implementations uses KorIM to rasterize the vector
shapes and generates an image out of it. KorIM uses the platform specific API to render vector graphics when available,
while defaulting to a Kotlin software rasterizer when no vector graphics API is available. It implements
the [`VectorBuilder`](/imaging/#VectorBuilder) interface from KorIM so it offers the standard vector drawing API.

```kotlin
inline fun Container.graphics(callback: Graphics.() -> Unit = {}): Graphics

class Graphics : View, VectorBuilder {
    inline fun dirty(callback: () -> Unit)
    fun clear()
    fun lineStyle(thickness: Double, color: RGBA, alpha: Double)
    inline fun fill(color: RGBA, alpha: Number = 1.0, callback: () -> Unit)
    inline fun fill(paint: Context2d.Paint, callback: () -> Unit)
    fun beginFill(paint: Context2d.Paint) = dirty
    fun beginFill(color: RGBA, alpha: Double)
    inline fun shape(shape: VectorPath)
    fun endFill()
}

```

## Camera

```kotlin
inline fun Container.camera(callback: @ViewsDslMarker Camera.() -> Unit)

class Camera : Container() {
    fun getLocalMatrixFittingGlobalRect(rect: Rectangle): Matrix
    fun getLocalMatrixFittingView(view: View?): Matrix
    fun setTo(view: View?)
    fun setTo(rect: Rectangle)

    suspend fun tweenTo(view: View?, vararg vs: V2<*>, time: TimeSpan, easing: Easing = Easing.LINEAR)
    suspend fun tweenTo(rect: Rectangle, vararg vs: V2<*>, time: TimeSpan, easing: Easing = Easing.LINEAR)
}
```

## CameraContainer
```
inline fun Container.cameraContainer(content: @ViewDslMarker Container.() -> Unit = {})
class CameraContainer(){
    fun follow(view: View?, setImmediately: Boolean = false)

    fun unfollow() 

    fun updateCamera(block: Camera.() -> Unit) 
    suspend fun tweenCamera(camera: Camera, time: TimeSpan = 1.seconds, easing: Easing = Easing.LINEAR)
}
```
## Mesh

Mesh allows to render a raw set of points as triangles or triangle strips. Used for example by the skeleton-based
animations with mesh deforms. 

```kotlin
open class Mesh(
    var texture: BmpSlice? = null,
    var vertices: Float32Buffer = Float32BufferAlloc(0),
    var uvs: Float32Buffer = Float32BufferAlloc(0),
    var indices: Uint16Buffer = Uint16BufferAlloc(0),
    var drawMode: DrawModes = DrawModes.Triangles
) : View() {
    enum class DrawModes { Triangles, TriangleStrip }

    val textureNN get() = texture ?: Bitmaps.white
    var dirty: Int = 0
    var indexDirty: Int = 0

    var pivotX: Double = 0.0
    var pivotY: Double = 0.0

    fun updatedVertices()
}

fun <T : Mesh> T.pivot(x: Double, y: Double): T = this.apply { this.pivotX = x }.also { this.pivotY = y }
```

## NinePatch

NinePatch is similar to an Image, but when stretching or shrinking, it preserves the size of its sides:

```kotlin
inline fun Container.ninePatch(
    tex: BmpSlice, width: Double, height: Double, left: Double, top: Double, right: Double, bottom: Double,
    callback: @ViewsDslMarker NinePatch.() -> Unit
)

class NinePatch(
    var tex: BmpSlice,
    override var width: Double,
    override var height: Double,
    var left: Double,
    var top: Double,
    var right: Double,
    var bottom: Double
) : View() {
    var smoothing = true
}
```

There is an extended version of the NinePatch, that uses the KorIM's `NinePatchBitmap32`, that is compatible with the
IntelliJ 9-patch bitmaps:

```kotlin
inline fun Container.ninePatch(
    tex: NinePatchEx.Tex, width: Double, height: Double, callback: @ViewsDslMarker NinePatchEx.() -> Unit
)

inline fun Container.ninePatch(
    ninePatch: NinePatchBitmap32, width: Double = ninePatch.dwidth, height: Double = ninePatch.dheight,
    callback: @ViewsDslMarker NinePatchEx.() -> Unit
)

class NinePatchEx : View() {
    var smoothing = true

    constructor(
        ninePatch: NinePatchBitmap32,
        width: Double = ninePatch.width.toDouble(),
        height: Double = ninePatch.height.toDouble()
    ) : NinePatchEx
}
```

## ScaleView

`ScaleView` is a FixedSizeContainer where all its contents is renderized to a normal size into a texture and then scaled
with or without filtering. This enables pixelated retro games.

```kotlin
inline fun Container.scaleView(
    width: Int, height: Int, scale: Double = 2.0, filtering: Boolean = false,
    callback: @ViewsDslMarker Container.() -> Unit = {}
) = ScaleView(width, height, scale, filtering).addTo(this).apply(callback)

class ScaleView(width: Int, height: Int, scale: Double = 2.0, var filtering: Boolean = false) : FixedSizeContainer(),
    View.Reference {
    init {
        this.width = width.toDouble()
        this.height = height.toDouble()
        this.scale = scale
    }
}
```

## Text

`Text` is a view that renders texts with a BitmapFont. It supports a small set of HTML for formating.

```kotlin
inline fun Container.text(
    text: String, textSize: Double = 16.0, font: BitmapFont = Fonts.defaultFont,
    callback: @ViewsDslMarker Text.() -> Unit = {}
)

class Text : View(), IText, IHtml {
    companion object {
        operator fun invoke(
            text: String,
            textSize: Double = 16.0,
            color: RGBA = Colors.WHITE,
            font: BitmapFont = Fonts.defaultFont
        ): Text
    }

    val textBounds = Rectangle(0, 0, 1024, 1024)
    var document: Html.Document? = null
    var filtering = true
    var bgcolor = Colors.TRANSPARENT_BLACK
    val fonts = Fonts.fonts

    fun setTextBounds(rect: Rectangle)
    fun unsetTextBounds()
    var format: Html.Format
    var text: String
    var html: String
    fun relayout()
}

interface IText {
    var text: String
}
interface IHtml {
    var html: String
}

fun View?.setText(text: String) = run { this.foreachDescendant { if (it is IText) it.text = text } }
fun View?.setHtml(html: String) = run { this.foreachDescendant { if (it is IHtml) it.html = html } }
```

## Alignment / Centering

Korge has a variety of extension functions for aligning views to other views. They're in the form
of `View.align___To___Of`, for example:

```kotlin
fun <T : View> T.alignLeftToLeftOf(other: View, padding: Double = 0.0): T =
    alignX(other, 0.0, inside = true, padding = padding)
fun <T : View> T.alignLeftToRightOf(other: View, padding: Double = 0.0): T =
    alignX(other, 1.0, inside = false, padding = padding)
fun <T : View> T.alignRightToLeftOf(other: View, padding: Double = 0.0): T =
    alignX(other, 0.0, inside = false, padding = padding)
```

_Tip: Use the auto complete from your IDE to discover more of these functions._

For example, for the function `alignLeftToLeftOf(other)`, this means to align the left side of `this` current view to
the left side of the `other` view.

There's also a variety of extension functions for centering a view on another view as well:

```kotlin
fun <T : View> T.centerBetween(x1: Double, y1: Double, x2: Double, y2: Double): T = this.centerXBetween(x1, x2).centerYBetween(y1, y2)
fun <T : View> T.centerOn(other: View): T = this.centerXOn(other).centerYOn(other)
fun <T : View> T.centerXOn(other: View): T = this.alignX(other, 0.5, true)
fun <T : View> T.centerYOn(other: View): T = this.alignY(other, 0.5, true)
```

[Here's a video explaining these functions can be used.](https://youtu.be/rdcdYirCuHo)
