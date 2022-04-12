package com.soywiz.korag

import com.soywiz.kgl.KmlGl
import com.soywiz.korag.shader.*

fun AGCullFace.toGl(): Int = when (this) {
    AGCullFace.BOTH -> KmlGl.FRONT_AND_BACK
    AGCullFace.FRONT -> KmlGl.FRONT
    AGCullFace.BACK -> KmlGl.BACK
}

fun AGFrontFace.toGl(): Int = when (this) {
    AGFrontFace.BOTH -> KmlGl.CCW // @TODO: Invalid
    AGFrontFace.CW -> KmlGl.CW
    AGFrontFace.CCW -> KmlGl.CCW // Default
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
}

fun AGDrawType.toGl(): Int = when (this) {
    AGDrawType.POINTS -> KmlGl.POINTS
    AGDrawType.LINE_STRIP -> KmlGl.LINE_STRIP
    AGDrawType.LINE_LOOP -> KmlGl.LINE_LOOP
    AGDrawType.LINES -> KmlGl.LINES
    AGDrawType.TRIANGLE_STRIP -> KmlGl.TRIANGLE_STRIP
    AGDrawType.TRIANGLE_FAN -> KmlGl.TRIANGLE_FAN
    AGDrawType.TRIANGLES -> KmlGl.TRIANGLES
}

fun AGIndexType.toGl(): Int = when (this) {
    AGIndexType.UBYTE -> KmlGl.UNSIGNED_BYTE
    AGIndexType.USHORT -> KmlGl.UNSIGNED_SHORT
    AGIndexType.UINT -> KmlGl.UNSIGNED_INT
}

fun AGEnable.toGl(): Int = when (this) {
    AGEnable.BLEND -> KmlGl.BLEND
    AGEnable.CULL_FACE -> KmlGl.CULL_FACE
    AGEnable.DEPTH -> KmlGl.DEPTH_TEST
    AGEnable.SCISSOR -> KmlGl.SCISSOR_TEST
    AGEnable.STENCIL -> KmlGl.STENCIL_TEST
}

fun AGBlendEquation.toGl(): Int = when (this) {
    AGBlendEquation.ADD -> KmlGl.FUNC_ADD
    AGBlendEquation.SUBTRACT -> KmlGl.FUNC_SUBTRACT
    AGBlendEquation.REVERSE_SUBTRACT -> KmlGl.FUNC_REVERSE_SUBTRACT
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
}

fun AGTriangleFace.toGl() = when (this) {
    AGTriangleFace.FRONT -> KmlGl.FRONT
    AGTriangleFace.BACK -> KmlGl.BACK
    AGTriangleFace.FRONT_AND_BACK -> KmlGl.FRONT_AND_BACK
    AGTriangleFace.NONE -> KmlGl.FRONT
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
}

fun AG.TextureTargetKind.toGl(): Int = when (this) {
    AG.TextureTargetKind.TEXTURE_2D -> KmlGl.TEXTURE_2D
    AG.TextureTargetKind.TEXTURE_3D -> KmlGl.TEXTURE_3D
    AG.TextureTargetKind.TEXTURE_CUBE_MAP -> KmlGl.TEXTURE_CUBE_MAP
}

fun VarType.toGl(): Int = when (this.kind) {
    VarKind.TBYTE -> KmlGl.BYTE
    VarKind.TUNSIGNED_BYTE -> KmlGl.UNSIGNED_BYTE
    VarKind.TSHORT -> KmlGl.SHORT
    VarKind.TUNSIGNED_SHORT -> KmlGl.UNSIGNED_SHORT
    VarKind.TINT -> KmlGl.UNSIGNED_INT
    VarKind.TFLOAT -> KmlGl.FLOAT
}
