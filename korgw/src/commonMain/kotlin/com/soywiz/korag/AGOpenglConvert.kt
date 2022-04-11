package com.soywiz.korag

import com.soywiz.kgl.*
import com.soywiz.korag.shader.*

fun AGCullFace.toGl(gl: KmlGl): Int = when (this) {
    AGCullFace.BOTH -> gl.FRONT_AND_BACK
    AGCullFace.FRONT -> gl.FRONT
    AGCullFace.BACK -> gl.BACK
}

fun AGFrontFace.toGl(gl: KmlGl): Int = when (this) {
    AGFrontFace.BOTH -> gl.CCW // @TODO: Invalid
    AGFrontFace.CW -> gl.CW
    AGFrontFace.CCW -> gl.CCW // Default
}

fun AGCompareMode.toGl(gl: KmlGl): Int = when (this) {
    AGCompareMode.ALWAYS -> gl.ALWAYS
    AGCompareMode.EQUAL -> gl.EQUAL
    AGCompareMode.GREATER -> gl.GREATER
    AGCompareMode.GREATER_EQUAL -> gl.GEQUAL
    AGCompareMode.LESS -> gl.LESS
    AGCompareMode.LESS_EQUAL -> gl.LEQUAL
    AGCompareMode.NEVER -> gl.NEVER
    AGCompareMode.NOT_EQUAL -> gl.NOTEQUAL
}

fun AGDrawType.toGl(gl: KmlGl): Int = when (this) {
    AGDrawType.POINTS -> gl.POINTS
    AGDrawType.LINE_STRIP -> gl.LINE_STRIP
    AGDrawType.LINE_LOOP -> gl.LINE_LOOP
    AGDrawType.LINES -> gl.LINES
    AGDrawType.TRIANGLE_STRIP -> gl.TRIANGLE_STRIP
    AGDrawType.TRIANGLE_FAN -> gl.TRIANGLE_FAN
    AGDrawType.TRIANGLES -> gl.TRIANGLES
}

fun AGIndexType.toGl(gl: KmlGl): Int = when (this) {
    AGIndexType.UBYTE -> gl.UNSIGNED_BYTE
    AGIndexType.USHORT -> gl.UNSIGNED_SHORT
    AGIndexType.UINT -> gl.UNSIGNED_INT
}

fun AGEnable.toGl(gl: KmlGl): Int = when (this) {
    AGEnable.BLEND -> gl.BLEND
    AGEnable.CULL_FACE -> gl.CULL_FACE
    AGEnable.DEPTH -> gl.DEPTH_TEST
    AGEnable.SCISSOR -> gl.SCISSOR_TEST
    AGEnable.STENCIL -> gl.STENCIL_TEST
}

fun AGBlendEquation.toGl(gl: KmlGl): Int = when (this) {
    AGBlendEquation.ADD -> gl.FUNC_ADD
    AGBlendEquation.SUBTRACT -> gl.FUNC_SUBTRACT
    AGBlendEquation.REVERSE_SUBTRACT -> gl.FUNC_REVERSE_SUBTRACT
}

fun AGBlendFactor.toGl(gl: KmlGl): Int = when (this) {
    AGBlendFactor.DESTINATION_ALPHA -> gl.DST_ALPHA
    AGBlendFactor.DESTINATION_COLOR -> gl.DST_COLOR
    AGBlendFactor.ONE -> gl.ONE
    AGBlendFactor.ONE_MINUS_DESTINATION_ALPHA -> gl.ONE_MINUS_DST_ALPHA
    AGBlendFactor.ONE_MINUS_DESTINATION_COLOR -> gl.ONE_MINUS_DST_COLOR
    AGBlendFactor.ONE_MINUS_SOURCE_ALPHA -> gl.ONE_MINUS_SRC_ALPHA
    AGBlendFactor.ONE_MINUS_SOURCE_COLOR -> gl.ONE_MINUS_SRC_COLOR
    AGBlendFactor.SOURCE_ALPHA -> gl.SRC_ALPHA
    AGBlendFactor.SOURCE_COLOR -> gl.SRC_COLOR
    AGBlendFactor.ZERO -> gl.ZERO
}

fun AGTriangleFace.toGl(gl: KmlGl) = when (this) {
    AGTriangleFace.FRONT -> gl.FRONT
    AGTriangleFace.BACK -> gl.BACK
    AGTriangleFace.FRONT_AND_BACK -> gl.FRONT_AND_BACK
    AGTriangleFace.NONE -> gl.FRONT
}

fun AGStencilOp.toGl(gl: KmlGl) = when (this) {
    AGStencilOp.DECREMENT_SATURATE -> gl.DECR
    AGStencilOp.DECREMENT_WRAP -> gl.DECR_WRAP
    AGStencilOp.INCREMENT_SATURATE -> gl.INCR
    AGStencilOp.INCREMENT_WRAP -> gl.INCR_WRAP
    AGStencilOp.INVERT -> gl.INVERT
    AGStencilOp.KEEP -> gl.KEEP
    AGStencilOp.SET -> gl.REPLACE
    AGStencilOp.ZERO -> gl.ZERO
}

fun AG.TextureTargetKind.toGl(gl: KmlGl): Int = when (this) {
    AG.TextureTargetKind.TEXTURE_2D -> gl.TEXTURE_2D
    AG.TextureTargetKind.TEXTURE_3D -> gl.TEXTURE_3D
    AG.TextureTargetKind.TEXTURE_CUBE_MAP -> gl.TEXTURE_CUBE_MAP
}

fun VarType.toGl(gl: KmlGl): Int = when (this.kind) {
    VarKind.TBYTE -> gl.BYTE
    VarKind.TUNSIGNED_BYTE -> gl.UNSIGNED_BYTE
    VarKind.TSHORT -> gl.SHORT
    VarKind.TUNSIGNED_SHORT -> gl.UNSIGNED_SHORT
    VarKind.TINT -> gl.UNSIGNED_INT
    VarKind.TFLOAT -> gl.FLOAT
}
