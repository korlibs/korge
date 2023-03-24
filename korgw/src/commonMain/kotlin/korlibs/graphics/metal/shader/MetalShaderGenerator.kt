package korlibs.graphics.metal.shader

import korlibs.graphics.*
import korlibs.graphics.shader.*
import korlibs.graphics.shader.gl.*
import korlibs.io.util.*

internal const val vertexMainFunctionName = "vertexMain"
internal const val fragmentMainFunctionName = "fragmentMain"

class MetalShaderGenerator(
    private val vertexShader: VertexShader,
    private val fragmentShader: FragmentShader
) : BaseMetalShaderGenerator {

    private val inputBuffers = mutableListOf<VariableWithOffset>()

    data class Result(
        val result: String,
        val inputBuffers: List<VariableWithOffset>
    )

    fun generateResult(): Result = generateResult(vertexShader.functions + fragmentShader.functions)

    private fun generateResult(customFunctions: List<FuncDecl>): Result {
        val vertexInstructions = vertexShader.stm
        val types = GlobalsProgramVisitor()

        FuncDecl("main", VarType.TVOID, listOf(), vertexInstructions)
            .also(types::visit)

        val result = Indenter {

            addHeaders()

            declareVertexOutputStructure(types.varyings)

            customFunctions.filter { it.ref.name in types.funcRefs }
                .reversed()
                .distinctBy { it.ref.name }
                .let { generationFunctions(it) }

            generateVertexMainFunction(types.attributes, types.uniforms)
                .also(inputBuffers::addAll)

            generateFragmentMainFunction()
                .also(inputBuffers::addAll)


        }.toString()

        return Result(
            result,
            inputBuffers.toList()
        )
    }

    private fun Indenter.declareVertexOutputStructure(attributes: LinkedHashSet<Varying>) {
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
    ): List<VariableWithOffset> {
        val generator = MetalShaderBodyGenerator(ShaderType.VERTEX)
        val inputBuffers = attributes.toList() + uniforms

        val parameters = listOf(("uint vertexId [[vertex_id]]")) +
            inputBuffers.mapIndexed { index, variableWithOffset ->
                val parameterModifier = when (variableWithOffset) {
                    is Attribute -> "device const" to "*"
                    else -> "constant" to "&"
                }

                "${parameterModifier.first} ${generator.typeToString(variableWithOffset.type)}${parameterModifier.second} ${variableWithOffset.name} [[buffer($index)]]"
            }

        line("vertex v2f $vertexMainFunctionName(")
        indent {
            for ((index, parameter) in parameters.withIndex()) {
                line(if (index == parameters.size - 1) parameter else "$parameter,")
            }
        }
        ")" {
            line("v2f out;")
            generator.visit(vertexShader.stm)
            line(generator.programIndenter)
            line("return out;")
        }

        return inputBuffers
    }

    private fun Indenter.generateFragmentMainFunction(): List<VariableWithOffset> {

        val generator = MetalShaderBodyGenerator(ShaderType.FRAGMENT)
        val fragmentInstructions = fragmentShader.stm
        val types = GlobalsProgramVisitor()
        FuncDecl("main", VarType.TVOID, listOf(), fragmentInstructions)
            .also(types::visit)

        val fragmentUniformInput = types.uniforms.toList()
        val initialBufferIndex = inputBuffers.size

        val parameters = listOf(("v2f in [[stage_in]]")) +
            fragmentUniformInput.mapIndexed { indexOfUniform, uniform ->
                val parameterModifier = when (uniform) {
                    is Attribute -> "device const" to "*"
                    else -> "constant" to "&"
                }

                val index = indexOfUniform + initialBufferIndex
                "${parameterModifier.first} ${generator.typeToString(uniform.type)}${parameterModifier.second} ${uniform.name} [[buffer($index)]]"
            }


        line("fragment float4 $fragmentMainFunctionName(")
        indent {
            for ((index, parameter) in parameters.withIndex()) {
                line(if (index == parameters.size - 1) parameter else "$parameter,")
            }
        }
        line(")") {
            line("float4 out;")
            generator.visit(fragmentShader.stm)
            line(generator.programIndenter)
            line("return out;")
        }

        return fragmentUniformInput
    }

    private fun Indenter.generationFunctions(functions: List<FuncDecl>) {
        for (function in functions) {
            val generator = MetalShaderBodyGenerator()
            generator.visit(function)

            val argsStrings = function.args.map { "${typeToString(it.second)} ${it.first}" }

            line("${typeToString(function.rettype)} ${function.name}(${argsStrings.joinToString(", ")})") {
                for (temp in generator.temps) {
                    line(precitionToString(temp.precision) + typeToString(temp.type) + " " + temp.name + ";")
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