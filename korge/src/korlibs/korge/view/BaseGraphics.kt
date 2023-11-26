package korlibs.korge.view

import korlibs.datastructure.iterators.*
import korlibs.image.bitmap.*
import korlibs.image.vector.*
import korlibs.korge.annotations.*
import korlibs.korge.render.*
import korlibs.math.*
import korlibs.math.geom.*
import kotlin.math.*

abstract class BaseGraphics(
    var autoScaling: Boolean = false,
) : BaseImage(Bitmaps.transparent) {
    var useNativeRendering = true
        set(value) {
            field = value
            dirty()
        }

    var preciseAutoScaling: Boolean = false
        set(value) {
            field = value
            if (value) autoScaling = true
        }

    var ignoreStrokesForBounds: Boolean = false
        set(value) {
            if (value) TODO()
            field = value
        }

    @PublishedApi
    internal val bitmapsToRemove = arrayListOf<Bitmap>()

    private var autoscaling = InternalViewAutoscaling()

    private val EXTRA_PIXELS = 1
    //private val EXTRA_PIXELS = 0

    @PublishedApi
    internal var _dirty = true

    @PublishedApi
    internal var _dirtyBounds = true

    @PublishedApi
    internal fun dirty() {
        _dirty = true
        _dirtyBounds = true
        invalidateLocalBounds()
    }

    override fun onAncestorChanged() {
        super.onAncestorChanged()
        if (parent == null || stage == null) {
            if (bitmap.base != Bitmaps.transparent.bmp) {
                //println("REMOVED FROM STAGE! $bitmap")
                bitmapsToRemove.add(bitmap.base)
            }
            clearBitmaps(onParentChanged = true)
        }
    }

    private fun clearBitmaps(onParentChanged: Boolean = false) {
        if (bitmapsToRemove.isEmpty()) return

        if (onParentChanged) {
            //println("clearBitmaps! onParentChanged=$onParentChanged : $bitmapsToRemove")
        }

        bitmapsToRemove.fastForEach {
            if (it != Bitmaps.transparent.bmp) {
                _ctx?.agBitmapTextureManager?.removeBitmap(it, "BaseGraphics")
            }
        }
        bitmapsToRemove.clear()
    }

    private var _ctx: RenderContext? = null

    @OptIn(KorgeExperimental::class)
    override fun renderInternal(ctx: RenderContext) {
        _ctx = ctx
        clearBitmaps(onParentChanged = false)

        if (redrawIfRequired()) {
            //ctx.coroutineContext.launchUnscoped { this.bitmap.bmpBase.writeTo(localVfs("/tmp/image.png"), PNG) }
        }
        super.renderInternal(ctx)
    }

    fun redrawIfRequired(): Boolean {
        if (autoscaling.onRender(autoScaling, preciseAutoScaling, globalMatrix)) {
            _dirty = true
        }

        if (!_dirty) return false

        _dirty = false

        val boundsWithShapes = boundsUnsafe(strokes = true)

        //println("boundsWithShapes=$boundsWithShapes")

        // Removes old image
        run {
            bitmapsToRemove.add(this.bitmap.base)
        }
        // Generates new image
        run {
            //println("Regenerate image: bounds=${bounds}, renderedAtScale=${renderedAtScaleX},${renderedAtScaleY}, sLeft=$sLeft, sTop=$sTop, bwidth=$bwidth, bheight=$bheight")

            //println("autoscaling.renderedAtScaleX=${autoscaling.renderedAtScaleX}")

            val imageWidth = (boundsWithShapes.width * autoscaling.renderedAtScaleX).toIntCeil().coerceAtLeast(1)
            val imageHeight = (boundsWithShapes.height * autoscaling.renderedAtScaleY).toIntCeil().coerceAtLeast(1)
            //val imageWidth = boundsWithShapes.width.toIntCeil()
            //val imageHeight = boundsWithShapes.height.toIntCeil()

            val image = NativeImageOrBitmap32(
                imageWidth + EXTRA_PIXELS,
                imageHeight + EXTRA_PIXELS,
                useNativeRendering, premultiplied = true
            )
            //println("bounds=$boundsWithShapes, scale=${autoscaling.renderedAtScaleX},${autoscaling.renderedAtScaleY}, image=$image")
            realImageScaleX = autoscaling.renderedAtScaleX
            realImageScaleY = autoscaling.renderedAtScaleY

            //realImageScaleX = 1.0
            //realImageScaleY = 1.0

            image.context2d {
                scale(realImageScaleX, realImageScaleY)
                translate(-boundsWithShapes.x, -boundsWithShapes.y)
                drawShape(this)
            }
            //println("image=${image.premultiplied}")
            this.bitmap = image.slice()
        }

        return true
    }

    var realImageScaleX = 1.0
    var realImageScaleY = 1.0

    protected abstract fun drawShape(ctx: Context2d) // this@BaseGraphics.compoundShape.draw(this)
    protected abstract fun getShapeBounds(includeStrokes: Boolean): Rectangle // shapes.fastForEach { it.addBounds(bb) }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private val renderBoundsStrokes = true
    //private val renderBoundsStrokes = false

    //val fillWidth get() = (bitmap.width - EXTRA_PIXELS).toDouble() / realImageScaleX
    //val fillHeight get() = (bitmap.height - EXTRA_PIXELS).toDouble() / realImageScaleY
    val fillWidth: Double get() = _getLocalBoundsInternal(strokes = renderBoundsStrokes).width
    val fillHeight: Double get() = _getLocalBoundsInternal(strokes = renderBoundsStrokes).height

    private val bitmapWidth: Float get() = bitmap.width.toFloat()
    private val bitmapHeight: Float get() = bitmap.height.toFloat()

    final override val bwidth: Float get() = (bitmapWidth / realImageScaleX).toFloat()
    final override val bheight: Float get() = (bitmapHeight / realImageScaleY).toFloat()
    final override val frameWidth: Float get() = bwidth
    final override val frameHeight: Float get() = bheight

    final override val anchorDispX: Float get() = (anchor.sx * bwidth).toFloat()
    final override val anchorDispY: Float get() = (anchor.sy * bheight).toFloat()
    override val sLeft: Float get() = _getLocalBoundsInternal(strokes = renderBoundsStrokes).x.toFloat()
    override val sTop: Float get() = _getLocalBoundsInternal(strokes = renderBoundsStrokes).y.toFloat()

    internal val _sLeft get() = sLeft
    internal val _sTop get() = sTop

    override fun getLocalBoundsInternal(): Rectangle = _getLocalBoundsInternal()

    //var boundsIncludeStrokes = false
    var boundsIncludeStrokes = true
        set(value) {
            field = value
            invalidateLocalBounds()
        }
    private fun _getLocalBoundsInternal(strokes: Boolean = this.boundsIncludeStrokes): Rectangle {
        val bounds = boundsUnsafe(strokes = strokes)
        return Rectangle(bounds.x - anchorDispX, bounds.y - anchorDispY, bounds.width, bounds.height)
    }

    private var _localBoundsWithStrokes = Rectangle()
    private var _localBounds = Rectangle()

    private fun boundsUnsafe(strokes: Boolean): Rectangle {
        if (_dirtyBounds) {
            _dirtyBounds = false
            _localBounds = getShapeBounds(includeStrokes = false)
            _localBoundsWithStrokes = getShapeBounds(includeStrokes = true)

            /*
            println("getLocalBoundsInternalNoAnchor: ")
            println(" - _localBounds=$_localBounds")
            println(" - _localBoundsWithStrokes=$_localBoundsWithStrokes")
             */
            //println("Graphics.BOUNDS: $out")
        }
        return if (strokes) _localBoundsWithStrokes else _localBounds
    }

    fun getLocalBoundsInternalNoAnchor(includeStrokes: Boolean): Rectangle = boundsUnsafe(includeStrokes)

    internal class InternalViewAutoscaling {
        var renderedAtScaleXInv = 1.0; private set
        var renderedAtScaleYInv = 1.0; private set
        var renderedAtScaleX = 1.0; private set
        var renderedAtScaleY = 1.0; private set
        var renderedAtScaleXY = 1.0; private set
        private var matrixTransform = MatrixTransform()

        fun onRender(autoScaling: Boolean, autoScalingPrecise: Boolean, globalMatrix: Matrix): Boolean {
            if (autoScaling) {
                matrixTransform = globalMatrix.toTransform()
                //val sx = kotlin.math.abs(matrixTransform.scaleX / this.scaleX)
                //val sy = kotlin.math.abs(matrixTransform.scaleY / this.scaleY)

                val sx = abs(matrixTransform.scaleX)
                val sy = abs(matrixTransform.scaleY)
                val sxy = max(sx, sy)

                val diffX = abs((sx / renderedAtScaleX) - 1.0)
                val diffY = abs((sy / renderedAtScaleY) - 1.0)

                val shouldUpdate = when (autoScalingPrecise) {
                    true -> (diffX > 0.0 || diffY > 0.0)
                    false -> diffX >= 0.1 || diffY >= 0.1
                }

                if (shouldUpdate) {
                    //println("diffX=$diffX, diffY=$diffY")

                    renderedAtScaleX = sx
                    renderedAtScaleY = sy
                    renderedAtScaleXY = sxy
                    renderedAtScaleXInv = 1.0 / sx
                    renderedAtScaleYInv = 1.0 / sy
                    //println("renderedAtScale: $renderedAtScaleX, $renderedAtScaleY")
                    return true
                }
            } else {
                renderedAtScaleX = 1.0
                renderedAtScaleY = 1.0
                renderedAtScaleXY = 1.0
                renderedAtScaleXInv = 1.0
                renderedAtScaleYInv = 1.0
            }
            return false
        }
    }
}
