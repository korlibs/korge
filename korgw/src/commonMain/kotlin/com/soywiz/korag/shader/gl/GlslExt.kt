package com.soywiz.korag.shader.gl

import com.soywiz.korag.shader.*

fun Shader.toNewGlslStringResult(gles: Boolean = true, version: Int = GlslGenerator.DEFAULT_VERSION, compatibility: Boolean = true, android: Boolean = false) =
    toNewGlslStringResult(GlslConfig(gles = gles, version = version, compatibility = compatibility, android = android))

fun Shader.toNewGlslStringResult(config: GlslConfig) =
    GlslGenerator(this.type, config).generateResult(this.stm)
