---
permalink: /imaging/vector/
group: imaging
layout: default
title: "Imaging Vector Rendering"
title_short: Vector Rendering
description: KorIM support vector rendering by the `Context2d` class. This class is meant to simulate the JS HTML Canvas API, but hyper-vitaminated. 
fa-icon: fa-bezier-curve
priority: 35
status: new
---

## Context2d

Context2d is a class that mimics the HTML Canvas API, it implements the VectorBuilder interface.
You can get a Context2d from a `NativeImage` or a `Bitmap32`.

> NOTE: Context2d with Bitmap32 has a performance and quality that might be subpar with native implementations, but it is consistent among targets

### Constructing

The typical usage is by calling the `Bitmap.context2d` method.

```kotlin
bitmap.context2d { // this: Context2d ->
	// ...
}
```

You can get an instance with `getContext2d`, but then you are on your own to dispose the instance after finishing.

```kotlin
val context2d = bitmap.getContext2d(antialiasing = true)
run {
	// ...
}
context2d.dispose() // We must dispose it once finished to release resources and write back the data 
```

### API

```kotlin

class Context2d : Disposable, VectorBuilder {
	val renderer: Renderer
	val defaultFontRegistry: FontRegistry? = null
	val defaultFont: Font? = null
	var debug: Boolean
	val width: Int
	val height: Int
	
	// Last moveTo registered position
	var moveX: Double
	var moveY: Double

	fun dispose()

	fun withScaledRenderer(scaleX: Double, scaleY: Double = scaleX): Context2d = if (scaleX == 1.0 && scaleY == 1.0) this else Context2d(ScaledRenderer(renderer, scaleX, scaleY))
	inline fun <T> buffering(callback: () -> T): T
}

class Context2d.ScaledRenderer(val parent: Renderer, val scaleX: Double, val scaleY: Double) : Renderer()
```

### Bounds

You can get the bounds of your vector graphics:

```kotlin
fun Context2d.getBounds(out: Rectangle = Rectangle()): Rectangle
```

### Vector building

Context2d implements the basic VectorBuilder interface, so you can call all the extension methods for building
different shapes.

```kotlin
// From VectorBuilder
override var lastX: Double
override var lastY: Double
override val totalPoints: Int
fun beginPath()
override fun close()
override fun moveTo(x: Double, y: Double)
override fun lineTo(x: Double, y: Double)
override fun quadTo(cx: Double, cy: Double, ax: Double, ay: Double)
override fun cubicTo(cx1: Double, cy1: Double, cx2: Double, cy2: Double, ax: Double, ay: Double)
```

### Drawing

You can also draw (put vectors) a GraphicsPath and a Drawable

```kotlin
fun Context2d.path(path: GraphicsPath)
fun Context2d.draw(d: Drawable)
```

### Direct filling / stroking

For some basic stuff, there are methods that directly draw things without having to call the `stroke` or `fill` methods.

```kotlin
fun Context2d.drawShape(
	shape: Drawable,
	rasterizerMethod: ShapeRasterizerMethod = ShapeRasterizerMethod.X4
)

fun Context2d.drawImage(image: Bitmap, x: Number, y: Number, width: Number = image.width, height: Number = image.height)
fun Context2d.strokeRect(x: Number, y: Number, width: Number, height: Number)
fun Context2d.fillRect(x: Number, y: Number, width: Number, height: Number)
fun Context2d.fillRoundRect(x: Number, y: Number, width: Number, height: Number, rx: Number, ry: Number = rx)
```

### Filling and stroking

Once you have your vectors added, you can stroke those paths or fill them.

#### Filling and stroking with the paints provided in the state

```kotlin
fun Context2d.stroke()
fun Context2d.fill()
fun Context2d.fillStroke()
```

#### Filling the current vector with a specific paint

```kotlin
fun Context2d.fill(paint: Paint)
fun Context2d.stroke(paint: Paint)
```

#### Block for filling, stroking or both

You can also call a block, that will render the vectors you draw inside the block.
This is usually the preferred way, since it is clearer.

