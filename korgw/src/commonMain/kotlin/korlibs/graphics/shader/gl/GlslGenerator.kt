package korlibs.graphics.shader.gl

import korlibs.graphics.*
import korlibs.graphics.gl.*
import korlibs.graphics.shader.*
import korlibs.io.lang.*
import korlibs.io.util.*
import korlibs.logger.*

data class GlslConfig constructor(
    val variant: GLVariant,
    val features: AGFeatures,
    val glslVersion: Int = -1,
    val compatibility: Boolean = true,
) {
    fun getFunctionName(name: String): String {
        if (compatibility) return name

        return when (name) {
            "texture2D" -> "texture"
            else -> name
        }
    }

    //val newGlSlVersion: Boolean get() = version > 120
    val newGlSlVersion: Boolean get() {
        if (variant.isES) return glslVersion >= 300
        return !compatibility
    }

    val useUniformBlocks: Boolean get() = ENABLE_UNIFORM_BLOCKS && newGlSlVersion && features.isUniformBuffersSupported
    //val useUniformBlocks: Boolean get() = false

    companion object {
        val NAME: String = "GLSL"
        val DEFAULT_VERSION: Int = 100
        val FRAGCOLOR: String = "fragColor"
        val GL_FRAGCOLOR: String = "gl_FragColor"
        val FORCE_GLSL_VERSION: Int? get() = Environment["FORCE_GLSL_VERSION"]?.replace(".", "")?.toIntOrNull()
        val DEBUG_GLSL: Boolean get() = Environment["DEBUG_GLSL"] == "true"
    }

    val IN: String get() = if (newGlSlVersion) "in" else "attribute"
    val OUT: String get() = if (newGlSlVersion) "out" else "varying"
    val UNIFORM: String get() = "uniform"
    val gl_FragColor: String get() = if (newGlSlVersion) FRAGCOLOR else GL_FRAGCOLOR

}

class GlobalsProgramVisitor : Program.Visitor<Unit>(Unit) {
    val attributes = LinkedHashSet<Attribute>()
    val varyings = LinkedHashSet<Varying>()
    val uniforms = LinkedHashSet<Uniform>()
    val typedUniforms = LinkedHashSet<TypedUniform<*>>()
    val uniformBlocks = LinkedHashSet<UniformBlock>()
    val samplers = LinkedHashSet<Sampler>()
    val funcRefs = LinkedHashSet<String>()

    override fun visit(attribute: Attribute) {
        attributes += attribute
    }

    override fun visit(varying: Varying) {
        varyings += varying
    }

    override fun visit(uniform: Uniform) {
        uniforms += uniform
        uniform.typedUniform?.let { typedUniforms += it }
        uniformBlocks += uniform.typedUniform.block
    }

    override fun visit(typedUniform: TypedUniform<*>) {
        uniforms += typedUniform.uniform
        typedUniforms += typedUniform
        uniformBlocks += typedUniform.block
    }

    override fun visit(sampler: Sampler) {
        samplers += sampler
    }

    override fun visit(func: Program.CustomFunc) {
        //println("VISITED: $func")
        funcRefs += func.ref.name
    }
}

