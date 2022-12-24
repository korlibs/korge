package korge.graphics.backend.metal.shader

import com.soywiz.klogger.*
import com.soywiz.korag.shader.*
import com.soywiz.korag.shader.gl.*
import com.soywiz.korio.util.*

class MetalShaderGenerator(
    private val kind: ShaderType
) : BaseMetalShaderGenerator {

    data class Result(
        val generator: MetalShaderGenerator,
        val result: String,
        val attributes: List<Attribute>,
        val uniforms: List<Uniform>,
        val varyings: List<Varying>
    )

    fun generateResult(shader: Shader): Result = generateResult(shader.stm, shader.funcs)

    fun generateResult(root: Program.Stm, funcs: List<FuncDecl>): Result {
        val types = GlobalsProgramVisitor()

        val mainFunc = FuncDecl("main", VarType.TVOID, listOf(), root)
        types.visit(mainFunc)

        val customFuncs = funcs.filter { it.ref.name in types.funcRefs }.reversed().distinctBy { it.ref.name }
        for (func in funcs) types.visit(mainFunc)

        val allFuncs = customFuncs + listOf(mainFunc)

        val result = Indenter {

            for (it in types.attributes) line("$IN ${precToString(it.precision)}${typeToString(it.type)} ${it.name}${it.arrayDecl};")
            for (it in types.uniforms) line("$UNIFORM ${precToString(it.precision)}${typeToString(it.type)} ${it.name}${it.arrayDecl};")
            for (it in types.varyings) {
                if (it is Output) continue
                line("$OUT ${precToString(it.precision)}${typeToString(it.type)} ${it.name};")
            }

            for (func in allFuncs) {
                val gen = MetalShaderBodyGenerator(kind)
                gen.visit(func)

                val argsStrings = func.args.map { "${typeToString(it.second)} ${it.first}" }

                line("${typeToString(func.rettype)} ${func.name}(${argsStrings.joinToString(", ")})") {
                    for (temp in gen.temps) {
                        line(precToString(temp.precision) + typeToString(temp.type) + " " + temp.name + ";")
                    }
                    line(gen.programIndenter)
                }
            }
        }.toString()

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
