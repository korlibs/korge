package com.soywiz.korag.gl

import com.soywiz.kgl.KmlGl
import com.soywiz.korag.AG
import com.soywiz.korag.AGBlendEquation
import com.soywiz.korag.AGBlendFactor
import com.soywiz.korag.AGCompareMode
import com.soywiz.korag.AGCullFace
import com.soywiz.korag.AGDrawType
import com.soywiz.korag.AGEnable
import com.soywiz.korag.AGFrontFace
import com.soywiz.korag.AGIndexType
import com.soywiz.korag.AGStencilOp
import com.soywiz.korag.AGTriangleFace
import com.soywiz.korag.shader.VarKind
import com.soywiz.korag.shader.VarType

fun AGCullFace.toGl(): Int = when (this) {
    AG.CullFace.BOTH -> KmlGl.FRONT_AND_BACK
    AG.CullFace.FRONT -> KmlGl.FRONT
    AG.CullFace.BACK -> KmlGl.BACK
}

fun AGFrontFace.toGl(): Int = when (this) {
    AG.FrontFace.BOTH -> KmlGl.CCW // @TODO: Invalid
    AG.FrontFace.CW -> KmlGl.CW
    AG.FrontFace.CCW -> KmlGl.CCW // Default
}

fun AGCompareMode.toGl(): Int = when (this) {
    AG.CompareMode.ALWAYS -> KmlGl.ALWAYS
    AG.CompareMode.EQUAL -> KmlGl.EQUAL
    AG.CompareMode.GREATER -> KmlGl.GREATER
    AG.CompareMode.GREATER_EQUAL -> KmlGl.GEQUAL
    AG.CompareMode.LESS -> KmlGl.LESS
    AG.CompareMode.LESS_EQUAL -> KmlGl.LEQUAL
    AG.CompareMode.NEVER -> KmlGl.NEVER
    AG.CompareMode.NOT_EQUAL -> KmlGl.NOTEQUAL
}

fun AGDrawType.toGl(): Int = when (this) {
    AG.DrawType.POINTS -> KmlGl.POINTS
    AG.DrawType.LINE_STRIP -> KmlGl.LINE_STRIP
    AG.DrawType.LINE_LOOP -> KmlGl.LINE_LOOP
    AG.DrawType.LINES -> KmlGl.LINES
    AG.DrawType.TRIANGLE_STRIP -> KmlGl.TRIANGLE_STRIP
    AG.DrawType.TRIANGLE_FAN -> KmlGl.TRIANGLE_FAN
    AG.DrawType.TRIANGLES -> KmlGl.TRIANGLES
}

fun AGIndexType.toGl(): Int = when (this) {
    AG.IndexType.UBYTE -> KmlGl.UNSIGNED_BYTE
    AG.IndexType.USHORT -> KmlGl.UNSIGNED_SHORT
    AG.IndexType.UINT -> KmlGl.UNSIGNED_INT
}

fun AGEnable.toGl(): Int = when (this) {
    AGEnable.BLEND -> KmlGl.BLEND
    AGEnable.CULL_FACE -> KmlGl.CULL_FACE
    AGEnable.DEPTH -> KmlGl.DEPTH_TEST
    AGEnable.SCISSOR -> KmlGl.SCISSOR_TEST
    AGEnable.STENCIL -> KmlGl.STENCIL_TEST
}

fun AGBlendEquation.toGl(): Int = when (this) {
    AG.BlendEquation.ADD -> KmlGl.FUNC_ADD
    AG.BlendEquation.SUBTRACT -> KmlGl.FUNC_SUBTRACT
    AG.BlendEquation.REVERSE_SUBTRACT -> KmlGl.FUNC_REVERSE_SUBTRACT
}

fun AGBlendFactor.toGl(): Int = when (this) {
    AG.BlendFactor.DESTINATION_ALPHA -> KmlGl.DST_ALPHA
    AG.BlendFactor.DESTINATION_COLOR -> KmlGl.DST_COLOR
    AG.BlendFactor.ONE -> KmlGl.ONE
    AG.BlendFactor.ONE_MINUS_DESTINATION_ALPHA -> KmlGl.ONE_MINUS_DST_ALPHA
    AG.BlendFactor.ONE_MINUS_DESTINATION_COLOR -> KmlGl.ONE_MINUS_DST_COLOR
    AG.BlendFactor.ONE_MINUS_SOURCE_ALPHA -> KmlGl.ONE_MINUS_SRC_ALPHA
    AG.BlendFactor.ONE_MINUS_SOURCE_COLOR -> KmlGl.ONE_MINUS_SRC_COLOR
    AG.BlendFactor.SOURCE_ALPHA -> KmlGl.SRC_ALPHA
    AG.BlendFactor.SOURCE_COLOR -> KmlGl.SRC_COLOR
    AG.BlendFactor.ZERO -> KmlGl.ZERO
}

