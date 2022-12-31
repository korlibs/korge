package korge.graphics.backend.metal.shader

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

    fun generateResult(shader: Shader): Result = generateResult(shader.stm, shader.functions)

    private fun generateResult(root: Program.Stm, customFunctions: List<FuncDecl>): Result {
        val types = GlobalsProgramVisitor()

        val mainFunction = when (kind) {
            ShaderType.FRAGMENT -> FuncDecl("fragment main", VarType.Float4, listOf(), root)
            ShaderType.VERTEX -> FuncDecl("vertex main", VarType.Float2, listOf(), root)
        }.also(types::visit)

        val allFunctions = customFunctions.filter { it.ref.name in types.funcRefs }
            .reversed()
            .distinctBy { it.ref.name }
            .plus(mainFunction)

        val result = Indenter {

            // include metal std library
            line("#include <metal_stdlib>")

            // use metal namespace to use std type short name
            line("using namespace metal;")

            for (it in types.attributes) line("$IN ${precToString(it.precision)}${typeToString(it.type)} ${it.name}${it.arrayDecl};")
            for (it in types.uniforms) line("$UNIFORM ${precToString(it.precision)}${typeToString(it.type)} ${it.name}${it.arrayDecl};")
            for (it in types.varyings) {
                if (it is Output) continue
                line("$OUT ${precToString(it.precision)}${typeToString(it.type)} ${it.name};")
            }

            for (function in allFunctions) {
                val generator = MetalShaderBodyGenerator(kind)
                generator.visit(function)

                val argsStrings = function.args.map { "${typeToString(it.second)} ${it.first}" }

                line("${typeToString(function.rettype)} ${function.name}(${argsStrings.joinToString(", ")})") {
                    for (temp in generator.temps) {
                        line(precToString(temp.precision) + typeToString(temp.type) + " " + temp.name + ";")
                    }
                    line(generator.programIndenter)
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

    fun generate(root: Program.Stm, customFunctions: List<FuncDecl>): String = generateResult(root, customFunctions).result
    fun generate(root: Shader): String = generate(root.stm, root.functions)
}