```kotlin
fun Context2d.fill(paint: Paint, begin: Boolean = true, block: () -> Unit)
fun Context2d.stroke(paint: Paint, lineWidth: Double = this.lineWidth, lineCap: LineCap = this.lineCap, lineJoin: LineJoin = this.lineJoin, begin: Boolean = true, callback: () -> Unit)
fun Context2d.stroke(paint: Paint, info: StrokeInfo, begin: Boolean = true, callback: () -> Unit)
fun Context2d.fillStroke(fill: Paint, stroke: Paint, callback: () -> Unit)
```

### Clipping

```kotlin
fun Context2d.unclip()
fun Context2d.clip(path: VectorPath? = state.path, winding: Winding = Winding.NON_ZERO)
fun Context2d.clip(path: VectorPath?, winding: Winding = Winding.NON_ZERO, block: () -> Unit)
fun Context2d.clip(path: VectorPath.() -> Unit, winding: Winding = Winding.NON_ZERO, block: () -> Unit)
```

### Fills

```kotlin
fun Context2d.createLinearGradient(x0: Number, y0: Number, x1: Number, y1: Number, cycle: CycleMethod = CycleMethod.NO_CYCLE, transform: Matrix = Matrix(), block: GradientPaint.() -> Unit = {}): LinearGradientPaint
fun Context2d.createRadialGradient(x0: Number, y0: Number, r0: Number, x1: Number, y1: Number, r1: Number, cycle: CycleMethod = CycleMethod.NO_CYCLE, transform: Matrix = Matrix(), block: GradientPaint.() -> Unit = {}): RadialGradientPaint
fun Context2d.createSweepGradient(x0: Number, y0: Number, transform: Matrix = Matrix(), block: GradientPaint.() -> Unit = {}): SweepGradientPaint
fun Context2d.createColor(color: RGBA): RGBA
fun Context2d.createPattern(
	bitmap: Bitmap,
	cycleX: CycleMethod = CycleMethod.NO_CYCLE,
	cycleY: CycleMethod = cycleX,
	smooth: Boolean = true,
	transform: Matrix = Matrix()
): BitmapPaint
```

### Rendering State

```kotlin
var Context2d.lineScaleMode: LineScaleMode
var Context2d.lineWidth: Double
var Context2d.lineCap: LineCap
var Context2d.startLineCap: LineCap
var Context2d.endLineCap: LineCap
var Context2d.lineJoin: LineJoin
var Context2d.strokeStyle: Paint
var Context2d.fillStyle: Paint
var Context2d.fontRegistry: FontRegistry?
var Context2d.font: Font?
var Context2d.fontName: String?
var Context2d.fontSize: Double
var Context2d.verticalAlign: VerticalAlign
var Context2d.horizontalAlign: HorizontalAlign
var Context2d.alignment: TextAlignment
var Context2d.globalAlpha: Double
var Context2d.globalCompositeOperation: CompositeOperation
```

#### Temporarily set some states

Equivalent to calling `keep` and setting state properties manually.

```kotlin
fun Context2d.fillStyle(paint: Paint, callback: () -> Unit)
fun Context2d.strokeStyle(paint: Paint, callback: () -> Unit)
fun Context2d.font(
	font: Font? = this.font,
	halign: HorizontalAlign = this.horizontalAlign,
	valign: VerticalAlign = this.verticalAlign,
	fontSize: Double = this.fontSize,
	callback: () -> Unit
)
```

### Text

```kotlin
fun Context2d.fillText(text: String, x: Double, y: Double) = rendererRenderSystemText(state, font, fontSize, text, x, y, fill = true)
fun Context2d.strokeText(text: String, x: Double, y: Double) = rendererRenderSystemText(state, font, fontSize, text, x, y, fill = false)
fun Context2d.getTextBounds(text: String, out: TextMetrics = TextMetrics()): TextMetrics
fun Context2d.strokeText(text: String, x: Number, y: Number): Unit
fun Context2d.fillText(text: String, x: Number, y: Number): Unit
fun Context2d.fillText(
	text: String,
	x: Number,
	y: Number,
	font: Font? = this.font,
	fontSize: Double = this.fontSize,
	halign: HorizontalAlign = this.horizontalAlign,
	valign: VerticalAlign = this.verticalAlign,
	color: Paint? = null
): Unit
fun <T> Context2d.drawText(text: T, x: Double = 0.0, y: Double = 0.0, fill: Boolean = true, paint: Paint? = null, font: Font? = this.font, size: Double = this.fontSize, renderer: TextRenderer<T> = DefaultStringTextRenderer as TextRenderer<T>)
```

