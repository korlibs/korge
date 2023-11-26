package korlibs.graphics.gl

import korlibs.datastructure.*
import korlibs.graphics.shader.*
import korlibs.graphics.shader.gl.*
import korlibs.kgl.*
import korlibs.logger.*
import kotlin.native.concurrent.*

internal data class GLProgramInfo(var programId: Int, var vertexId: Int, var fragmentId: Int, val blocks: List<UniformBlock>, val config: GlslConfig) {
    private val blocksByFixedLocation = blocks.associateBy { it.fixedLocation }
    private val maxBlockId = (blocks.maxOfOrNull { it.fixedLocation } ?: -1) + 1
    val uniforms: Array<UniformsRef?> = Array(maxBlockId + 1) { blocksByFixedLocation[it]?.let { UniformsRef(it) } }
    //@Deprecated("This is the only place where AGUniformValues are still used")
    //val cache = AGUniformValues()

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

internal object GLShaderCompiler {
    private val logger = Logger("GLShaderCompiler")

    private fun String.replaceVersion(version: Int) = this.replace("#version 100", "#version $version")

    // @TODO: Prevent leaks if we throw exceptions, we should free resources
    fun programCreate(gl: KmlGl, config: GlslConfig, program: Program, debugName: String?): GLProgramInfo {
        val id = gl.createProgram()

        //println("GL_SHADING_LANGUAGE_VERSION: $glslVersionInt : $glslVersionString")

        val guessedGlSlVersion = gl.versionInt
        val usedGlSlVersion = GlslGenerator.FORCE_GLSL_VERSION ?: guessedGlSlVersion

        if (GlslGenerator.DEBUG_GLSL) {
            logger.trace { "GLSL version: usedGlSlVersion=$usedGlSlVersion, guessed=$guessedGlSlVersion, forced=${GlslGenerator.FORCE_GLSL_VERSION}. used=$usedGlSlVersion" }
        }

        val (
            fragmentShaderId,
            vertexShaderId,
            finalConfig
        ) = createShaderWithConfigs(
            gl, program, debugName,
            listOf(false, true).map { compat -> config.copy(glslVersion = usedGlSlVersion, compatibility = compat) }
        )

        for (attr in program.attributes) {
            val location = attr.fixedLocation
            gl.bindAttribLocation(id, location, attr.name)
        }
        gl.attachShader(id, fragmentShaderId)
        gl.attachShader(id, vertexShaderId)
        gl.linkProgram(id)
        val linkStatus = gl.getProgramiv(id, gl.LINK_STATUS)
        return GLProgramInfo(id, vertexShaderId, fragmentShaderId, program.uniformBlocks, finalConfig)
    }

    fun createShaderWithConfigs(gl: KmlGl, program: Program, debugName: String?, configs: List<GlslConfig>): Triple<Int, Int, GlslConfig> {
        val errors = arrayListOf<Throwable>()
        for (config in configs) {
            try {
                val fragmentString = program.fragment.toNewGlslString(config)
                val vertexString = program.vertex.toNewGlslString(config)
                val fragmentShaderId = createShader(gl, KmlGl.FRAGMENT_SHADER, fragmentString, debugName)
                val vertexShaderId = createShader(gl, KmlGl.VERTEX_SHADER, vertexString, debugName)
                logger.debug {
                    "!!! PROGRAM SUCCESSFULLY COMPILED: config=$config\n$vertexString\n$fragmentString"
                }
                //println("!!! PROGRAM SUCCESSFULLY COMPILED: config=$config\n$vertexString\n$fragmentString" )
                return Triple(fragmentShaderId, vertexShaderId, config)
            } catch (e: AGOpengl.ShaderException) {
                logger.debug { e.stackTraceToString() }
                //e.printStackTrace()

                errors += e
                continue
            }
        }
        throw Exception("Tried several shaders: ${errors.map { it.message }}", errors.last())
    }

    private fun createShader(gl: KmlGl, type: Int, str: String, debugName: String?): Int {
        val shaderId = gl.createShader(type)

        gl.shaderSource(shaderId, str)
        gl.compileShader(shaderId)

        val out = gl.getShaderiv(shaderId, gl.COMPILE_STATUS)
        val errorInt = gl.getError()
        if (out != gl.GTRUE) {
            val error = gl.getShaderInfoLog(shaderId)
            throw AGOpengl.ShaderException(str, error, errorInt, gl, debugName, type, out)
        }
        return shaderId
    }
}

@SharedImmutable
val KmlGl.versionString by Extra.PropertyThis<KmlGl, String> {
    when {
        this.variant.isWebGL -> if (this.variant.version == 1) "1.00" else "3.00"
        else -> getString(SHADING_LANGUAGE_VERSION)
    }
}

@SharedImmutable
val KmlGl.versionInt by Extra.PropertyThis<KmlGl, Int> {
    Regex("(\\d+\\.\\d+)").find(versionString)?.value?.replace(".", "")?.trim()?.toIntOrNull() ?: 100
}
