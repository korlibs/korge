package com.soywiz.korim.format

import com.soywiz.kds.iterators.fastForEach
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RGBA
import com.soywiz.korim.paint.BitmapPaint
import com.soywiz.korim.vector.Context2d
import com.soywiz.korim.vector.CycleMethod
import com.soywiz.korim.vector.renderer.BufferedRenderer
import com.soywiz.korma.geom.Matrix
import com.soywiz.korma.geom.vector.LineCap
import com.soywiz.korma.geom.vector.LineJoin
import com.soywiz.korma.geom.vector.VectorPath
import com.soywiz.korma.geom.vector.Winding
import kotlinx.cinterop.*
import platform.gdiplus.CombineModeIntersect
import platform.gdiplus.FillModeAlternate
import platform.gdiplus.FillModeWinding
import platform.gdiplus.FlushIntentionSync
import platform.gdiplus.GdipAddPathBezier
import platform.gdiplus.GdipAddPathLine
import platform.gdiplus.GdipClosePathFigure
import platform.gdiplus.GdipCreateBitmapFromScan0
import platform.gdiplus.GdipCreateFromHDC
import platform.gdiplus.GdipCreateMatrix
import platform.gdiplus.GdipCreatePath
import platform.gdiplus.GdipCreatePen2
import platform.gdiplus.GdipCreateSolidFill
import platform.gdiplus.GdipCreateTexture
import platform.gdiplus.GdipDeleteBrush
import platform.gdiplus.GdipDeleteCachedBitmap
import platform.gdiplus.GdipDeleteGraphics
import platform.gdiplus.GdipDeleteMatrix
import platform.gdiplus.GdipDeletePath
import platform.gdiplus.GdipDeletePen
import platform.gdiplus.GdipDrawPath
import platform.gdiplus.GdipFillPath
import platform.gdiplus.GdipFlush
import platform.gdiplus.GdipResetClip
import platform.gdiplus.GdipSetClipPath
import platform.gdiplus.GdipSetMatrixElements
import platform.gdiplus.GdipSetPenDashArray
import platform.gdiplus.GdipSetPenDashOffset
import platform.gdiplus.GdipSetPenEndCap
import platform.gdiplus.GdipSetPenLineJoin
import platform.gdiplus.GdipSetPenMiterLimit
import platform.gdiplus.GdipSetPenStartCap
import platform.gdiplus.GdipSetSmoothingMode
import platform.gdiplus.GdipSetTextureTransform
import platform.gdiplus.GdipStartPathFigure
import platform.gdiplus.LineCapFlat
import platform.gdiplus.LineCapRound
import platform.gdiplus.LineCapSquare
import platform.gdiplus.LineJoinBevel
import platform.gdiplus.LineJoinMiter
import platform.gdiplus.LineJoinRound
import platform.gdiplus.Ok
import platform.gdiplus.PixelFormat32bppARGB
import platform.gdiplus.PixelFormat32bppPARGB
import platform.gdiplus.REALVar
import platform.gdiplus.SmoothingModeHighQuality
import platform.gdiplus.SmoothingModeHighSpeed
import platform.gdiplus.UnitPixel
import platform.gdiplus.WrapModeClamp
import platform.gdiplus.WrapModeTile
import platform.gdiplus.WrapModeTileFlipX
import platform.gdiplus.WrapModeTileFlipXY
import platform.gdiplus.WrapModeTileFlipY
import platform.windows.BITMAPINFO
import platform.windows.BI_RGB
import platform.windows.CreateCompatibleBitmap
import platform.windows.CreateCompatibleDC
import platform.windows.DIB_RGB_COLORS
import platform.windows.DeleteDC
import platform.windows.DeleteObject
import platform.windows.GetDC
import platform.windows.GetDIBits
import platform.windows.SelectObject
import platform.windows.SetDIBits