### Transformations

Context2d has an internal affine transformation, so all the paintings happening at some point
have that transformation applied.

#### Transforming 

```kotlin
fun Context2d.scale(sx: Number, sy: Number = sx, block: () -> Unit = {})
fun Context2d.translate(tx: Number, ty: Number, block: () -> Unit = {})
fun Context2d.rotate(angle: Angle, block: () -> Unit = {}) 
fun Context2d.rotateDeg(degs: Number)
fun Context2d.shear(sx: Double, sy: Double)

fun Context2d.transform(m: Matrix)
fun Context2d.setTransform(m: Matrix)

fun Context2d.transform(a: Double, b: Double, c: Double, d: Double, tx: Double, ty: Double)
fun Context2d.setTransform(a: Double, b: Double, c: Double, d: Double, tx: Double, ty: Double)
```

#### Keeping the transformations

```kotlin
fun Context2d.keepApply(callback: Context2d.() -> Unit)
fun Context2d.keep(callback: () -> Unit)
fun Context2d.keepTransform(callback: () -> Unit)

fun Context2d.save()
fun Context2d.restore()
```

You would typically call the `keep` block when transforming,
so code outside that block is not affected by the new transformations:

```kotlin
...context2d {
	keep {
		translate(10, 20)
		scale(2)
	}
	// Returns to the original transformation
}

```

## VectorBuilder (KorMA)

Since Context2d implements VectorBuilder, you can use these extension methods for simplifying drawing other shapes:

```kotlin
fun VectorBuilder.isEmpty(): Boolean
fun VectorBuilder.isNotEmpty(): Boolean

fun VectorBuilder.arcTo(ax: Double, ay: Double, cx: Double, cy: Double, r: Double)
fun VectorBuilder.rect(x: Double, y: Double, width: Double, height: Double)
fun VectorBuilder.rectHole(x: Double, y: Double, width: Double, height: Double)
fun VectorBuilder.roundRect(x: Double, y: Double, w: Double, h: Double, rx: Double, ry: Double = rx)
fun VectorBuilder.arc(x: Double, y: Double, r: Double, start: Double, end: Double)
fun VectorBuilder.circle(x: Double, y: Double, radius: Double)
fun VectorBuilder.ellipse(x: Double, y: Double, rw: Double, rh: Double)
fun VectorBuilder.moveTo(p: Point)
fun VectorBuilder.lineTo(p: Point)
fun VectorBuilder.quadTo(c: Point, a: Point)
fun VectorBuilder.cubicTo(c1: Point, c2: Point, a: Point)

fun VectorBuilder.moveTo(x: Number, y: Number)
fun VectorBuilder.lineTo(x: Number, y: Number)
fun VectorBuilder.quadTo(controlX: Number, controlY: Number, anchorX: Number, anchorY: Number)
fun VectorBuilder.cubicTo(cx1: Number, cy1: Number, cx2: Number, cy2: Number, ax: Number, ay: Number)

fun VectorBuilder.moveToH(x: Number)
fun VectorBuilder.rMoveToH(x: Number)

fun VectorBuilder.moveToV(y: Number)
fun VectorBuilder.rMoveToV(y: Number)

fun VectorBuilder.lineToH(x: Number)
fun VectorBuilder.rLineToH(x: Number)

fun VectorBuilder.lineToV(y: Number)
fun VectorBuilder.rLineToV(y: Number)

fun VectorBuilder.rMoveTo(x: Number, y: Number)
fun VectorBuilder.rLineTo(x: Number, y: Number)

fun VectorBuilder.rQuadTo(cx: Number, cy: Number, ax: Number, ay: Number)
fun VectorBuilder.rCubicTo(cx1: Number, cy1: Number, cx2: Number, cy2: Number, ax: Number, ay: Number)
fun VectorBuilder.arcTo(ax: Number, ay: Number, cx: Number, cy: Number, r: Number)
fun VectorBuilder.rect(x: Number, y: Number, width: Number, height: Number)
fun VectorBuilder.rectHole(x: Number, y: Number, width: Number, height: Number)
fun VectorBuilder.roundRect(x: Number, y: Number, w: Number, h: Number, rx: Number, ry: Number = rx)
fun VectorBuilder.arc(x: Number, y: Number, r: Number, start: Number, end: Number)
fun VectorBuilder.circle(x: Number, y: Number, radius: Number)
fun VectorBuilder.ellipse(x: Number, y: Number, rw: Number, rh: Number)

fun VectorBuilder.star(points: Int, radiusSmall: Double, radiusBig: Double, rotated: Angle = 0.degrees, x: Double = 0.0, y: Double = 0.0)
fun VectorBuilder.regularPolygon(points: Int, radius: Double, rotated: Angle = 0.degrees, x: Double = 0.0, y: Double = 0.0)
fun VectorBuilder.starHole(points: Int, radiusSmall: Double, radiusBig: Double, rotated: Angle = 0.degrees, x: Double = 0.0, y: Double = 0.0)
fun VectorBuilder.regularPolygonHole(points: Int, radius: Double, rotated: Angle = 0.degrees, x: Double = 0.0, y: Double = 0.0)
fun VectorBuilder.polygon(path: IPointArrayList, close: Boolean = true)
fun VectorBuilder.polygon(path: Array<IPoint>, close: Boolean = true)
fun VectorBuilder.polygon(path: List<IPoint>, close: Boolean = true
```

