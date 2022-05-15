package com.soywiz.korge.view

import com.soywiz.kds.iterators.fastForEach
import com.soywiz.kmem.toIntCeil
import com.soywiz.korge.annotations.KorgeExperimental
import com.soywiz.korge.render.RenderContext
import com.soywiz.korge.view.internal.InternalViewAutoscaling
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.bitmap.Bitmaps
import com.soywiz.korim.bitmap.NativeImage
import com.soywiz.korim.bitmap.context2d
import com.soywiz.korim.bitmap.slice
import com.soywiz.korim.vector.Context2d
import com.soywiz.korma.geom.BoundsBuilder
import com.soywiz.korma.geom.Rectangle

abstract class BaseGraphics(
    var autoScaling: Boolean = false,
) : BaseImage(Bitmaps.transparent) {
    var useNativeRendering = true

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

    private fun createImage(width: Int, height: Int): Bitmap {
        return if (useNativeRendering) NativeImage(width, height) else Bitmap32(width, height, premultiplied = true)
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
                ctx.agBitmapTextureManager.removeBitmap(it)
            }
        }
        bitmapsToRemove.clear()

        if (redrawIfRequired()) {
            //ctx.coroutineContext.launchUnscoped { this.bitmap.bmpBase.writeTo(localVfs("/tmp/image.png"), PNG) }
        }
        super.renderInternal(ctx)
    }

    @PublishedApi
    internal fun redrawIfRequired(): Boolean {
        if (autoscaling.onRender(autoScaling, preciseAutoScaling, globalMatrix)) {
            _dirty = true
        }

        if (!_dirty) return false

        _dirty = false

        val boundsWithShapes = boundsUnsafe(strokes = true)

        // Removes old image
        run {
            bitmapsToRemove.add(this.bitmap.base)
        }
        // Generates new image
        run {
            //println("Regenerate image: bounds=${bounds}, renderedAtScale=${renderedAtScaleX},${renderedAtScaleY}, sLeft=$sLeft, sTop=$sTop, bwidth=$bwidth, bheight=$bheight")

            val imageWidth = (boundsWithShapes.width * autoscaling.renderedAtScaleX).toIntCeil().coerceAtLeast(1)
            val imageHeight = (boundsWithShapes.height * autoscaling.renderedAtScaleY).toIntCeil().coerceAtLeast(1)
            val image = createImage(
                imageWidth + EXTRA_PIXELS,
                imageHeight + EXTRA_PIXELS
            )
            //println("bounds=$boundsWithShapes, scale=${autoscaling.renderedAtScaleX},${autoscaling.renderedAtScaleY}, image=$image")
            realImageScaleX = imageWidth / boundsWithShapes.width
            realImageScaleY = imageHeight / boundsWithShapes.height

            image.context2d {
                scale(realImageScaleX, realImageScaleY)
                translate(-boundsWithShapes.x, -boundsWithShapes.y)
                drawShape(this)
                //this@BaseGraphics.compoundShape.draw(this)
            }
            this.bitmap = image.slice()
        }

        return true
    }

    var realImageScaleX = 1.0
    var realImageScaleY = 1.0

    protected abstract fun drawShape(ctx: Context2d) // this@BaseGraphics.compoundShape.draw(this)
    protected abstract fun getShapeBounds(bb: BoundsBuilder, includeStrokes: Boolean) // shapes.fastForEach { it.addBounds(bb) }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    //val fillWidth get() = (bitmap.width - EXTRA_PIXELS).toDouble() / realImageScaleX
    //val fillHeight get() = (bitmap.height - EXTRA_PIXELS).toDouble() / realImageScaleY
    val fillWidth get() = boundsUnsafe(strokes = false).width
    val fillHeight get() = boundsUnsafe(strokes = false).height

    //private val bitmapWidth get() = bitmap.width.toDouble() - EXTRA_PIXELS
    //private val bitmapHeight get() = bitmap.height.toDouble() - EXTRA_PIXELS

    final override val bwidth: Double get() = fillWidth
    final override val bheight: Double get() = fillHeight

    final override val frameWidth: Double get() = fillWidth
    final override val frameHeight: Double get() = fillHeight

    //final override val anchorDispX: Double get() = -boundsUnsafe(strokes = false).x + (anchorX * bwidth)
    //final override val anchorDispY: Double get() = -boundsUnsafe(strokes = false).y + (anchorY * bheight)
    //override val sLeft: Double get() = super.sLeft
    //override val sTop: Double get() = super.sTop

    final override val anchorDispX: Double get() = (anchorX * bwidth)
    final override val anchorDispY: Double get() = (anchorY * bheight)
    override val sLeft: Double get() = +boundsUnsafe(strokes = false).x - anchorDispX
    override val sTop: Double get() = +boundsUnsafe(strokes = false).y - anchorDispY

    final internal val _sLeft get() = sLeft
    final internal val _sTop get() = sTop

    override fun getLocalBoundsInternal(out: Rectangle) {
        out.setTo(sLeft, sTop, bwidth, bheight)
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
