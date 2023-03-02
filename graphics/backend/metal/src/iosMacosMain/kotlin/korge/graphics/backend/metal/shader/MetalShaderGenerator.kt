package korge.graphics.backend.metal.shader

import com.soywiz.korag.*
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
        val result: String,
        val attributes: List<Attribute>,
        val uniforms: List<Uniform>,
        val varyings: List<Varying>
    )

    fun generateResult(): Result = generateResult( vertexShader.functions + fragmentShader.functions)

    private fun generateResult(customFunctions: List<FuncDecl>): Result {
        val vertexInstructions = vertexShader.stm
        val types = GlobalsProgramVisitor()

        FuncDecl("main", VarType.TVOID, listOf(), vertexInstructions)
            .also(types::visit)

        val result = Indenter {

            addHeaders()

            declareOutputStructure(types.varyings)

            customFunctions.filter { it.ref.name in types.funcRefs }
                .reversed()
                .distinctBy { it.ref.name }
                .let { generationFunctions(it) }

            generateVertexMainFunction(types.attributes, types.uniforms)
            generateFragmentMainFunction()


        }.toString()

        return Result(
            result,
            attributes = types.attributes.toList(),
            uniforms = types.uniforms.toList(),
            varyings = types.varyings.toList()
        )
    }

    private fun Indenter.declareInputStructure(attributes: LinkedHashSet<Attribute>) {
        if (attributes.isEmpty()) return
        val generator = MetalShaderBodyGenerator(ShaderType.VERTEX)

        "struct VertexInput" {
            attributes.forEach {
                +"${generator.typeToString(it.type)} ${it.name};"
            }
        }
    }

    private fun Indenter.declareOutputStructure(attributes: LinkedHashSet<Varying>) {
        if (attributes.isEmpty()) return
        val generator = MetalShaderBodyGenerator(ShaderType.VERTEX)

        "struct v2f"(expressionSuffix = ";") {
            attributes.forEach {
                val name = if (it == Output) "position [[position]]" else it.name
                +"${generator.typeToString(it.type)} $name;"
            }
        }
    }

    private fun Indenter.generateVertexMainFunction(
        attributes: LinkedHashSet<Attribute>,
        uniforms: LinkedHashSet<Uniform>
    ) {
        val generator = MetalShaderBodyGenerator(ShaderType.VERTEX)
        val parameters = mutableListOf("uint vertexId [[vertex_id]]")

        attributes.forEachIndexed { index, attribute ->
            parameters.add(
                "device const ${generator.typeToString(attribute.type)}* ${attribute.name} [[buffer($index)]]"
            )
        }

        uniforms.forEachIndexed { index, uniform ->
            val uniformIndex = index + attributes.size
            parameters.add(
                "constant ${generator.typeToString(uniform.type)}& ${uniform.name} [[buffer($uniformIndex)]]"
            )
        }

        "vertex v2f $vertexMainFunctionName(${parameters.joinToString(",")})" {
            line("v2f out;")
            generator.visit(vertexShader.stm)
            line(generator.programIndenter)
            line("return out;")
        }
    }

    private fun Indenter.generateFragmentMainFunction() {
        "fragment float4 $fragmentMainFunctionName(v2f in [[stage_in]])" {
            line("float4 out;")
            val generator = MetalShaderBodyGenerator(ShaderType.FRAGMENT)
            generator.visit(fragmentShader.stm)
            line(generator.programIndenter)
            line("return out;")
        }
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

private fun Attribute.toMetalName(): String {
    return when (this) {
        DefaultShaders.a_Tex -> "texture"
        DefaultShaders.a_Col -> "color"
        DefaultShaders.a_Pos -> "position"
        else -> error("unreachable statement")
    }
}

private fun Indenter.addHeaders() {
    // include metal std library
    line("#include <metal_stdlib>")

    // use metal namespace to use std type short name
    line("using namespace metal;")
}