## GraphicsPath

GraphicsPath is a normal VectorPath that is also a `SizedDrawable`, so it can be renderer in a Context2d with the `draw(drawable: Drawable)` method.

## Paint

```kotlin
interface Paint {
    fun transformed(m: Matrix): Paint
}

val DefaultPaint: Paint get() = Colors.BLACK
```

### NonePaint

```kotlin
object NonePaint : Paint
```

### ColorPaint

ColorPaint is an alias of the RGBA class, that represents a single solid color with opacity.

### TransformedPaint

A Transformed Paint is a paint that can have an affine transformation Matrix. That's used for bitmap and gradient paints.

```kotlin
interface TransformedPaint : Paint { val transform: Matrix }
```

### GradientPaint

Gradient paints can be linear, radial and sweep (sweep only supported in some targets and Bitmap32).

```kotlin
enum class GradientKind { LINEAR, RADIAL, SWEEP }
enum class GradientUnits { USER_SPACE_ON_USE, OBJECT_BOUNDING_BOX }
enum class GradientInterpolationMethod { LINEAR, NORMAL }

data class GradientPaint(
    val kind: GradientKind,
    val x0: Double,
    val y0: Double,
    val r0: Double,
    val x1: Double,
    val y1: Double,
    val r1: Double,
    val stops: DoubleArrayList = DoubleArrayList(),
    val colors: IntArrayList = IntArrayList(),
    val cycle: CycleMethod = CycleMethod.NO_CYCLE,
    override val transform: Matrix = Matrix(),
    val interpolationMethod: GradientInterpolationMethod = GradientInterpolationMethod.NORMAL,
    val units: GradientUnits = GradientUnits.OBJECT_BOUNDING_BOX
) : TransformedPaint {
    fun x0(m: Matrix) = m.transformX(x0, y0)
    fun y0(m: Matrix) = m.transformY(x0, y0)
    fun r0(m: Matrix) = m.transformX(r0, r0)

    fun x1(m: Matrix) = m.transformX(x1, y1)
    fun y1(m: Matrix) = m.transformY(x1, y1)
    fun r1(m: Matrix) = m.transformX(r1, r1)

    val numberOfStops: Int

    companion object {
        fun identity(kind: GradientKind): GradientPaint
        fun gradientBoxMatrix(width: Double, height: Double, rotation: Angle, tx: Double, ty: Double, out: Matrix = Matrix()): Matrix
        fun fromGradientBox(kind: GradientKind, width: Double, height: Double, rotation: Angle, tx: Double, ty: Double): GradientPaint
    }

    fun addColorStop(stop: Number, color: RGBA): GradientPaint = add(stop.toDouble(), color)

    fun add(stop: Double, color: RGBA): GradientPaint
    val untransformedGradientMatrix: Matrix
    val gradientMatrix: Matrix
    val gradientMatrixInv = gradientMatrix.inverted()
    fun getRatioAt(x: Double, y: Double): Double
    fun getRatioAt(x: Double, y: Double, m: Matrix): Double
    fun applyMatrix(m: Matrix): GradientPaint
}
```

