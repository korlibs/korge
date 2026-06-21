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
                val matrix = indexMatrix8x8
                //val matrix = indexMatrix4x4
                val width = kotlin.math.sqrt(matrix.size.toDouble()).toInt()
                val x = int(mod(coords.x, width.toFloat().lit))
                val y = int(mod(coords.y, width.toFloat().lit))
                val index = TEMP(float(x + y * width.lit))
                IF_ELSE_BINARY_LOOKUP(index, 0, matrix.size - 1) {
                    RETURN((matrix[it].toFloat() / matrix.size.toFloat()).lit)
                }
                RETURN(0f.lit)
            }
        }

        override val fragment: FragmentShader = FragmentShaderDefault {
            val steps = DitherUB.u_Levels

            val col = TEMP(tex(fragmentCoords))
            val col1 = TEMP(vec4(floor(col * steps)) / steps)
            val col2 = TEMP(vec4(ceil(col * steps)) / steps)

            val dist1 = TEMP(abs(col1 - col))
            val dist3 = TEMP(abs(col2 - col1))

            val index1 = TEMP(DitheringTools.indexValue(fragmentCoords))
            val hueDiff = TEMP(dist1 / dist3)

            SET(out, vec4(
                TERNARY(hueDiff.x lt index1, col1.x, col2.x),
                TERNARY(hueDiff.y lt index1, col1.y, col2.y),
                TERNARY(hueDiff.z lt index1, col1.z, col2.z),
                TERNARY(hueDiff.w lt index1, col1.w, col2.w),
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