class GlslGenerator constructor(
    val kind: ShaderType,
    override val config: GlslConfig
) : BaseGlslGenerator {
    companion object {
        private val logger = Logger("GlslGenerator")

        val NAME: String get() = GlslConfig.NAME
        val DEFAULT_VERSION: Int get() = GlslConfig.DEFAULT_VERSION
        val FORCE_GLSL_VERSION: Int? get() = GlslConfig.FORCE_GLSL_VERSION
        val DEBUG_GLSL: Boolean get() = GlslConfig.DEBUG_GLSL
    }

    val compatibility: Boolean get() = config.compatibility

    data class Result(
        val generator: GlslGenerator,
        val result: String,
        val attributes: List<Attribute>,
        val uniforms: List<Uniform>,
        val varyings: List<Varying>
    )

    fun generateResult(shader: Shader): Result = generateResult(shader.stm, shader.functions)

    fun generateResult(root: Program.Stm, funcs: List<FuncDecl>): Result {
        val types = GlobalsProgramVisitor()

        if (kind == ShaderType.FRAGMENT && config.newGlSlVersion) {
            types.varyings.add(Varying(config.gl_FragColor, VarType.Float4))
        }

        val mainFunc = FuncDecl("main", VarType.TVOID, listOf(), root)
        types.visit(mainFunc)

        //println("types.funcRefs=${types.funcRefs}")
        val customFuncs = funcs.filter { it.ref.name in types.funcRefs }.reversed().distinctBy { it.ref.name }
        //TODO: is it relevant to visit mainFunc for each existing function ?
        for (func in funcs) types.visit(mainFunc)

        val allFuncs = customFuncs + listOf(mainFunc)

        // NOT THE CASE.
        // You can define inputs for a Fragment shader, and attributes are used to define inputs
        //if (kind == ShaderType.FRAGMENT && attributes.isNotEmpty()) {
        //    throw RuntimeException("Can't use attributes in fragment shader")
        //}

        val result = Indenter {
            if (!config.compatibility) {
                val suffix = if (config.variant.isES) " es" else ""
                line("#version ${config.glslVersion}$suffix")
            }
            //line("/* glslVersion=${config.glslVersion}, newGlSlVersion=${config.newGlSlVersion}, compatibility=${config.compatibility} */")
            line("#extension GL_OES_standard_derivatives : enable")
            line("#ifdef GL_ES")
            line("precision mediump float;")
            line("#endif")

            //println("gles=$gles, compatibility=$compatibility")

            for (it in types.attributes) {
                // https://www.khronos.org/opengl/wiki/Layout_Qualifier_(GLSL)
                val layout = when {
                    config.newGlSlVersion && config.glslVersion >= 410 -> "layout(location = ${it.fixedLocation}) "
                    else -> ""
                }

                line("$layout$IN ${precToString(it.precision)}${typeToString(it.type)} ${it.name}${it.arrayDecl};")
            }
            for (it in types.samplers) {
                // https://www.khronos.org/opengl/wiki/Layout_Qualifier_(GLSL)
                val layout = when {
                    config.newGlSlVersion -> ""
                    else -> ""
                }

                line("$layout$UNIFORM ${precToString(it.precision)}${typeToString(it.type)} ${it.name}${it.arrayDecl};")
            }

            if (config.useUniformBlocks) {
                for (block in types.uniformBlocks) {
                    //line("layout(binding = ${block.fixedLocation}) layout(std140) uniform ${block.name} {")
                    line("layout(std140) uniform ${block.name} {")
                    for (uniform in block.uniforms) {
                        line("  ${precToString(uniform.precision)}${typeToString(uniform.type)} ${uniform.name}${uniform.arrayDecl};")
                    }
                    line("};")
                }
            } else {
                for (it in types.uniforms) line("$UNIFORM ${precToString(it.precision)}${typeToString(it.type)} ${it.name}${it.arrayDecl};")
            }

            for ((index, it) in types.varyings.sortedBy { it.name }.withIndex()) {
                if (it is Output) continue

                val INOUT = when {
                    !config.newGlSlVersion -> OUT
                    it.name == config.gl_FragColor && kind == ShaderType.FRAGMENT -> OUT
                    kind == ShaderType.VERTEX -> OUT
                    else -> IN
                }

                line("$INOUT ${precToString(it.precision)}${typeToString(it.type)} ${it.name};")
            }

            for (func in allFuncs) {
                val gen = GlslBodyGenerator(kind, config)
                gen.visit(func)

                val argsStrings = func.args.map { "${typeToString(it.second)} ${it.first}" }

                line("${typeToString(func.rettype)} ${func.name}(${argsStrings.joinToString(", ")})") {
                    for (temp in gen.temps) {
                        line(precToString(temp.precision) + typeToString(temp.type) + " " + temp.name + ";")
                    }
                    line(gen.programIndenter)
                }
            }
        }.toString().also {
            if (GlslConfig.DEBUG_GLSL) {
                logger.info { "GlSlGenerator.version: ${config.glslVersion}" }
                logger.debug { "GlSlGenerator:\n$it" }
            }
        }
        return Result(
            this, if (root is Program.Stm.Raw) root.string(GlslConfig.NAME) else result,
            attributes = types.attributes.toList(),
            uniforms = types.uniforms.toList(),
            varyings = types.varyings.toList()
        )
    }

	fun generate(root: Program.Stm, funcs: List<FuncDecl>): String = generateResult(root, funcs).result
    fun generate(root: Shader): String = generate(root.stm, root.functions)

}