#### Constructing GradientPaint

```kotlin
fun LinearGradientPaint(x0: Number, y0: Number, x1: Number, y1: Number, cycle: CycleMethod = CycleMethod.NO_CYCLE, transform: Matrix = Matrix(), block: GradientPaint.() -> Unit = {}): GradientPaint
fun RadialGradientPaint(x0: Number, y0: Number, r0: Number, x1: Number, y1: Number, r1: Number, cycle: CycleMethod = CycleMethod.NO_CYCLE, transform: Matrix = Matrix(), block: GradientPaint.() -> Unit = {}): GradientPaint
fun SweepGradientPaint(x0: Number, y0: Number, transform: Matrix = Matrix(), block: GradientPaint.() -> Unit = {}): GradientPaint
```

### BitmapPaint

```kotlin
class BitmapPaint(
    val bitmap: Bitmap,
    override val transform: Matrix,
    val cycleX: CycleMethod = CycleMethod.NO_CYCLE,
    val cycleY: CycleMethod = CycleMethod.NO_CYCLE,
    val smooth: Boolean = true
) : TransformedPaint {
    val repeatX: Boolean get() = cycleX.repeating
    val repeatY: Boolean get() = cycleY.repeating
    val repeat: Boolean get() = repeatX || repeatY

    val bmp32 = bitmap.toBMP32()
}
```

## Drawable, SizedDrawable and BoundsDrawable

All these interfaces allows to draw to a Context2d, and each one provides more information about metrics and expected size.
That extra information is used by some code to generate bitmaps of the right size or determine the bounds of an object.

```kotlin
interface Drawable {
    fun draw(c: Context2d)
}

interface SizedDrawable : Drawable {
    val width: Int
    val height: Int
}

interface BoundsDrawable : SizedDrawable {
    val bounds: Rectangle
    val left: Int
    val top: Int
}
```

### Constructing a Drawable from a lambda

```kotlin
class FuncDrawable(val action: Context2d.() -> Unit) : Drawable {
    override fun draw(c: Context2d) {
        c.keep {
            action(c)
        }
    }
}
```

### Extra methods

```kotlin
fun <T : Bitmap> Drawable.draw(out: T): T {
    out.context2d {
        this@draw.draw(this)
    }
    return out
}
```

## ShapeBuilder

## Shape

A shape is a set of vector and styling information that can be rendered in a Context2d.  

### Shape

The basic interface:

```kotlin
interface Shape : BoundsDrawable {
	fun addBounds(bb: BoundsBuilder, includeStrokes: Boolean = false): Unit
	fun buildSvg(svg: SvgBuilder): Unit
    fun getPath(path: GraphicsPath = GraphicsPath()): GraphicsPath 
	fun containsPoint(x: Double, y: Double): Boolean
}

fun Shape.getBounds(out: Rectangle = Rectangle()): Rectangle
```

### EmptyShape

When you want to represent a shape that renders nothing, this is your object:

```kotlin
object EmptyShape : Shape
```

### StyledShape

The base interface for `FillShape`, `PolylineShape` and `TextShape`,
with information about the path, the paint, clipping, and transformation. 

