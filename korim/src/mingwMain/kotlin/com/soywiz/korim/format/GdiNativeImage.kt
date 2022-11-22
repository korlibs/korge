package com.soywiz.korim.format

import com.soywiz.kds.iterators.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.paint.*
import com.soywiz.korim.vector.*
import com.soywiz.korim.vector.renderer.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.vector.*
import com.soywiz.korma.geom.vector.LineCap
import com.soywiz.korma.geom.vector.LineJoin
import kotlinx.cinterop.*
import platform.gdiplus.*
import platform.windows.*

class GdiNativeImage(bitmap: Bitmap32) : BitmapNativeImage(bitmap) {
    override val name: String get() = "GdiNativeImage"
    // @TODO: Enable this once ready!
    //override fun getContext2d(antialiasing: Boolean): Context2d = Context2d(GdiRenderer(bitmap, antialiasing))
}

/**
 * http://www.jose.it-berater.org/gdiplus/iframe/index.htm
 */
@OptIn(ExperimentalUnsignedTypes::class)
class GdiRenderer(val bitmap: Bitmap32, val antialiasing: Boolean) : BufferedRenderer() {
    override val width: Int get() = bitmap.width
    override val height: Int get() = bitmap.height

    val gdipErrors = arrayOf(
        "Ok", "GenericError", "InvalidParameter", "OutOfMemory",
        "ObjectBusy", "InsufficientBuffer", "NotImplemented", "Win32Error", "WrongState", "Aborted",
        "FileNotFound", "ValueOverflow", "AccessDenied", "UnknownImageFormat", "FontFamilyNotFound",
        "FontStyleNotFound", "NotTrueTypeFont", "UnsupportedGdiplusVersion", "GdiplusNotInitialized",
        "PropertyNotFound", "PropertyNotSupported", "ProfileNotFound",
    )

    private fun UInt.checkp(name: String): UInt {
        val error = this
        if (error != Ok) {
            println("ERROR: $name : $error : ${gdipErrors.getOrNull(error.toInt())}")
        }
        return this
    }

    override fun isBuffering(): Boolean = true

    fun Winding.toGdip() = when (this) {
        Winding.EVEN_ODD -> FillModeAlternate
        Winding.NON_ZERO -> FillModeWinding
    }

    fun LineCap.toGdip(): platform.gdiplus.LineCap = when (this) {
        LineCap.BUTT -> LineCapFlat
        LineCap.SQUARE -> LineCapSquare
        LineCap.ROUND -> LineCapRound
    }

    fun LineJoin.toGdip(): platform.gdiplus.LineJoin = when (this) {
        LineJoin.BEVEL -> LineJoinBevel
        LineJoin.MITER -> LineJoinMiter
        LineJoin.ROUND -> LineJoinRound
    }

    fun RGBA.toArgb(): UInt = this.value.toUInt()

    fun Double.ensureY(bitmap: Bitmap): Double {
        if (FLIP_POINTS) {
            return bitmap.height - this
        } else {
            return this
        }
    }

    private fun createPath(ppath: CArrayPointer<COpaquePointerVar>, vpath: VectorPath?, bitmap: Bitmap): COpaquePointer? {
        if (vpath == null) return null
        GdipCreatePath(vpath.winding.toGdip(), ppath).checkp("GdipCreatePath")
        val path = ppath[0]
        GdipStartPathFigure(path).checkp("GdipStartPathFigure")
        vpath.visitEdgesSimple(
            { x0, y0, x1, y1 ->
                GdipAddPathLine(path, x0.toFloat(), y0.ensureY(bitmap).toFloat(), x1.toFloat(), y1.ensureY(bitmap).toFloat()).checkp("GdipAddPathLine")
            },
            { x0, y0, x1, y1, x2, y2, x3, y3 ->
                GdipAddPathBezier(
                    path,
                    x0.toFloat(), y0.ensureY(bitmap).toFloat(),
                    x1.toFloat(), y1.ensureY(bitmap).toFloat(),
                    x2.toFloat(), y2.ensureY(bitmap).toFloat(),
                    x3.toFloat(), y3.ensureY(bitmap).toFloat()
                ).checkp("GdipAddPathBezier")
            },
            {
                GdipClosePathFigure(path).checkp("GdipClosePathFigure")
            }
        )
        return path
    }

    fun wrapMode(cycleX: CycleMethod, cycleY: CycleMethod = cycleX): WrapMode {
        return when {
            cycleX == CycleMethod.NO_CYCLE_CLAMP && cycleY == CycleMethod.NO_CYCLE_CLAMP -> WrapModeClamp
            cycleX == CycleMethod.NO_CYCLE && cycleY == CycleMethod.NO_CYCLE -> WrapModeTile
            cycleX == CycleMethod.REFLECT && cycleY == CycleMethod.REPEAT -> WrapModeTileFlipX
            cycleX == CycleMethod.REPEAT && cycleY == CycleMethod.REFLECT -> WrapModeTileFlipY
            cycleX == CycleMethod.REFLECT && cycleY == CycleMethod.REFLECT -> WrapModeTileFlipXY
            else -> WrapModeTile
        }
    }

