package korlibs.metal

import korlibs.graphics.*
import korlibs.io.lang.*
import kotlinx.cinterop.*
import platform.Metal.*

internal fun AGFrameBufferInfo.toViewPort() = cValue<MTLViewport> {
    originX = 0.0
    originY = 0.0
    width = this@toViewPort.width.toDouble()
    height = this@toViewPort.height.toDouble()
    znear = 0.0
    zfar = 1.0
}

internal fun AGDrawType.toMetal(): MTLPrimitiveType = when (this) {
    AGDrawType.POINTS -> MTLPrimitiveTypePoint
    AGDrawType.LINE_STRIP -> MTLPrimitiveTypeLineStrip
    AGDrawType.LINE_LOOP -> invalidOp
    AGDrawType.LINES -> MTLPrimitiveTypeLine
    AGDrawType.TRIANGLE_STRIP -> MTLPrimitiveTypeTriangleStrip
    AGDrawType.TRIANGLE_FAN -> invalidOp
    AGDrawType.TRIANGLES -> MTLPrimitiveTypeTriangle
    else -> unreachable
}

internal fun AGIndexType.toMetal(): MTLIndexType = when (this) {
    AGIndexType.NONE -> invalidOp
    AGIndexType.UBYTE -> invalidOp
    AGIndexType.USHORT -> MTLIndexTypeUInt16
    AGIndexType.UINT -> MTLIndexTypeUInt32
    else -> unreachable
}