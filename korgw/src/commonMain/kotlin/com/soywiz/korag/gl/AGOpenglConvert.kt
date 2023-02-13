package com.soywiz.korag.gl

import com.soywiz.kgl.KmlGl
import com.soywiz.korag.*
import com.soywiz.korag.shader.VarKind
import com.soywiz.korag.shader.VarType
import com.soywiz.korio.lang.*

fun AGCullFace.toGl(): Int = when (this) {
    AGCullFace.BOTH -> KmlGl.FRONT_AND_BACK
    AGCullFace.FRONT -> KmlGl.FRONT
    AGCullFace.BACK -> KmlGl.BACK
    else -> TODO("Invalid AGCullFace($this)")
}

fun AGFrontFace.toGl(): Int = when (this) {
    AGFrontFace.BOTH -> KmlGl.CCW // @TODO: Invalid
    AGFrontFace.CW -> KmlGl.CW
    AGFrontFace.CCW -> KmlGl.CCW // Default
    else -> unreachable
}

fun AGCompareMode.toGl(): Int = when (this) {
    AGCompareMode.ALWAYS -> KmlGl.ALWAYS
    AGCompareMode.EQUAL -> KmlGl.EQUAL
    AGCompareMode.GREATER -> KmlGl.GREATER
    AGCompareMode.GREATER_EQUAL -> KmlGl.GEQUAL
    AGCompareMode.LESS -> KmlGl.LESS
    AGCompareMode.LESS_EQUAL -> KmlGl.LEQUAL
    AGCompareMode.NEVER -> KmlGl.NEVER
    AGCompareMode.NOT_EQUAL -> KmlGl.NOTEQUAL
    else -> unreachable
}

fun AGDrawType.toGl(): Int = when (this) {
    AGDrawType.POINTS -> KmlGl.POINTS
    AGDrawType.LINE_STRIP -> KmlGl.LINE_STRIP
    AGDrawType.LINE_LOOP -> KmlGl.LINE_LOOP
    AGDrawType.LINES -> KmlGl.LINES
    AGDrawType.TRIANGLE_STRIP -> KmlGl.TRIANGLE_STRIP
    AGDrawType.TRIANGLE_FAN -> KmlGl.TRIANGLE_FAN
    AGDrawType.TRIANGLES -> KmlGl.TRIANGLES
    else -> unreachable
}

fun AGIndexType.toGl(): Int = when (this) {
    AGIndexType.NONE -> KmlGl.NONE
    AGIndexType.UBYTE -> KmlGl.UNSIGNED_BYTE
    AGIndexType.USHORT -> KmlGl.UNSIGNED_SHORT
    AGIndexType.UINT -> KmlGl.UNSIGNED_INT
    else -> unreachable
}

fun AGBlendEquation.toGl(): Int = when (this) {
    AGBlendEquation.ADD -> KmlGl.FUNC_ADD
    AGBlendEquation.SUBTRACT -> KmlGl.FUNC_SUBTRACT
    AGBlendEquation.REVERSE_SUBTRACT -> KmlGl.FUNC_REVERSE_SUBTRACT
    else -> unreachable
}

fun AGBlendFactor.toGl(): Int = when (this) {
    AGBlendFactor.DESTINATION_ALPHA -> KmlGl.DST_ALPHA
    AGBlendFactor.DESTINATION_COLOR -> KmlGl.DST_COLOR
    AGBlendFactor.ONE -> KmlGl.ONE
    AGBlendFactor.ONE_MINUS_DESTINATION_ALPHA -> KmlGl.ONE_MINUS_DST_ALPHA
    AGBlendFactor.ONE_MINUS_DESTINATION_COLOR -> KmlGl.ONE_MINUS_DST_COLOR
    AGBlendFactor.ONE_MINUS_SOURCE_ALPHA -> KmlGl.ONE_MINUS_SRC_ALPHA
    AGBlendFactor.ONE_MINUS_SOURCE_COLOR -> KmlGl.ONE_MINUS_SRC_COLOR
    AGBlendFactor.SOURCE_ALPHA -> KmlGl.SRC_ALPHA
    AGBlendFactor.SOURCE_COLOR -> KmlGl.SRC_COLOR
    AGBlendFactor.ZERO -> KmlGl.ZERO
    else -> unreachable
}