class GlslBodyGenerator(
    val kind: ShaderType,
    override val config: GlslConfig
) : Program.Visitor<String>(""), BaseGlslGenerator {
    val temps = LinkedHashSet<Temp>()
    val programIndenter = Indenter()

    override fun visit(stms: Program.Stm.Stms) {
        //programIndenter.line("") {
        for (stm in stms.stms) {
            visit(stm)
        }
        //}
    }

    override fun visit(stm: Program.Stm.Set) {
        programIndenter.line("${visit(stm.to)} = ${visit(stm.from)};")
    }

    override fun visit(stm: Program.Stm.Discard) {
        programIndenter.line("discard;")
    }

    override fun visit(stm: Program.Stm.Break) {
        programIndenter.line("break;")
    }

    override fun visit(stm: Program.Stm.Continue) {
        programIndenter.line("continue;")
    }

    override fun visit(stm: Program.Stm.Return) {
        val result = stm.result
        if (result != null) {
            programIndenter.line("return ${visit(result)};")
        } else {
            programIndenter.line("return;")
        }
    }

    override fun visit(operand: Program.Vector): String =
        typeToString(operand.type) + "(" + operand.ops.joinToString(", ") { visit(it) } + ")"

    override fun visit(operand: Program.Unop): String = "(" + operand.op + "("  + visit(operand.right) + ")" + ")"
    override fun visit(operand: Program.Binop): String = "(" + visit(operand.left) + " " + operand.op + " " + visit(operand.right) + ")"
    override fun visit(func: Program.BaseFunc): String {

        return config.getFunctionName(func.name) + "(" + func.ops.joinToString(", ") { visit(it) } + ")"
    }
    override fun visit(ternary: Program.Ternary): String = "((${visit(ternary.cond)}) ? (${visit(ternary.otrue)}) : (${visit(ternary.ofalse)}))"

    override fun visit(stm: Program.Stm.If) {
        programIndenter.apply {
            line("if (${visit(stm.cond)})") {
                visit(stm.tbody)
            }
            if (stm.fbody != null) {
                line("else") {
                    visit(stm.fbody!!)
                }
            }
        }
    }

    override fun visit(stm: Program.Stm.ForSimple) {
        programIndenter.apply {
            val varType = typeToString(stm.loopVar.type)
            val loopVar = visit(stm.loopVar)
            val min = visit(stm.min)
            val maxExclusive = visit(stm.maxExclusive)
            line("for ($varType $loopVar = ($min); $loopVar < ($maxExclusive); $loopVar++)") {
                visit(stm.body)
            }
        }
    }

    override fun visit(stm: Program.Stm.Raw) {
        programIndenter.apply {
            line(stm.string(GlslConfig.NAME))
        }
    }

    override fun visit(operand: Variable): String {
        super.visit(operand)
        return when (operand) {
            is Output -> when (kind) {
                ShaderType.VERTEX -> "gl_Position"
                ShaderType.FRAGMENT -> config.gl_FragColor
            }
            else -> operand.name
        }
    }

    override fun visit(temp: Temp): String {
        temps += temp
        return super.visit(temp)
    }

    override fun visit(output: Output): String {
        return super.visit(output)
    }

    override fun visit(operand: Program.IntLiteral): String = "${operand.value}"

    override fun visit(operand: Program.FloatLiteral): String {
        val str = "${operand.value}"
        return if (str.contains('.')) str else "$str.0"
    }

    override fun visit(operand: Program.BoolLiteral): String = "${operand.value}"
    override fun visit(operand: Program.Swizzle): String = visit(operand.left) + "." + operand.swizzle
    override fun visit(operand: Program.ArrayAccess): String = visit(operand.left) + "[" + visit(operand.index) + "]"
}

interface BaseGlslGenerator {
    val config: GlslConfig

    private fun errorType(type: VarType): Nothing = invalidOp("Don't know how to serialize type $type")

    fun precToString(prec: Precision) = when {
        !config.variant.isES -> ""
        else -> when (prec) {
            Precision.DEFAULT -> ""
            Precision.LOW -> "lowp "
            Precision.MEDIUM -> "mediump "
            Precision.HIGH -> "highp "
        }
    }

    fun typeToString(type: VarType) = when (type) {
        VarType.TVOID -> "void"
        VarType.Byte4 -> "vec4"
        VarType.Mat2 -> "mat2"
        VarType.Mat3 -> "mat3"
        VarType.Mat4 -> "mat4"
        VarType.Sampler1D -> "sampler1D"
        VarType.Sampler2D -> "sampler2D"
        //VarType.Sampler2D -> if (config.programConfig.externalTextureSampler) "samplerExternalOES" else "sampler2D"
        VarType.Sampler3D -> "sampler3D"
        VarType.SamplerCube -> "samplerCube"
        else -> {
            when (type.kind) {
                VarKind.TBOOL -> {
                    when (type.elementCount) {
                        1 -> "bool"
                        2 -> "bvec2"
                        3 -> "bvec3"
                        4 -> "bvec4"
                        else -> errorType(type)
                    }
                }
                VarKind.TBYTE, VarKind.TUNSIGNED_BYTE, VarKind.TSHORT, VarKind.TUNSIGNED_SHORT, VarKind.TFLOAT -> {
                    when (type.elementCount) {
                        1 -> "float"
                        2 -> "vec2"
                        3 -> "vec3"
                        4 -> "vec4"
                        else -> errorType(type)
                    }
                }
                VarKind.TINT -> {
                    when (type.elementCount) {
                        1 -> "int"
                        2 -> "ivec2"
                        3 -> "ivec3"
                        4 -> "ivec4"
                        else -> errorType(type)
                    }
                }
            }
        }
    }

    val Variable.arrayDecl get() = if (arrayCount != 1) "[$arrayCount]" else ""

    val IN: String get() = config.IN
    val OUT: String get() = config.OUT
    val UNIFORM: String get() = config.UNIFORM
    val gl_FragColor: String get() = config.gl_FragColor
}