class GdiNativeImage(bitmap: Bitmap32) : BitmapNativeImage(bitmap) {
    // @TODO: Enable this once ready!
    //override fun getContext2d(antialiasing: Boolean): Context2d = Context2d(GdiRenderer(bitmap, antialiasing))
}

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

    fun LineCap.toGdip() = when (this) {
        LineCap.BUTT -> LineCapFlat
        LineCap.SQUARE -> LineCapSquare
        LineCap.ROUND -> LineCapRound
    }

    fun LineJoin.toGdip() = when (this) {
        LineJoin.BEVEL -> LineJoinBevel
        LineJoin.MITER -> LineJoinMiter
        LineJoin.ROUND -> LineJoinRound
    }

    private fun createPath(ppath: CArrayPointer<COpaquePointerVar>, vpath: VectorPath?): COpaquePointer? {
        if (vpath == null) return null
        GdipCreatePath(vpath.winding.toGdip(), ppath).checkp("GdipCreatePath")
        val path = ppath[0]
        GdipStartPathFigure(path).checkp("GdipStartPathFigure")
        vpath.visitEdgesSimple(
            { x0, y0, x1, y1 ->
                GdipAddPathLine(path, x0.toFloat(), y0.toFloat(), x1.toFloat(), y1.toFloat()).checkp("GdipAddPathLine")
            },
            { x0, y0, x1, y1, x2, y2, x3, y3 ->
                GdipAddPathBezier(path,
                    x0.toFloat(), y0.toFloat(),
                    x1.toFloat(), y1.toFloat(),
                    x2.toFloat(), y2.toFloat(),
                    x3.toFloat(), y3.toFloat()
                ).checkp("GdipAddPathBezier")
            },
            {
                GdipClosePathFigure(path).checkp("GdipClosePathFigure")
            }
        )
        return path
    }

    override fun flushCommands(commands: List<RenderCommand>) {
        bitmap.flipY()
        memScoped {
            bitmap.intData.usePinned {  dataPin ->
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
                GdipSetSmoothingMode(graphics, if (antialiasing) SmoothingModeHighQuality else SmoothingModeHighSpeed).checkp("GdipSetSmoothingMode")

                //val bmp = alloc<IntVar>()
                //GdipCreateBitmapFromScan0(width, height, width * 4, if (this.bitmap.premultiplied) PixelFormat32bppARGB else PixelFormat32bppPARGB, scan, bmp)
                try {
                    commands.fastForEach { command ->
                        val state = command.state
                        val fill = command.fill
                        val ppath = allocArray<COpaquePointerVar>(1)
                        val pclip = allocArray<COpaquePointerVar>(1)
                        val path = createPath(ppath, state.path)
                        val clip = createPath(pclip, state.clip)

                        val pbrush = allocArray<COpaquePointerVar>(1)
                        val pmatrix = allocArray<COpaquePointerVar>(1)
                        val pbitmap = allocArray<COpaquePointerVar>(1)
                        val style = if (fill) state.fillStyle else state.strokeStyle

                        when (style) {
                            is RGBA -> {
                                GdipCreateSolidFill(style.value.toUInt(), pbrush).checkp("GdipCreateSolidFill")
                            }
                            is BitmapPaint -> {
                                GdipCreateMatrix(pmatrix)
                                val matrix = pmatrix[0]
                                //val transform = Matrix().copyFrom(style.transform)
                                val transform = Matrix().apply {
                                    identity()
                                    multiply(this, style.transform)
                                    multiply(this, state.transform)
                                }

                                //transform.invert()
                                //style.transform
                                //transform.invert()
                                //val transform = style.transform
                                GdipSetMatrixElements(matrix, transform.af, transform.bf, transform.cf, transform.df, transform.txf, transform.tyf)
                                val bmp = style.bmp32
                                bmp.intData.usePinned { bmpPin ->
                                    val ptr = bmpPin.addressOf(0)
                                    GdipCreateBitmapFromScan0(bmp.width, bmp.height, 4 * bmp.width, if (bmp.premultiplied) PixelFormat32bppARGB else PixelFormat32bppPARGB, ptr.reinterpret(), pbitmap)
                                }
                                val wrapMode = when {
                                    style.cycleX == CycleMethod.NO_CYCLE_CLAMP && style.cycleY == CycleMethod.NO_CYCLE_CLAMP -> WrapModeClamp
                                    style.cycleX == CycleMethod.NO_CYCLE && style.cycleY == CycleMethod.NO_CYCLE -> WrapModeTile
                                    style.cycleX == CycleMethod.REFLECT && style.cycleY == CycleMethod.REPEAT -> WrapModeTileFlipX
                                    style.cycleX == CycleMethod.REPEAT && style.cycleY == CycleMethod.REFLECT -> WrapModeTileFlipY
                                    style.cycleX == CycleMethod.REFLECT && style.cycleY == CycleMethod.REFLECT -> WrapModeTileFlipXY
                                    else -> WrapModeTile
                                }
                                GdipCreateTexture(pbitmap[0], wrapMode, pbrush)
                                GdipSetTextureTransform(pbrush[0], matrix)
                            }
                            // Missing gradients
                            else -> {
                                GdipCreateSolidFill(Colors.RED.value.toUInt(), pbrush).checkp("GdipCreateSolidFill")
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
                                    GdipCreatePen2(brush, state.lineWidth.toFloat(), UnitPixel, ppen).checkp("GdipCreatePen2")
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
        bitmap.flipY()
    }
}
