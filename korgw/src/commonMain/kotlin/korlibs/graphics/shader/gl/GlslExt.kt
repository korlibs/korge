package korlibs.graphics.shader.gl

import korlibs.graphics.annotation.KoragExperimental
import korlibs.graphics.shader.FragmentShader
import korlibs.graphics.shader.Program
import korlibs.graphics.shader.Shader
import korlibs.graphics.shader.VertexShader

fun Shader.toNewGlslStringResult(config: GlslConfig): GlslGenerator.Result =
    GlslGenerator(this.type, config).generateResult(this)

fun Shader.toNewGlslString(config: GlslConfig): String =
    toNewGlslStringResult(config).result

// @TODO: For RAW stuff, we might want to support other shader languages, other than GlSl, and let the engine select the proper string?
@KoragExperimental
fun VertexShaderRawGlSl(stm: Program.Stm, glsl: String) = VertexShader(mapOf(GlslGenerator.NAME to glsl), stm)
@KoragExperimental
fun FragmentShaderRawGlSl(stm: Program.Stm, glsl: String) = FragmentShader(mapOf(GlslGenerator.NAME to glsl), stm)

@KoragExperimental
fun VertexShaderRawGlSl(glsl: String, stm: Program.Stm? = null) = VertexShader(mapOf(GlslGenerator.NAME to glsl), stm)
@KoragExperimental
fun FragmentShaderRawGlSl(glsl: String, stm: Program.Stm? = null) = FragmentShader(mapOf(GlslGenerator.NAME to glsl), stm)
