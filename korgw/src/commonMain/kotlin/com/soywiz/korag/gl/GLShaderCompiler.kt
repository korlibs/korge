package com.soywiz.korag.gl

import com.soywiz.kds.*
import com.soywiz.kgl.*
import com.soywiz.klogger.*
import com.soywiz.kmem.*
import com.soywiz.korag.shader.*
import com.soywiz.korag.shader.gl.*
import kotlin.native.concurrent.*

data class GLProgramInfo(var programId: Int, var vertexId: Int, var fragmentId: Int) {
    val cachedAttribLocations = FastStringMap<Int>()
    val cachedUniformLocations = FastStringMap<Int>()

    fun getAttribLocation(gl: KmlGl, name: String): Int =
        cachedAttribLocations.getOrPut(name) { gl.getAttribLocation(programId, name) }

    fun getUniformLocation(gl: KmlGl, name: String): Int =
        cachedUniformLocations.getOrPut(name) { gl.getUniformLocation(programId, name) }

    fun use(gl: KmlGl) {
        gl.useProgram(programId)
    }

    fun delete(gl: KmlGl) {
        if (vertexId != 0) gl.deleteShader(vertexId)
        if (fragmentId != 0) gl.deleteShader(fragmentId)
        if (programId != 0) gl.deleteProgram(programId)
        vertexId = 0
        fragmentId = 0
        programId = 0
    }
}

@ThreadLocal
private val GLShaderCompiler_tempBuffer1 = FBuffer(4)

object GLShaderCompiler {
    private inline val tempBuffer1 get() = GLShaderCompiler_tempBuffer1

    private fun String.replaceVersion(version: Int) = this.replace("#version 100", "#version $version")

    // @TODO: Prevent leaks if we throw exceptions, we should free resources
    fun programCreate(gl: KmlGl, config: GlslConfig, program: Program, glSlVersion: Int? = null): GLProgramInfo {
        val id = gl.createProgram()

        //println("GL_SHADING_LANGUAGE_VERSION: $glslVersionInt : $glslVersionString")

        val guessedGlSlVersion = glSlVersion ?: gl.versionInt
        val usedGlSlVersion = GlslGenerator.FORCE_GLSL_VERSION?.toIntOrNull()
            ?: when (guessedGlSlVersion) {
                460 -> 460
                in 300..450 -> 100
                else -> guessedGlSlVersion
            }

        if (GlslGenerator.DEBUG_GLSL) {
            Console.trace("GLSL version: requested=$glSlVersion, guessed=$guessedGlSlVersion, forced=${GlslGenerator.FORCE_GLSL_VERSION}. used=$usedGlSlVersion")
        }

        val fragmentShaderId = createShaderCompat(gl, gl.FRAGMENT_SHADER) { compatibility ->
            program.fragment.toNewGlslString(config.copy(version = usedGlSlVersion, compatibility = compatibility))
        }
        val vertexShaderId = createShaderCompat(gl, gl.VERTEX_SHADER) { compatibility ->
            program.vertex.toNewGlslString(config.copy(version = usedGlSlVersion, compatibility = compatibility))
        }
        gl.attachShader(id, fragmentShaderId)
        gl.attachShader(id, vertexShaderId)
        gl.linkProgram(id)
        tempBuffer1.setInt(0, 0)
        gl.getProgramiv(id, gl.LINK_STATUS, tempBuffer1)
        return GLProgramInfo(id, vertexShaderId, fragmentShaderId)
    }

    private fun createShaderCompat(gl: KmlGl, config: GlslConfig, type: Int, shader: Shader): Int {
        return createShaderCompat(gl, type) { compatibility ->
            shader.toNewGlslString(config.copy(compatibility = compatibility))
        }
    }

    private inline fun createShaderCompat(gl: KmlGl, type: Int, gen: (compat: Boolean) -> String): Int {
        return try {
            createShader(gl, type, gen(true))
        } catch (e: AGOpengl.ShaderException) {
            createShader(gl, type, gen(false))
        }
    }

    private fun createShader(gl: KmlGl, type: Int, str: String): Int {
        val shaderId = gl.createShader(type)

        gl.shaderSource(shaderId, str)
        gl.compileShader(shaderId)

        val out = gl.getShaderiv(shaderId, gl.COMPILE_STATUS)
        val errorInt = gl.getError()
        if (out != gl.GTRUE) {
            val error = gl.getShaderInfoLog(shaderId)
            throw AGOpengl.ShaderException(str, error, errorInt, gl)
        }
        return shaderId
    }
}
