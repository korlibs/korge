package korlibs.metal

import korlibs.graphics.*
import korlibs.graphics.shader.*
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

fun VarType.toMetalVertexFormat(normalized: Boolean) = when (normalized) {
    false -> toMetalVertexFormat()
    else -> toMetalVertexFormatNormalized()
}

private fun VarType.toMetalVertexFormatNormalized() = when (this) {
    VarType.UByte2 -> MTLVertexFormatUChar2Normalized
    VarType.UByte3 -> MTLVertexFormatUChar3Normalized
    VarType.UByte4 -> MTLVertexFormatUChar4Normalized
    else -> TODO("implement with format: $this")
}

private fun VarType.toMetalVertexFormat() = when (this) {
    VarType.Float1 -> MTLVertexFormatFloat
    VarType.Float2 -> MTLVertexFormatFloat2
    VarType.Float3 -> MTLVertexFormatFloat3
    VarType.Float4 -> MTLVertexFormatFloat4
    VarType.Byte4 -> MTLVertexFormatChar4
    VarType.UByte2 -> MTLVertexFormatUChar2
    VarType.UByte3 -> MTLVertexFormatUChar3
    VarType.UByte4 -> MTLVertexFormatUChar4
    else -> TODO("implement with format: $this")
}
