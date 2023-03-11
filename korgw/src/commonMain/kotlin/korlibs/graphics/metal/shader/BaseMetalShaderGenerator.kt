package korlibs.graphics.metal.shader

import korlibs.graphics.shader.*
import korlibs.io.lang.*


internal interface BaseMetalShaderGenerator {

    private fun errorType(type: VarType): Nothing = invalidOp("Don't know how to serialize type $type")

    fun precitionToString(precision: Precision) = when (precision) {
            Precision.DEFAULT -> ""
            Precision.LOW -> "half "
            Precision.MEDIUM, Precision.HIGH -> "float "
    }

    fun typeToString(type: VarType) = when (type) {
        VarType.TVOID -> "void"
        VarType.Byte4 -> "uchar4"
        VarType.Mat2 -> "float2x2"
        VarType.Mat3 -> "float3x3"
        VarType.Mat4 -> "float4x4"
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
