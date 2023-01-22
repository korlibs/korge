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
        VarType.Byte4 -> "char4"
        VarType.Mat2 -> "matrix_float2x2"
        VarType.Mat3 -> "matrix_float3x3"
        VarType.Mat4 -> "matrix_float4x4"
        VarType.Sampler1D -> "sampler"
        VarType.Sampler2D -> "sampler"
        VarType.Sampler3D -> "sampler"
        VarType.SamplerCube -> "sampler"
        else -> {
            when (type.kind) {
                VarKind.TBOOL -> {
                    when (type.elementCount) {
                        1 -> "bool"
                        2 -> "half2"
                        3 -> "half3"
                        4 -> "half4"
                        else -> errorType(type)
                    }
                }
                VarKind.TBYTE, VarKind.TUNSIGNED_BYTE, VarKind.TSHORT, VarKind.TUNSIGNED_SHORT, VarKind.TFLOAT -> {
                    when (type.elementCount) {
                        1 -> "float"
                        2 -> "float2"
                        3 -> "float3"
                        4 -> "float4"
                        else -> errorType(type)
                    }
                }
                VarKind.TINT -> {
                    when (type.elementCount) {
                        1 -> "int"
                        2 -> "int2"
                        3 -> "int3"
                        4 -> "int4"
                        else -> errorType(type)
                    }
                }
            }
        }
    }

}