```kotlin
interface StyledShape : Shape {
	val path: GraphicsPath? get() = null
	val clip: GraphicsPath?
	val paint: Paint
	val transform: Matrix
}
```

### FillShape

A shape that renders fillings a solid vector.

```kotlin
data class FillShape(
    override val path: GraphicsPath,
    override val clip: GraphicsPath?,
    override val paint: Paint,
    override val transform: Matrix = Matrix()
) : StyledShape
```

### PolylineShape

A shape that renders stroking the contour of a path.

```kotlin
data class PolylineShape(
    override val path: GraphicsPath,
    override val clip: GraphicsPath?,
    override val paint: Paint,
    override val transform: Matrix,
    val thickness: Double,
    val pixelHinting: Boolean,
    val scaleMode: LineScaleMode,
    val startCaps: LineCap,
    val endCaps: LineCap,
    val lineJoin: LineJoin,
    val miterLimit: Double
) : StyledShape
```

### TextShape

A shape that renders text.

```kotlin
class TextShape(
    val text: String,
    val x: Double,
    val y: Double,
    val font: Font?,
    val fontSize: Double,
    override val clip: GraphicsPath?,
    val fill: Paint?,
    val stroke: Paint?,
    val halign: HorizontalAlign = HorizontalAlign.LEFT,
    val valign: VerticalAlign = VerticalAlign.TOP,
    override val transform: Matrix = Matrix()
) : StyledShape
```

### CompoundShape

A shape that can hold/group several shapes at once.

```kotlin
class CompoundShape(
	val components: List<Shape>
) : Shape
```

### Converting a Drawable into a Shape

You can convert a Drawable and a SizedDrawable into a Shape: 

```kotlin
fun Drawable.toShape(width: Int, height: Int): Shape
fun SizedDrawable.toShape(): Shape
```

## SVG

`SVG` is interrelated with `Shape`. You can convert a Shape into an SVG string.

```kotlin
fun Shape.buildSvg(svg: SvgBuilder): Unit = Unit
fun Shape.toSvg(scale: Double = 1.0): Xml
fun Drawable.toSvg(width: Int, height: Int, scale: Double = 1.0): Xml
fun SizedDrawable.toSvg(scale: Double = 1.0): Xml = toSvg(width, height, scale)

class SvgBuilder(val bounds: Rectangle, val scale: Double) {
	val defs = arrayListOf<Xml>()
	val nodes = arrayListOf<Xml>()
	fun toXml(): Xml
}
```

## BitmapVector

A BitmapVector is a special Bitmap that is lazily created from a `BoundsDrawable` instance:

```kotlin
class BitmapVector : Bitmap {
    val shape: BoundsDrawable
    val bounds: Rectangle
    val scale: Double
    val rasterizerMethod: ShapeRasterizerMethod
    val antialiasing: Boolean
    val width: Int
    val height: Int
    val premultiplied: Boolean
    val left: Int
    val top: Int
    val nativeImage: Bitmap
}
```

## CycleMethod, CompositeOperation, CompositeMode, BlendMode, LineScaleMode, StrokeInfo

### CycleMethod
```kotlin
enum class CycleMethod {
    NO_CYCLE, REFLECT, REPEAT;

    val repeating: Boolean

    fun apply(ratio: Double, clamp: Boolean = false): Double
    fun apply(value: Double, size: Double): Double
    fun apply(value: Double, min: Double, max: Double): Double

    companion object {
        fun fromRepeat(repeat: Boolean): CycleMethod
    }
}
```

### CompositeOperation, CompositeMode, BlendMode

