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

fun VarType.toMetalVertexFormat() = when (this) {
    VarType.TVOID -> TODO()
    VarType.Mat2 -> TODO()
    VarType.Mat3 -> TODO()
    VarType.Mat4 -> TODO()
    VarType.Sampler1D -> TODO()
    VarType.Sampler2D -> TODO()
    VarType.Sampler3D -> TODO()
    VarType.SamplerCube -> TODO()
    VarType.Int1 -> TODO()
    VarType.Float1 -> MTLVertexFormatFloat
    VarType.Float2 -> MTLVertexFormatFloat2
    VarType.Float3 -> MTLVertexFormatFloat3
    VarType.Float4 -> MTLVertexFormatFloat4
    VarType.Short1 -> TODO()
    VarType.Short2 -> TODO()
    VarType.Short3 -> TODO()
    VarType.Short4 -> TODO()
    VarType.Bool1 -> TODO()
    VarType.Bool2 -> TODO()
    VarType.Bool3 -> TODO()
    VarType.Bool4 -> TODO()
    VarType.Byte4 -> MTLVertexFormatChar4
    VarType.SByte1 -> TODO()
    VarType.SByte2 -> TODO()
    VarType.SByte3 -> TODO()
    VarType.SByte4 -> TODO()
    VarType.UByte1 -> TODO()
    VarType.UByte2 -> MTLVertexFormatChar2
    VarType.UByte3 -> MTLVertexFormatChar3
    VarType.UByte4 -> MTLVertexFormatChar4
    VarType.SShort1 -> TODO()
    VarType.SShort2 -> TODO()
    VarType.SShort3 -> TODO()
    VarType.SShort4 -> TODO()
    VarType.UShort1 -> TODO()
    VarType.UShort2 -> TODO()
    VarType.UShort3 -> TODO()
    VarType.UShort4 -> TODO()
    VarType.SInt1 -> TODO()
    VarType.SInt2 -> TODO()
    VarType.SInt3 -> TODO()
    VarType.SInt4 -> TODO()
}
