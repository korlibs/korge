package com.soywiz.korag.shader.gl

import com.soywiz.korag.annotation.KoragExperimental
import com.soywiz.korag.shader.*

fun Shader.toNewGlslStringResult(gles: Boolean = true, version: Int = GlslGenerator.DEFAULT_VERSION, compatibility: Boolean = true, android: Boolean = false) =
    toNewGlslStringResult(GlslConfig(gles = gles, version = version, compatibility = compatibility, android = android))

fun Shader.toNewGlslStringResult(config: GlslConfig): GlslGenerator.Result =
    GlslGenerator(this.type, config).generateResult(this.stm)

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

