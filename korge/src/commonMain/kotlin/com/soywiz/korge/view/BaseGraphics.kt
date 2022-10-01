package com.soywiz.korge.view

import com.soywiz.kds.iterators.fastForEach
import com.soywiz.kmem.toIntCeil
import com.soywiz.korge.annotations.KorgeExperimental
import com.soywiz.korge.render.RenderContext
import com.soywiz.korge.view.internal.InternalViewAutoscaling
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.Bitmaps
import com.soywiz.korim.bitmap.NativeImageOrBitmap32
import com.soywiz.korim.bitmap.context2d
import com.soywiz.korim.bitmap.slice
import com.soywiz.korim.vector.Context2d
import com.soywiz.korma.geom.BoundsBuilder
import com.soywiz.korma.geom.Rectangle

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
    }

    private val bb = BoundsBuilder()

    @OptIn(KorgeExperimental::class)
    override fun renderInternal(ctx: RenderContext) {
        bitmapsToRemove.fastForEach {
            if (it != Bitmaps.transparent.bmpBase) {
                ctx.agBitmapTextureManager.removeBitmap(it, "BaseGraphics")
            }
        }
        bitmapsToRemove.clear()

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
    protected abstract fun getShapeBounds(bb: BoundsBuilder, includeStrokes: Boolean) // shapes.fastForEach { it.addBounds(bb) }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private val renderBoundsStrokes = true
    //private val renderBoundsStrokes = false

    //val fillWidth get() = (bitmap.width - EXTRA_PIXELS).toDouble() / realImageScaleX
    //val fillHeight get() = (bitmap.height - EXTRA_PIXELS).toDouble() / realImageScaleY
    val fillWidth get() = _getLocalBoundsInternal(strokes = renderBoundsStrokes).width
    val fillHeight get() = _getLocalBoundsInternal(strokes = renderBoundsStrokes).height

    private val bitmapWidth: Double get() = bitmap.width.toDouble()
    private val bitmapHeight: Double get() = bitmap.height.toDouble()

    final override val bwidth: Double get() = bitmapWidth / realImageScaleX
    final override val bheight: Double get() = bitmapHeight / realImageScaleY
    final override val frameWidth: Double get() = bwidth
    final override val frameHeight: Double get() = bheight

    final override val anchorDispX: Double get() = (anchorX * bwidth)
    final override val anchorDispY: Double get() = (anchorY * bheight)
    override val sLeft: Double get() = _getLocalBoundsInternal(strokes = renderBoundsStrokes).x
    override val sTop: Double get() = _getLocalBoundsInternal(strokes = renderBoundsStrokes).y

    internal val _sLeft get() = sLeft
    internal val _sTop get() = sTop

    override fun getLocalBoundsInternal(out: Rectangle) {
        _getLocalBoundsInternal(out)
    }

    private val __localBounds: Rectangle = Rectangle()
    //var boundsIncludeStrokes = false
    var boundsIncludeStrokes = true
    private fun _getLocalBoundsInternal(out: Rectangle = __localBounds, strokes: Boolean = this.boundsIncludeStrokes): Rectangle {
        val bounds = boundsUnsafe(strokes = strokes)
        out.setTo(bounds.x - anchorDispX, bounds.y - anchorDispY, bounds.width, bounds.height)
        return out
    }

    private val _localBoundsWithStrokes = Rectangle()
    private val _localBounds = Rectangle()

    private fun boundsUnsafe(strokes: Boolean): Rectangle {
        if (_dirtyBounds) {
            _dirtyBounds = false
            bb.reset()
            getShapeBounds(bb, includeStrokes = false)
            bb.getBounds(_localBounds)

            bb.reset()
            getShapeBounds(bb, includeStrokes = true)
            bb.getBounds(_localBoundsWithStrokes)

            /*
            println("getLocalBoundsInternalNoAnchor: ")
            println(" - _localBounds=$_localBounds")
            println(" - _localBoundsWithStrokes=$_localBoundsWithStrokes")
             */
            //println("Graphics.BOUNDS: $out")
        }
        return if (strokes) _localBoundsWithStrokes else _localBounds
    }

    fun getLocalBoundsInternalNoAnchor(out: Rectangle, includeStrokes: Boolean) {
        out.copyFrom(boundsUnsafe(includeStrokes))
    }
}