    companion object {
        //val FLIP_POINTS = true
        val FLIP_POINTS = false
    }

    override fun flushCommands(commands: List<RenderCommand>) {
        if (!FLIP_POINTS) bitmap.flipY()
        //println("flushCommands.flushCommands[${commands.size}]")
        memScoped {
            bitmap.ints.usePinned { dataPin ->
                val dataPtr = dataPin.addressOf(0)
                val bmpInfo = alloc<BITMAPINFO>()
                initGdiPlusOnce()
                val pgraphics = allocArray<COpaquePointerVar>(1)
                val winhdc = GetDC(null) ?: error("winhdc = null")
                val hdc = CreateCompatibleDC(winhdc) ?: error("hdc = null")
                val bmap = CreateCompatibleBitmap(winhdc, width, height) ?: error("bmap = null")
                SelectObject(hdc, bmap)

                bmpInfo.bmiHeader.also {
                    it.biSize = sizeOf<BITMAPINFO>().convert()
                    it.biWidth = width
                    it.biHeight = height
                    it.biPlanes = 1.convert()
                    it.biBitCount = 32.convert()
                    it.biCompression = BI_RGB.convert()
                    it.biSizeImage = (width * height * 4).convert()
                    it.biXPelsPerMeter = 1024
                    it.biYPelsPerMeter = 1024
                    it.biClrUsed = 0.convert()
                    it.biClrImportant = 0.convert()
                }
                SetDIBits(hdc, bmap, 0, height.convert(), dataPtr, bmpInfo.ptr, DIB_RGB_COLORS)

                GdipCreateFromHDC(hdc, pgraphics).checkp("GdipCreateFromHDC")
                val graphics = pgraphics[0]
                GdipSetSmoothingMode(
                    graphics,
                    if (antialiasing) SmoothingModeHighQuality else SmoothingModeHighSpeed
                ).checkp("GdipSetSmoothingMode")

                //val bmp = alloc<IntVar>()
                //GdipCreateBitmapFromScan0(width, height, width * 4, if (this.bitmap.premultiplied) PixelFormat32bppARGB else PixelFormat32bppPARGB, scan, bmp)
                try {
                    commands.fastForEach { command ->
                        val state = command.state
                        val fill = command.fill
                        val ppath = allocArray<COpaquePointerVar>(1)
                        val pclip = allocArray<COpaquePointerVar>(1)
                        val path = createPath(ppath, state.path, bitmap)
                        val clip = createPath(pclip, state.clip, bitmap)

                        val pbrush = allocArray<COpaquePointerVar>(1)
                        val pmatrix = allocArray<COpaquePointerVar>(1)
                        //val plineGradient = allocArray<COpaquePointerVar>(1)
                        val pbitmap = allocArray<COpaquePointerVar>(1)
                        val style: Paint = if (fill) state.fillStyle else state.strokeStyle

                        when (style) {
                            is RGBA -> {
                                GdipCreateSolidFill(style.toArgb(), pbrush).checkp("GdipCreateSolidFill")
                            }

                            is TransformedPaint -> {
                                GdipCreateMatrix(pmatrix)
                                val matrix = pmatrix[0]
                                //val transform = Matrix().copyFrom(style.transform)
                                val transform = Matrix().apply {
                                    identity()
                                    multiply(this, style.transform)
                                    multiply(this, state.transform)
                                }
                                GdipSetMatrixElements(matrix, transform.af, transform.bf, transform.cf, transform.df, transform.txf, transform.tyf)
                                when (style) {
                                    is BitmapPaint -> {
                                        val bmp = style.bmp32
                                        bmp.ints.usePinned { bmpPin ->
                                            val ptr = bmpPin.addressOf(0)
                                            GdipCreateBitmapFromScan0(
                                                bmp.width,
                                                bmp.height,
                                                4 * bmp.width,
                                                if (bmp.premultiplied) PixelFormat32bppPARGB else PixelFormat32bppARGB,
                                                ptr.reinterpret(),
                                                pbitmap
                                            )
                                        }
                                        GdipCreateTexture(pbitmap[0], wrapMode(style.cycleX, style.cycleY), pbrush)
                                        GdipSetTextureTransform(pbrush[0], matrix).checkp("GdipSetTextureTransform")
                                    }

                                    is GradientPaint -> {
                                        when (style.kind) {
                                            GradientKind.LINEAR -> {
                                                // @TODO:
                                                val colors = allocArray<UIntVar>(style.numberOfStops)
                                                val stops = allocArray<REALVar>(style.numberOfStops)
                                                val points = allocArray<GpPointF>(2)
                                                for (n in 0 until style.numberOfStops) {
                                                    colors[n] = RGBA(style.colors[n]).toArgb()
                                                    stops[n] = style.stops[n].toFloat()
                                                }

                                                val point1 = points[0].also {
                                                    it.X = style.x0.toFloat()
                                                    it.Y = style.y0.ensureY(bitmap).toFloat()
                                                }
                                                val point2 = points[1].also {
                                                    it.X = style.x1.toFloat()
                                                    it.Y = style.y1.ensureY(bitmap).toFloat()
                                                }
                                                GdipCreateLineBrush(
                                                    points[0].ptr,
                                                    points[1].ptr,
                                                    RGBA(style.colors.first()).toArgb(),
                                                    RGBA(style.colors.last()).toArgb(),
                                                    wrapMode(style.cycle),
                                                    pbrush
                                                ).checkp("GdipCreateLineBrush")
                                                GdipSetLinePresetBlend(pbrush[0], colors, stops, style.numberOfStops)
                                                GdipSetLineTransform(pbrush[0], matrix).checkp("GdipSetLineTransform")
                                                //println(" -- transform=$transform, ${points[0].X}, ${points[0].Y}, ${points[1].X}, ${points[1].Y}")
                                            }

                                            GradientKind.RADIAL -> {
                                                // @TODO:
                                                GdipCreateSolidFill(
                                                    Colors.RED.toArgb(),
                                                    pbrush
                                                ).checkp("GdipCreateSolidFill")
                                            }

                                            GradientKind.SWEEP -> {
                                                // @TODO:
                                                GdipCreateSolidFill(
                                                    Colors.RED.toArgb(),
                                                    pbrush
                                                ).checkp("GdipCreateSolidFill")
                                            }
                                        }
                                    }
                                }
                            }
                            else -> {
                                GdipCreateSolidFill(Colors.RED.toArgb(), pbrush).checkp("GdipCreateSolidFill")
                            }
                        }
                        val brush = pbrush[0]
                        if (state.clip != null) {
                            GdipSetClipPath(graphics, clip, CombineModeIntersect)
                        } else {
                            GdipResetClip(graphics)
                        }

                        if (brush != null) {
                            memScoped {
                                if (fill) {
                                    GdipFillPath(graphics, brush, path).checkp("GdipFillPath")
                                } else {
                                    val ppen = allocArray<COpaquePointerVar>(1)
                                    GdipCreatePen2(
                                        brush,
                                        state.lineWidth.toFloat(),
                                        UnitPixel,
                                        ppen
                                    ).checkp("GdipCreatePen2")
                                    val pen = ppen[0]
                                    GdipSetPenEndCap(pen, state.endLineCap.toGdip()).checkp("GdipSetPenEndCap")
                                    GdipSetPenStartCap(pen, state.startLineCap.toGdip()).checkp("GdipSetPenStartCap")
                                    GdipSetPenLineJoin(pen, state.lineJoin.toGdip()).checkp("GdipSetPenLineJoin")
                                    GdipSetPenMiterLimit(pen, state.miterLimit.toFloat()).checkp("GdipSetPenMiterLimit")

                                    val lineDashFloatArray = state.lineDashFloatArray
                                    if (lineDashFloatArray != null) {
                                        val data = allocArray<REALVar>(lineDashFloatArray.size)
                                        for (n in lineDashFloatArray.indices) data[n] = lineDashFloatArray[n]
                                        GdipSetPenDashArray(graphics, data, lineDashFloatArray.size)
                                    } else {
                                        GdipSetPenDashArray(graphics, null, 0)
                                    }
                                    GdipSetPenDashOffset(graphics, state.lineDashOffset.toFloat())
                                    GdipDrawPath(graphics, pen, path).checkp("GdipDrawPath")
                                    GdipDeletePen(pen).checkp("GdipDeletePen")
                                }
                            }
                        }

                        //if (plineGradient[0] != null) GdipDeleteBrush(plineGradient[0])
                        if (pbitmap[0] != null) GdipDeleteCachedBitmap(pbitmap[0])
                        if (pmatrix[0] != null) GdipDeleteMatrix(pmatrix[0]).checkp("GdipDeleteMatrix")
                        GdipDeleteBrush(brush).checkp("GdipDeleteBrush")
                        GdipDeletePath(path).checkp("GdipDeletePath")
                        if (clip != null) GdipDeletePath(clip).checkp("GdipDeletePath")
                        GdipFlush(graphics, FlushIntentionSync).checkp("GdipFlush")

                        GetDIBits(hdc, bmap, 0, height.convert(), dataPtr, bmpInfo.ptr, DIB_RGB_COLORS)
                    }
                } finally {
                    GdipDeleteGraphics(graphics)
                    DeleteObject(bmap)
                    DeleteDC(hdc)
                }
            }
        }
        if (!FLIP_POINTS) bitmap.flipY()
    }
}
