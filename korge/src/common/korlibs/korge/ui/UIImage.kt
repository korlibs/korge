package korlibs.korge.ui

import korlibs.image.bitmap.*
import korlibs.image.color.*
import korlibs.korge.render.*
import korlibs.korge.view.*
import korlibs.korge.view.property.*
import korlibs.math.*
import korlibs.math.geom.*

inline fun Container.uiImage(
    size: Size = UI_DEFAULT_SIZE,
    bitmap: BmpSlice = Bitmaps.transparent,
    scaleMode: ScaleMode = ScaleMode.NO_SCALE,
    contentAnchor: Anchor = Anchor.TOP_LEFT,
    block: @ViewDslMarker UIImage.() -> Unit = {}
): UIImage = UIImage(size, bitmap, scaleMode, contentAnchor).addTo(this).apply(block)

class UIImage(
    size: Size,
    bitmap: BmpSlice,
    scaleMode: ScaleMode = ScaleMode.NO_SCALE,
    contentAnchor: Anchor = Anchor.TOP_LEFT,
) : UIView(size) {
    private var cachedGlobalMatrix = Matrix.IDENTITY
    private var validCoords: Boolean = false

    @ViewProperty
    var bgcolor: RGBA = Colors.TRANSPARENT

    @ViewProperty
    var smoothing: Boolean = true

    @ViewProperty
    var bitmap: BmpSlice = bitmap
        set(value) {
            if (field !== value) {
                field = value; validCoords = false
            }
        }

    @ViewProperty
    @ViewPropertyProvider(ScaleModeProvider::class)
    var scaleMode: ScaleMode = scaleMode
        set(value) {
            if (field !== value) {
                field = value; validCoords = false
            }
        }

    @ViewProperty
    var contentAnchor: Anchor = contentAnchor
        set(value) {
            if (field != value) {
                field = value; validCoords = false
            }
        }

    override fun onSizeChanged() {
        validCoords = false
    }

    private val vertices = TexturedVertexArray(4, TexturedVertexArray.QUAD_INDICES)

    override fun renderInternal(ctx: RenderContext) {
        if (!validCoords || cachedGlobalMatrix != globalMatrix) {
            validCoords = true
            cachedGlobalMatrix = globalMatrix

            // @TODO: Can we generalize this to be placed in KorMA?
            val bitmapSize = bitmap.bounds.size.toFloat()
            val width = this.width
            val height = this.height
            val finalRect = bitmapSize.applyScaleMode(Rectangle(0.0, 0.0, width, height), scaleMode, contentAnchor)

            val realL = finalRect.left.clamp(0.0, width).toFloat()
            val realT = finalRect.top.clamp(0.0, height).toFloat()
            val realR = finalRect.right.clamp(0.0, width).toFloat()
            val realB = finalRect.bottom.clamp(0.0, height).toFloat()

            val ratioL = realL.convertRange(finalRect.left.toFloat(), finalRect.right.toFloat(), 0f, 1f)
            val ratioR = realR.convertRange(finalRect.left.toFloat(), finalRect.right.toFloat(), 0f, 1f)
            val ratioT = realT.convertRange(finalRect.top.toFloat(), finalRect.bottom.toFloat(), 0f, 1f)
            val ratioB = realB.convertRange(finalRect.top.toFloat(), finalRect.bottom.toFloat(), 0f, 1f)

            //println("finalRect=$finalRect, ratioL=$ratioL, ratioR=$ratioR, ratioT=$ratioT, ratioB=$ratioB")

            vertices.quad(
                0,
                realL,
                realT,
                (realR - realL),
                (realB - realT),
                globalMatrix,
                ratioL.convertRange(0f, 1f, bitmap.tlX, bitmap.trX),
                ratioT.convertRange(0f, 1f, bitmap.tlY, bitmap.blY),
                ratioR.convertRange(0f, 1f, bitmap.tlX, bitmap.trX),
                ratioT.convertRange(0f, 1f, bitmap.trY, bitmap.brY),
                ratioL.convertRange(0f, 1f, bitmap.blX, bitmap.brX),
                ratioB.convertRange(0f, 1f, bitmap.tlY, bitmap.blY),
                ratioR.convertRange(0f, 1f, bitmap.blX, bitmap.brX),
                ratioB.convertRange(0f, 1f, bitmap.trY, bitmap.brY),
                renderColorMul,
            )
        }
        ctx.useBatcher { batch ->
            if (bgcolor.a != 0) batch.drawQuad(
                ctx.getTex(Bitmaps.white),
                0f,
                0f,
                width.toFloat(),
                height.toFloat(),
                globalMatrix,
                colorMul = bgcolor
            )
            batch.drawVertices(
                vertices,
                ctx.getTex(bitmap).base,
                smoothing,
                renderBlendMode,
            )
        }
    }
}