```kotlin
// https://drafts.fxtf.org/compositing-1/
interface CompositeOperation {
    companion object {
        val UNIMPLEMENTED: CompositeOperation
		
		// CompositeMode
		val DEFAULT: CompositeOperation
		val CLEAR: CompositeOperation
		val COPY: CompositeOperation
		val SOURCE_OVER: CompositeOperation
		val DESTINATION_OVER: CompositeOperation
		val SOURCE_IN: CompositeOperation
		val DESTINATION_IN: CompositeOperation
		val SOURCE_OUT: CompositeOperation
		val DESTINATION_OUT: CompositeOperation
		val SOURCE_ATOP: CompositeOperation
		val DESTINATION_ATOP: CompositeOperation
		val XOR: CompositeOperation
		val LIGHTER: CompositeOperation
		
		// BlendMode
		val NORMAL: CompositeOperation
		val MULTIPLY: CompositeOperation
		val SCREEN: CompositeOperation
		val OVERLAY: CompositeOperation
		val DARKEN: CompositeOperation
		val LIGHTEN: CompositeOperation
		val COLOR_DODGE: CompositeOperation
		val COLOR_BURN: CompositeOperation
		val HARD_LIGHT: CompositeOperation
		val SOFT_LIGHT: CompositeOperation
		val DIFFERENCE: CompositeOperation
		val EXCLUSION: CompositeOperation
		val HUE: CompositeOperation
		val SATURATION: CompositeOperation
		val COLOR: CompositeOperation
		val LUMINOSITY: CompositeOperation

        operator fun invoke(func: (dst: RgbaPremultipliedArray, dstN: Int, src: RgbaPremultipliedArray, srcN: Int, count: Int) -> Unit): CompositeOperation
    }

    fun blend(dst: RgbaPremultipliedArray, dstN: Int, src: RgbaPremultipliedArray, srcN: Int, count: Int)
}

// https://drafts.fxtf.org/compositing-1/
enum class CompositeMode(val op: CompositeOperation) : CompositeOperation by op {
    CLEAR,
    COPY,
    SOURCE_OVER,
    DESTINATION_OVER,
    SOURCE_IN,
    DESTINATION_IN,
    SOURCE_OUT,
    DESTINATION_OUT,
    SOURCE_ATOP,
    DESTINATION_ATOP,
    XOR,
    LIGHTER;
    
    companion object {
        val DEFAULT get() = SOURCE_OVER
    }
}

// https://drafts.fxtf.org/compositing-1/
enum class BlendMode(val op: CompositeOperation) : CompositeOperation by op {
    NORMAL,
    MULTIPLY,
    SCREEN,
    OVERLAY,
    DARKEN,
    LIGHTEN,
    COLOR_DODGE,
    COLOR_BURN,
    HARD_LIGHT,
    SOFT_LIGHT,
    DIFFERENCE,
    EXCLUSION,
    HUE,
    SATURATION,
    COLOR,
    LUMINOSITY,
    ADDITION,
    SUBTRACT,
    DIVIDE,
}
```

### LineScaleMode, StrokeInfo

```kotlin
enum class LineScaleMode {
    NONE, HORIZONTAL, VERTICAL, NORMAL;
    
    val hScale: Boolean
    val vScale: Boolean
}

class StrokeInfo {
    val thickness: Double
    val pixelHinting: Boolean
    val scaleMode: LineScaleMode
    val startCap: LineCap
    val endCap: LineCap
    val lineJoin: LineJoin
    val miterLimit: Double
}
```

## Chart

```kotlin
abstract class Chart() : Drawable {
	abstract fun Context2d.renderChart()
}
```

### ChartBars

```kotlin
open class ChartBars(val list: List<DataPoint>) : Chart() {
    companion object {
        operator fun invoke(vararg items: Pair<String, Number>): ChartBars
        fun fromPoints(vararg items: Pair<String, List<Number>>): ChartBars
    }

    data class DataPoint(val name: String, val values: List<Double>) {
        val localMaxValue = values.maxOrNull() ?: 0.0
    }

    val maxValue = list.map { it.localMaxValue }.maxOrNull() ?: 0.0
    val chartStep = 10.0.pow(floor(log10(maxValue))) / 2.0
    val rMaxValue = ceil(maxValue / chartStep) * chartStep
    val colors = listOf(Colors["#5485ec"], Colors.GREEN, Colors.BLUE, Colors.AZURE, Colors.CHARTREUSE, Colors.CADETBLUE)

    enum class Fit(val angle: Double) { FULL(0.0), DEG45(-45.0), DEG90(-90.0) }

    fun Context2d.renderBars(rect: Rectangle)
}
```
