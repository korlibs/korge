package korge.graphics.backend.metal.shader

import com.soywiz.korag.shader.*
import com.soywiz.korio.lang.*


internal interface BaseMetalShaderGenerator {

    private fun errorType(type: VarType): Nothing = invalidOp("Don't know how to serialize type $type")

    fun precToString(prec: Precision) = when (prec) {
            Precision.DEFAULT -> ""
            Precision.LOW -> "lowp "
            Precision.MEDIUM -> "mediump "
            Precision.HIGH -> "highp "
    }

    fun typeToString(type: VarType) = when (type) {
        VarType.TVOID -> "void"
        VarType.Byte4 -> "vec4"
        VarType.Mat2 -> "mat2"
        VarType.Mat3 -> "mat3"
        VarType.Mat4 -> "mat4"
        VarType.Sampler1D -> "sampler1D"
        VarType.Sampler2D -> "sampler2D"
        VarType.Sampler3D -> "sampler3D"
        VarType.SamplerCube -> "samplerCube"
        else -> {
            when (type.kind) {
                VarKind.TBOOL -> {
                    when (type.elementCount) {
                        1 -> "bool"
                        2 -> "bvec2"
                        3 -> "bvec3"
                        4 -> "bvec4"
                        else -> errorType(type)
                    }
                }
                VarKind.TBYTE, VarKind.TUNSIGNED_BYTE, VarKind.TSHORT, VarKind.TUNSIGNED_SHORT, VarKind.TFLOAT -> {
                    when (type.elementCount) {
                        1 -> "float"
                        2 -> "vec2"
                        3 -> "vec3"
                        4 -> "vec4"
                        else -> errorType(type)
                    }
                }
                VarKind.TINT -> {
                    when (type.elementCount) {
                        1 -> "int"
                        2 -> "ivec2"
                        3 -> "ivec3"
                        4 -> "ivec4"
                        else -> errorType(type)
                    }
                }
            }
        }
    }

}
