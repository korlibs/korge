package korlibs.korge.view.filter

import korlibs.graphics.*
import korlibs.graphics.shader.*
import korlibs.korge.render.*
import korlibs.korge.view.property.*

// Ideas from: http://alex-charlton.com/posts/Dithering_on_the_GPU/
class DitheringFilter(
    levels: Double = 4.0
) : ShaderFilter() {
    object DitherUB : UniformBlock(fixedLocation = 5) {
        val u_Levels by float()
    }

    companion object : BaseProgramProvider() {
        object DitheringTools : Program.Builder() {
            // @TODO: Support constant arrays, or pass array literals as uniforms
            val indexMatrix4x4 = intArrayOf(
                0,  8,  2,  10,
                12, 4,  14, 6,
                3,  11, 1,  9,
                15, 7,  13, 5
            )
            val indexMatrix8x8 = intArrayOf(
                0,  32, 8,  40, 2,  34, 10, 42,
                48, 16, 56, 24, 50, 18, 58, 26,
                12, 44, 4,  36, 14, 46, 6,  38,
                60, 28, 52, 20, 62, 30, 54, 22,
                3,  35, 11, 43, 1,  33, 9,  41,
                51, 19, 59, 27, 49, 17, 57, 25,
                15, 47, 7,  39, 13, 45, 5,  37,
                63, 31, 55, 23, 61, 29, 53, 21
            )
            val indexValue by FUNC(Float2, returns = Float1) { coords ->
                val index = TEMP(Float1)
                val matrix = indexMatrix8x8
                //val matrix = indexMatrix4x4
                val width = kotlin.math.sqrt(matrix.size.toDouble()).toInt()
                val x = int(mod(coords.x, width.toFloat().lit))
                val y = int(mod(coords.y, width.toFloat().lit))
                SET(index, float(x + y * width.lit))
                IF_ELSE_BINARY_LOOKUP(index, 0, matrix.size - 1) {
                    RETURN((matrix[it].toFloat() / matrix.size.toFloat()).lit)
                }
                RETURN(0f.lit)
            }
        }

        override val fragment: FragmentShader = FragmentShaderDefault {
            val COL = TEMP(Float4)
            val COL1 = TEMP(Float4)
            val COL2 = TEMP(Float4)
            val DIST1 = TEMP(Float4)
            val DIST3 = TEMP(Float4)
            val INDEX1 = TEMP(Float1)
            val STEPS = DitherUB.u_Levels
            val hueDiff = TEMP(Float4)
            SET(COL, tex(fragmentCoords))
            SET(COL1, vec4(floor(COL * STEPS)) / STEPS)
            SET(COL2, vec4(ceil(COL * STEPS)) / STEPS)
            SET(DIST1, abs(COL1 - COL))
            SET(DIST3, abs(COL2 - COL1))
            SET(INDEX1, DitheringTools.indexValue(fragmentCoords))
            SET(hueDiff , DIST1 / DIST3)
            SET(out, vec4(
                TERNARY(hueDiff.x lt INDEX1, COL1.x, COL2.x),
                TERNARY(hueDiff.y lt INDEX1, COL1.y, COL2.y),
                TERNARY(hueDiff.z lt INDEX1, COL1.z, COL2.z),
                TERNARY(hueDiff.w lt INDEX1, COL1.w, COL2.w),
            ))
        }
    }
    override val programProvider: ProgramProvider get() = DitheringFilter

    @ViewProperty
    var levels: Double = levels

    override fun updateUniforms(ctx: RenderContext, filterScale: Double) {
        ctx[DitherUB].push {
            it[u_Levels] = levels.toFloat()
        }
    }
}
