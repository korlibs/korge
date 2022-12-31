package korge.graphics.backend.metal.shader

import com.soywiz.korag.shader.*
import com.soywiz.korag.shader.gl.*
import com.soywiz.korio.util.*

internal const val vertexMainFunctionName = "vertexMain"
internal const val fragmentMainFunctionName = "fragmentMain"

class MetalShaderGenerator(
    private val vertexShader: VertexShader,
    private val fragmentShader: FragmentShader
) : BaseMetalShaderGenerator {

    data class Result(
        val generator: MetalShaderGenerator,
        val result: String,
        val attributes: List<Attribute>,
        val uniforms: List<Uniform>,
        val varyings: List<Varying>
    )

    fun generateResult(): Result = generateResult(vertexShader.stm, fragmentShader.stm, vertexShader.functions + fragmentShader.functions)

    private fun generateResult(vertexInstructions: Program.Stm, fragmentInstructions: Program.Stm, customFunctions: List<FuncDecl>): Result {
        val types = GlobalsProgramVisitor()

        FuncDecl("main", VarType.TVOID, listOf(), vertexInstructions)
            .also(types::visit)

        val result = Indenter {

            addHeaders()

            declareInputStructure()
            declareOutputStructure()

            customFunctions.filter { it.ref.name in types.funcRefs }
                .reversed()
                .distinctBy { it.ref.name }
                .let { generationFunctions(it) }

            generateVertexMainFunction()
            generateFragmentMainFunction()


        }.toString()

        return Result(
            this, result,
            attributes = types.attributes.toList(),
            uniforms = types.uniforms.toList(),
            varyings = types.varyings.toList()
        )
    }

    private fun Indenter.declareInputStructure() {
        line("struct VertexInput {")
        //TODO to complete
        line("}")
    }

    private fun Indenter.declareOutputStructure() {
        line("struct VertexOutput {")
        //TODO to complete
        line("}")
    }

    private fun Indenter.generateVertexMainFunction() {
        line("vertex VertexOutput $vertexMainFunctionName() {")
        val generator = MetalShaderBodyGenerator(ShaderType.VERTEX)
        generator.visit(vertexShader.stm)
        line(generator.programIndenter)
        line("}")
    }

    private fun Indenter.generateFragmentMainFunction() {
        line("vertex float4 $fragmentMainFunctionName() {")
        val generator = MetalShaderBodyGenerator(ShaderType.FRAGMENT)
        generator.visit(fragmentShader.stm)
        line(generator.programIndenter)
        line("}")
    }

    private fun Indenter.generationFunctions(functions: List<FuncDecl>) {
        for (function in functions) {
            val generator = MetalShaderBodyGenerator()
            generator.visit(function)

            val argsStrings = function.args.map { "${typeToString(it.second)} ${it.first}" }

            line("${typeToString(function.rettype)} ${function.name}(${argsStrings.joinToString(", ")})") {
                for (temp in generator.temps) {
                    line(precToString(temp.precision) + typeToString(temp.type) + " " + temp.name + ";")
                }
                line(generator.programIndenter)
            }
        }
    }

}
private fun Indenter.addHeaders() {
    // include metal std library
    line("#include <metal_stdlib>")

    // use metal namespace to use std type short name
    line("using namespace metal;")
}