fun AGTriangleFace.toGl() = when (this) {
    AGTriangleFace.FRONT -> KmlGl.FRONT
    AGTriangleFace.BACK -> KmlGl.BACK
    AGTriangleFace.FRONT_AND_BACK -> KmlGl.FRONT_AND_BACK
    AGTriangleFace.NONE -> KmlGl.FRONT
    else -> unreachable
}

fun AGStencilOp.toGl() = when (this) {
    AGStencilOp.DECREMENT_SATURATE -> KmlGl.DECR
    AGStencilOp.DECREMENT_WRAP -> KmlGl.DECR_WRAP
    AGStencilOp.INCREMENT_SATURATE -> KmlGl.INCR
    AGStencilOp.INCREMENT_WRAP -> KmlGl.INCR_WRAP
    AGStencilOp.INVERT -> KmlGl.INVERT
    AGStencilOp.KEEP -> KmlGl.KEEP
    AGStencilOp.SET -> KmlGl.REPLACE
    AGStencilOp.ZERO -> KmlGl.ZERO
    else -> KmlGl.ZERO
}

fun AGTextureTargetKind.toGl(): Int = when (this) {
    AGTextureTargetKind.TEXTURE_2D -> KmlGl.TEXTURE_2D
    AGTextureTargetKind.TEXTURE_3D -> KmlGl.TEXTURE_3D
    AGTextureTargetKind.TEXTURE_CUBE_MAP -> KmlGl.TEXTURE_CUBE_MAP
    AGTextureTargetKind.EXTERNAL_TEXTURE -> KmlGl.TEXTURE_EXTERNAL_OES
    else -> this.ordinal
}

fun AGTextureTargetKind.Companion.fromGl(value: Int): AGTextureTargetKind = when (value) {
    KmlGl.TEXTURE_2D -> AGTextureTargetKind.TEXTURE_2D
    KmlGl.TEXTURE_3D -> AGTextureTargetKind.TEXTURE_3D
    KmlGl.TEXTURE_CUBE_MAP -> AGTextureTargetKind.TEXTURE_CUBE_MAP
    KmlGl.TEXTURE_EXTERNAL_OES -> AGTextureTargetKind.EXTERNAL_TEXTURE
    KmlGl.TEXTURE_CUBE_MAP_POSITIVE_X -> AGTextureTargetKind.TEXTURE_CUBE_MAP
    KmlGl.TEXTURE_CUBE_MAP_NEGATIVE_X -> AGTextureTargetKind.TEXTURE_CUBE_MAP
    KmlGl.TEXTURE_CUBE_MAP_POSITIVE_Y -> AGTextureTargetKind.TEXTURE_CUBE_MAP
    KmlGl.TEXTURE_CUBE_MAP_NEGATIVE_Y -> AGTextureTargetKind.TEXTURE_CUBE_MAP
    KmlGl.TEXTURE_CUBE_MAP_POSITIVE_Z -> AGTextureTargetKind.TEXTURE_CUBE_MAP
    KmlGl.TEXTURE_CUBE_MAP_NEGATIVE_Z -> AGTextureTargetKind.TEXTURE_CUBE_MAP
    else -> TODO("Unknown TextureTargetKind: $value")
}

fun AGWrapMode.toGl(): Int = when (this) {
    AGWrapMode.CLAMP_TO_EDGE -> KmlGl.CLAMP_TO_EDGE
    AGWrapMode.REPEAT -> KmlGl.REPEAT
    AGWrapMode.MIRRORED_REPEAT -> KmlGl.MIRRORED_REPEAT
    else -> KmlGl.CLAMP_TO_EDGE
}

fun VarType.toGl(): Int = when (this.kind) {
    VarKind.TBOOL -> KmlGl.BOOL
    VarKind.TBYTE -> KmlGl.BYTE
    VarKind.TUNSIGNED_BYTE -> KmlGl.UNSIGNED_BYTE
    VarKind.TSHORT -> KmlGl.SHORT
    VarKind.TUNSIGNED_SHORT -> KmlGl.UNSIGNED_SHORT
    VarKind.TINT -> KmlGl.UNSIGNED_INT
    VarKind.TFLOAT -> KmlGl.FLOAT
}

fun AGBufferKind.toGl(): Int = when (this) {
    AGBufferKind.INDEX -> KmlGl.ELEMENT_ARRAY_BUFFER
    AGBufferKind.VERTEX -> KmlGl.ARRAY_BUFFER
}