fun AGTriangleFace.toGl() = when (this) {
    AG.TriangleFace.FRONT -> KmlGl.FRONT
    AG.TriangleFace.BACK -> KmlGl.BACK
    AG.TriangleFace.FRONT_AND_BACK -> KmlGl.FRONT_AND_BACK
    AG.TriangleFace.NONE -> KmlGl.FRONT
}

fun AGStencilOp.toGl() = when (this) {
    AG.StencilOp.DECREMENT_SATURATE -> KmlGl.DECR
    AG.StencilOp.DECREMENT_WRAP -> KmlGl.DECR_WRAP
    AG.StencilOp.INCREMENT_SATURATE -> KmlGl.INCR
    AG.StencilOp.INCREMENT_WRAP -> KmlGl.INCR_WRAP
    AG.StencilOp.INVERT -> KmlGl.INVERT
    AG.StencilOp.KEEP -> KmlGl.KEEP
    AG.StencilOp.SET -> KmlGl.REPLACE
    AG.StencilOp.ZERO -> KmlGl.ZERO
}

fun AG.TextureTargetKind.toGl(): Int = when (this) {
    AG.TextureTargetKind.TEXTURE_2D -> KmlGl.TEXTURE_2D
    AG.TextureTargetKind.TEXTURE_3D -> KmlGl.TEXTURE_3D
    AG.TextureTargetKind.TEXTURE_CUBE_MAP -> KmlGl.TEXTURE_CUBE_MAP
    AG.TextureTargetKind.EXTERNAL_TEXTURE -> KmlGl.TEXTURE_EXTERNAL_OES
}

fun AG.TextureTargetKind.Companion.fromGl(value: Int): AG.TextureTargetKind = when (value) {
    KmlGl.TEXTURE_2D -> AG.TextureTargetKind.TEXTURE_2D
    KmlGl.TEXTURE_3D -> AG.TextureTargetKind.TEXTURE_3D
    KmlGl.TEXTURE_CUBE_MAP -> AG.TextureTargetKind.TEXTURE_CUBE_MAP
    KmlGl.TEXTURE_EXTERNAL_OES -> AG.TextureTargetKind.EXTERNAL_TEXTURE
    KmlGl.TEXTURE_CUBE_MAP_POSITIVE_X -> AG.TextureTargetKind.TEXTURE_CUBE_MAP
    KmlGl.TEXTURE_CUBE_MAP_NEGATIVE_X -> AG.TextureTargetKind.TEXTURE_CUBE_MAP
    KmlGl.TEXTURE_CUBE_MAP_POSITIVE_Y -> AG.TextureTargetKind.TEXTURE_CUBE_MAP
    KmlGl.TEXTURE_CUBE_MAP_NEGATIVE_Y -> AG.TextureTargetKind.TEXTURE_CUBE_MAP
    KmlGl.TEXTURE_CUBE_MAP_POSITIVE_Z -> AG.TextureTargetKind.TEXTURE_CUBE_MAP
    KmlGl.TEXTURE_CUBE_MAP_NEGATIVE_Z -> AG.TextureTargetKind.TEXTURE_CUBE_MAP
    else -> TODO("Unknown TextureTargetKind: $value")
}

fun VarType.toGl(): Int = when (this.kind) {
    VarKind.TBYTE -> KmlGl.BYTE
    VarKind.TUNSIGNED_BYTE -> KmlGl.UNSIGNED_BYTE
    VarKind.TSHORT -> KmlGl.SHORT
    VarKind.TUNSIGNED_SHORT -> KmlGl.UNSIGNED_SHORT
    VarKind.TINT -> KmlGl.UNSIGNED_INT
    VarKind.TFLOAT -> KmlGl.FLOAT
}

fun AG.Buffer.Kind.toGl(): Int = when (this) {
    AG.Buffer.Kind.INDEX -> KmlGl.ELEMENT_ARRAY_BUFFER
    AG.Buffer.Kind.VERTEX -> KmlGl.ARRAY_BUFFER
}
