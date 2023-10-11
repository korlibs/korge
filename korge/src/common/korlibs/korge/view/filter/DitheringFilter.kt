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
            /*
            val random by FUNC(Float2, returns = Float1) { coords ->
                RETURN(fract(sin(dot(coords["xy"], vec2(12.9898f.lit, 78.233f.lit))) * 43758.5453f.lit))
            }

            val Epsilon = 1e-10f

            val HUEtoRGB by FUNC(Float1, returns = Float3) { H ->
                val R = abs(H * 6f.lit - 3f.lit) - 1f.lit
                val G = 2f.lit - abs(H * 6f.lit - 2f.lit)
                val B = 2f.lit - abs(H * 6f.lit - 4f.lit)
                RETURN(clamp(vec3(R, G, B), 0f.lit, 1f.lit))
            }

            // https://www.chilliant.com/rgb2hsv.html
            val RGBtoHCV by FUNC(Float3, returns = Float3) { RGB ->
                // Based on work by Sam Hocevar and Emil Persson
                val P = TEMP(VarType.Float4)
                val Q = TEMP(VarType.Float4)
                val C = TEMP(VarType.Float1)
                val H = TEMP(VarType.Float1)
                SET(P, TERNARY(RGB.g lt RGB.b, vec4(RGB["bg"], -1.0f.lit, 2.0f.lit/3.0f.lit), vec4(RGB["gb"], 0.0f.lit, -1.0f.lit/3.0f.lit)))
                SET(Q, TERNARY(RGB.r lt P.x, vec4(P["xyw"], RGB.r), vec4(RGB.r, P["yzx"])))
                SET(C, Q.x - min(Q.w, Q.y))
                SET(H, abs((Q.w - Q.y) / (6f.lit * C + Epsilon) + Q.z))
                RETURN(vec3(H, C, Q.x))
            }

            val RGBtoHSL by FUNC(Float3, returns = Float3) { RGB ->
                val HCV = TEMP(Float3)
                SET(HCV, RGBtoHCV(RGB))
                val L = HCV.z - HCV.y * 0.5f.lit
                val S = HCV.y / (1f.lit - abs(L * 2f.lit - 1f.lit) + Epsilon);
                RETURN(vec3(HCV.x, S, L))
            }

            val HSLtoRGB by FUNC(Float3, returns = Float3) { HSL ->
                val RGB = TEMP(Float3)
                val C = TEMP(Float1)
                SET(RGB, HUEtoRGB(HSL.x))
                SET(C, (1f.lit - abs(2f.lit * HSL.z - 1f.lit)) * HSL.y)
                RETURN((RGB - 0.5f.lit) * C + HSL.z)
            }
            */

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
                val width = 8
                val matrix = indexMatrix8x8
                //val width = 4
                //val matrix = indexMatrix4x4
                val x = int(mod(coords.x, width.toFloat().lit))
                val y = int(mod(coords.y, width.toFloat().lit))
                SET(index, float(x + y * width.lit))
                IF_ELSE_BINARY_LOOKUP(index, 0, matrix.size - 1) {
                    RETURN((matrix[it].toFloat() / matrix.size.toFloat()).lit)
                }
                RETURN(0f.lit)
            }


            /*
            val lightnessSteps = 4f

            val lightnessStep by FUNC(Float1, returns = Float1) { l ->
                /* Quantize the lightness to one of `lightnessSteps` values */
                RETURN(floor((0.5f.lit + l * lightnessSteps)) / lightnessSteps)
            }

            val dither by FUNC(Float3, returns = Float3) { color ->
                vec3 hsl = rgbToHsl(color);

                vec3 cs[2] = closestColors(hsl.x);
                vec3 c1 = cs[0];
                vec3 c2 = cs[1];
                float d = indexValue();
                float hueDiff = hueDistance(hsl.x, c1.x) / hueDistance(c2.x, c1.x);

                float l1 = lightnessStep(max((hsl.z - 0.125), 0.0));
                float l2 = lightnessStep(min((hsl.z + 0.124), 1.0));
                float lightnessDiff = (hsl.z - l1) / (l2 - l1);

                vec3 resultColor = (hueDiff < d) ? c1 : c2;
                resultColor.z = (lightnessDiff < d) ? l1 : l2;
                return hslToRgb(resultColor);
            }
            */
        }

        override val fragment: FragmentShader = FragmentShaderDefault {
            //SET(out, tex(fragmentCoords))
            //SET(out, mix(out, (ColorMatrixFilter.ColorMatrixUB.u_ColorMatrix * out), ColorMatrixFilter.ColorMatrixUB.u_BlendRatio))
            //BatchBuilder2D.DO_INPUT_OUTPUT(this, out)
            val NOISE_GRANULARITY = 0.5f/255.0f;
            val COL = TEMP(Float4)
            val COL1 = TEMP(Float4)
            val COL2 = TEMP(Float4)
            val DIST1 = TEMP(Float4)
            //val DIST2 = TEMP(Float4)
            val DIST3 = TEMP(Float4)
            val INDEX1 = TEMP(Float1)
            val STEPS = DitherUB.u_Levels
            SET(COL, tex(fragmentCoords))
            SET(COL1, vec4(floor(COL * STEPS)) / STEPS)
            SET(COL2, vec4(ceil(COL * STEPS)) / STEPS)
            SET(DIST1, abs(COL1 - COL))
            //SET(DIST2, abs(COL2 - COL))
            SET(DIST3, abs(COL2 - COL1))
            SET(INDEX1, DitheringTools.indexValue(fragmentCoords))
            //val COL2 = TEMP(Float4)


            val hueDiff = DIST1 / DIST3
            //val coordinates = tex(fragmentCoords)
            //val fragmentColor = mix(0.05f.lit, 0.35f.lit, 1.0f.lit - coordinates.y)
            //val fragmentColor2 = fragmentColor + mix(-NOISE_GRANULARITY.lit, NOISE_GRANULARITY.lit, DitheringTools.random(coordinates.x));


            //SET(out, TERNARY(DitheringTools.random(fragmentCoords01) gt 0.5f.lit, COL1, COL2))
            //SET(out, TERNARY((DIST1 lt DIST2), COL1, COL2))
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
