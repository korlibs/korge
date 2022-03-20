package com.soywiz.korag.shader.gl

import com.soywiz.klogger.*
import com.soywiz.korag.shader.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.util.*

data class GlslConfig(
    val gles: Boolean = true,
    val version: Int = DEFAULT_VERSION,
    val compatibility: Boolean = true,
    val android: Boolean = false,
    val programConfig: ProgramConfig = ProgramConfig.DEFAULT
) {
    //val newGlSlVersion: Boolean get() = version > 120
    val newGlSlVersion: Boolean get() = false

    companion object {
        val NAME: String = "GLSL"
        val DEFAULT_VERSION: Int = 100
        val FRAGCOLOR: String = "fragColor"
        val GL_FRAGCOLOR: String = "gl_FragColor"
        val FORCE_GLSL_VERSION: String? get() = Environment["FORCE_GLSL_VERSION"]
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
    val funcRefs = LinkedHashSet<Program.FuncRef>()

    override fun visit(attribute: Attribute) {
        attributes += attribute
    }

    override fun visit(varying: Varying) {
        varyings += varying
    }

    override fun visit(uniform: Uniform) {
        uniforms += uniform
    }

    override fun visit(func: Program.CustomFunc) {
        //println("VISITED: $func")
        funcRefs += func.ref
    }
}

class GlslGenerator constructor(
    val kind: ShaderType,
    override val config: GlslConfig = GlslConfig()
) : BaseGlslGenerator {
    companion object {
        val NAME: String get() = GlslConfig.NAME
        val DEFAULT_VERSION: Int get() = GlslConfig.DEFAULT_VERSION
        val FORCE_GLSL_VERSION: String? get() = GlslConfig.FORCE_GLSL_VERSION
        val DEBUG_GLSL: Boolean get() = GlslConfig.DEBUG_GLSL
    }

    constructor(
        kind: ShaderType,
        gles: Boolean = true,
        version: Int = GlslConfig.DEFAULT_VERSION,
        compatibility: Boolean = true
    ) : this(kind, GlslConfig(gles, version, compatibility))

    val gles: Boolean get() = config.gles
    val version: Int get() = config.version
    val compatibility: Boolean get() = config.compatibility
    val android: Boolean get() = config.android


    data class Result(
        val generator: GlslGenerator,
        val result: String,
        val attributes: List<Attribute>,
        val uniforms: List<Uniform>,
        val varyings: List<Varying>
    )

    fun generateResult(shader: Shader): Result = generateResult(shader.stm, shader.funcs)

    fun generateResult(root: Program.Stm, funcs: List<FuncDecl>): Result {
        val types = GlobalsProgramVisitor()

        if (kind == ShaderType.FRAGMENT && config.newGlSlVersion) {
            types.varyings.add(Varying(config.gl_FragColor, VarType.Float4))
        }

        val mainFunc = FuncDecl("main", VarType.TVOID, listOf(), root)
        types.visit(mainFunc)

        //println("types.funcRefs=${types.funcRefs}")
        val customFuncs = funcs.filter { it.ref in types.funcRefs }.reversed().distinctBy { it.ref.name }
        for (func in funcs) types.visit(mainFunc)

        val allFuncs = customFuncs + listOf(mainFunc)

        // NOT THE CASE.
        // You can define inputs for a Fragment shader, and attributes are used to define inputs
        //if (kind == ShaderType.FRAGMENT && attributes.isNotEmpty()) {
        //    throw RuntimeException("Can't use attributes in fragment shader")
        //}

        val result = Indenter {
            if (gles) {
                if (!android) {
                    if (compatibility) {
                        line("#version $version compatibility")
                    } else {
                        line("#version $version")
                    }
                }
                if (config.programConfig.externalTextureSampler) {
                    line("#extension GL_OES_EGL_image_external : require")
                }
                line("#ifdef GL_ES")
                indent {
                    line("precision highp float;")
                    line("precision highp int;")
                    line("precision lowp sampler2D;")
                    line("precision lowp samplerCube;")
                }
                line("#else")
                indent {
                    line("  #define highp ")
                    line("  #define mediump ")
                    line("  #define lowp ")
                }
                //indent {
                //    line("precision highp float;")
                //    line("precision highp int;")
                //}
                line("#endif")
                //line("precision highp float;")
                //line("precision highp int;")
                //line("precision lowp sampler2D;")
                //line("precision lowp samplerCube;")
            }

            for (it in types.attributes) line("$IN ${precToString(it.precision)}${typeToString(it.type)} ${it.name}${it.arrayDecl};")
            for (it in types.uniforms) line("$UNIFORM ${precToString(it.precision)}${typeToString(it.type)} ${it.name}${it.arrayDecl};")
            for (it in types.varyings) {
                if (it is Output) continue
                if (config.newGlSlVersion && it.name == config.gl_FragColor) {
                    line("layout(location=0) $OUT ${precToString(it.precision)}${typeToString(it.type)} ${it.name};")
                } else {
                    line("$OUT ${precToString(it.precision)}${typeToString(it.type)} ${it.name};")
                }
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
                Console.info("GlSlGenerator.version: $version")
                Console.debug("GlSlGenerator:\n$it")
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
    fun generate(root: Shader): String = generate(root.stm, root.funcs)

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
        programIndenter.line("return ${visit(stm.result)};")
    }

    override fun visit(operand: Program.Vector): String =
        typeToString(operand.type) + "(" + operand.ops.joinToString(", ") { visit(it) } + ")"

    override fun visit(operand: Program.Unop): String = "(" + operand.op + "("  + visit(operand.right) + ")" + ")"
    override fun visit(operand: Program.Binop): String = "(" + visit(operand.left) + " " + operand.op + " " + visit(operand.right) + ")"
    override fun visit(func: Program.BaseFunc): String = func.name + "(" + func.ops.joinToString(", ") { visit(it) } + ")"
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
        !config.gles -> ""
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
        VarType.TextureUnit -> if (config.programConfig.externalTextureSampler) "samplerExternalOES" else "sampler2D"
        VarType.Sampler1D -> "sampler1D"
        VarType.Sampler2D -> "sampler2D"
        VarType.Sampler3D -> "sampler3D"
        VarType.SamplerCube -> "samplerCube"
        else -> {
            when (type.kind) {
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
