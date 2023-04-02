package korlibs.graphics.shaders

import korlibs.graphics.*
import korlibs.graphics.shader.*
import korlibs.graphics.shader.gl.*
import kotlin.test.*

class ShadersTest {
	@Test
	fun testGlslGeneration() {
		val vs = VertexShader {
			IF(true.lit) {
                SET(DefaultShaders.t_Temp0, 1.lit * 2.lit)
			} ELSE {
                SET(DefaultShaders.t_Temp0, 3.lit * 4.lit)
			}
		}

		// @TODO: Optimizer phase!
        assertEqualsShader("""
            void main() {
                vec4 temp0;
                if (true) {
                    temp0 = (1 * 2);
                }
                else {
                    temp0 = (3 * 4);
                }
            }
        """.trimIndent(), vs)
	}

    @Test
    fun testGlslGenerationRaw() {
        val vs = VertexShaderRawGlSl("hello")

        assertEquals("hello", vs.toNewGlslString(GlslConfig()))
    }

    val fs by lazy {
        FragmentShader {
            DefaultShaders.apply {
                SET(out, vec4(1.lit, 0.lit, 0.lit, 1.lit))
            }
        }
    }

    @Test
    fun testGlslFragmentGenerationOld() {
        assertEqualsShader("""
            void main() {
                gl_FragColor = vec4(1, 0, 0, 1);
            }
        """.trimIndent(), fs, version = 100)
    }

    @Test
    fun testGlslDirectives() {
        assertEqualsShader("""
            #extension GL_OES_standard_derivatives : enable
            #ifdef GL_ES
            precision mediump float;
            #endif
            void main() {
                gl_FragColor = vec4(1, 0, 0, 1);
            }
        """.trimIndent(), fs, version = 100, stripDirectives = false)
    }

    @Test
    fun testGlslFragmentGenerationNew() {
        assertEqualsShader("""
            void main() {
                gl_FragColor = vec4(1, 0, 0, 1);
            }
        """.trimIndent(), fs, version = 330)
    }

    @Test
    fun testGlslFragmentGenerationNewCustomFunc() {
        assertEqualsShader("""
            float demo(float p0, float p1) {
                return ((p0 + p1) + 2);
            }
            void main() {
                gl_FragColor = vec4(1, 0, 0, demo(0, 1));
            }
        """.trimIndent(), FragmentShader {
            DefaultShaders.apply {
                // This is discarded
                val demo2 by FUNC(Float1, Float1, returns = Float1) { x, y ->
                    RETURN(x + y * 2.lit)
                }
                // Latest function with this name is used
                val demo by FUNC(Float1, Float1, returns = Float1) { x, y ->
                    RETURN(x + y + 2.lit)
                }

                SET(out, vec4(1.lit, 0.lit, 0.lit, demo(0.lit, 1.lit)))
            }
        }, version = 330)
    }

    @Test
    fun testGlslFragmentGenerationNewCustomFuncUpdated() {
        val fragment = FragmentShader {
            // This is discarded
            val demo = FUNC("demo", Float1) {
                val x = ARG("x", Float1)
                val y = ARG("y", Float1)
                RETURN(x + y * 2.lit)
            }
            SET(out, vec4(1.lit, 0.lit, 0.lit, demo(0.lit, 1.lit)))
        }.appending {
            // Latest function with this name is used
            FUNC("demo", Float1) {
                val x = ARG("x", Float1)
                val y = ARG("y", Float1)
                RETURN(x + y + 2.lit)
            }
        }

        assertEqualsShader("""
            float demo(float x, float y) {
                return ((x + y) + 2);
            }
            void main() {
                gl_FragColor = vec4(1, 0, 0, demo(0, 1));
            }
        """.trimIndent(), fragment, version = 330)
    }

    fun assertEqualsShader(expected: String, shader: Shader, version: Int = GlslGenerator.DEFAULT_VERSION, gles: Boolean = false, stripDirectives: Boolean = true) {
        assertEquals(
            expected.trimEnd(),
            shader.toNewGlslStringResult(gles = gles, version = version).result.lines().filter {
                val isDirective = it.startsWith("#") || it.startsWith("precision ")
                if (stripDirectives) !isDirective else true
            }.joinToString("\n").trimEnd().replace("\t", "    ")
        )
    }

    @Test
    fun testGlslEquality() {
        fun genFragment(sub: Int) = FragmentShader {
            DefaultShaders {
                SET(out, vec4(0f.lit, 0f.lit, 0f.lit, 0f.lit))

                for (y in 0 until 3) {
                    for (x in 0 until 3) {
                        SET(out, out + (tex(
                            fragmentCoords + vec2(
                                (x - sub).toFloat().lit,
                                (y - sub).toFloat().lit
                            )
                        )) * u_Weights[x][y])
                    }
                }
            }
        }

        assertEquals(genFragment(1), genFragment(1))
        assertNotEquals(genFragment(1), genFragment(0))
        assertEquals(1, LinkedHashSet<FragmentShader>().apply {
            add(genFragment(1))
            add(genFragment(1))
        }.size)
    }

    //@Test
    //fun testRegression() {
    //    assertEquals(Uniform("test1", VarType.Float1, 1), Uniform("test1", VarType.Float1, 1))
    //    assertNotEquals(Uniform("test1", VarType.Float1, 1), Uniform("test2", VarType.Float1, 1))
    //}

    @Test
    fun testAppendingKeepsType() {
        assertEquals(ShaderType.FRAGMENT, DefaultShaders.FRAGMENT_DEBUG.appending { SET(out, vec4(1f.lit)) }.type)
        assertEquals(ShaderType.VERTEX, DefaultShaders.VERTEX_DEFAULT.appending { SET(out, vec4(1f.lit)) }.type)
    }

    object MBlock : UniformBlock(6) {
        val u_Weights by mat3()
        val u_TextureSize by vec2()
    }

    private val u_Weights = MBlock.u_Weights.uniform
    val u_TextureSize = MBlock.u_TextureSize.uniform
    val Program.Builder.fragmentCoords01 get() = DefaultShaders.v_Tex["xy"]
    val Program.Builder.fragmentCoords get() = fragmentCoords01 * u_TextureSize
    fun Program.Builder.tex(coords: Operand) = texture2D(DefaultShaders.u_Tex, coords / u_TextureSize)
}
