package korlibs.graphics.metal.shader

import korlibs.graphics.shader.*
import korlibs.graphics.shader.gl.GlobalsProgramVisitor
import korlibs.io.util.Indenter
import korlibs.logger.Logger

internal const val vertexMainFunctionName = "vertexMain"
internal const val fragmentMainFunctionName = "fragmentMain"

class MetalShaderGenerator(
    private val vertexShader: VertexShader,
    private val fragmentShader: FragmentShader,
    private val bufferLayouts: MetalShaderBufferInputLayouts
) : BaseMetalShaderGenerator {


    private val vertexInstructions = vertexShader.stm
    private val fragmentInstructions = fragmentShader.stm
    private val varyings = computeVaryings()
    private val logger = Logger("MetalShaderGenerator")
    private val vertexBodyGenerator = MetalShaderBodyGenerator(ShaderType.VERTEX)
    private val inputStructure = mutableListOf<VertexLayout>()
    private val inputBuffers by with(bufferLayouts) {
        computeInputBuffers()
    }

    private fun computeVaryings(): List<Varying> {
        val types = GlobalsProgramVisitor()
        FuncDecl("main", VarType.TVOID, listOf(), vertexInstructions)
            .also(types::visit)
        return types.varyings.toList()
    }

    data class Result(
        val result: String,
        val inputBuffers: List<List<VariableWithOffset>>
    )

    fun generateResult(): Result = generateResult(vertexShader.functions + fragmentShader.functions)

    private fun generateResult(customFunctions: List<FuncDecl>): Result {
        val types = GlobalsProgramVisitor()

        FuncDecl("main", VarType.TVOID, listOf(), vertexInstructions)
            .also(types::visit)

        val result = Indenter {

            addHeaders()
            declareVertexInputStructures()
            declareVertexOutputStructure()

            customFunctions.filter { it.ref.name in types.funcRefs }
                .reversed()
                .distinctBy { it.ref.name }
                .let { generationFunctions(it) }

            generateVertexMainFunction()
                .also(inputBuffers::addAll)

            generateFragmentMainFunction()
                .also(inputBuffers::addAll)


        }.toString()

        return Result(
            result,
            inputBuffers.toList()
        )
    }

    private fun Indenter.declareVertexInputStructures() {
        bufferLayouts.vertexInputStructure
            .map { (index, attributes) -> index to attributes.attributes.toMetalShaderStructureGeneratorAttributes()}
            .forEach { (index, attributes) ->
                MetalShaderStructureGenerator.generate(
                    indenter = this,
                    name = "Buffer$index",
                    attributes = attributes
                )
            }
    }

    private fun Indenter.declareVertexOutputStructure() = MetalShaderStructureGenerator.generate(
        indenter = this,
        name = "v2f",
        attributes = varyings.toMetalShaderStructureGeneratorAttributes()
    )

    private fun Indenter.generateVertexMainFunction(): List<List<VariableWithOffset>> {
        val types = GlobalsProgramVisitor()

        FuncDecl("main", VarType.TVOID, listOf(), vertexInstructions)
            .also(types::visit)

        val attributes = types.attributes
        val uniforms =  types.uniforms

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

        return inputBuffers.map { listOf(it) }
    }

    private fun Indenter.generateFragmentMainFunction(): List<List<VariableWithOffset>> {

        val generator = MetalShaderBodyGenerator(ShaderType.FRAGMENT)
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

        return fragmentUniformInput.map { listOf(it) }
    }

    private fun Indenter.generationFunctions(functions: List<FuncDecl>) {
        for (function in functions) {
            val generator = MetalShaderBodyGenerator()
            generator.visit(function)

            val argsStrings = function.args.map { "${typeToString(it.second)} ${it.first}" }

            line("${typeToString(function.rettype)} ${function.name}(${argsStrings.joinToString(", ")})") {
                for (temp in generator.temps) {
                    line(precisionToString(temp.precision) + typeToString(temp.type) + " " + temp.name + ";")
                }
                line(generator.programIndenter)
            }
        }
    }

    private fun List<Variable>.toMetalShaderStructureGeneratorAttributes(): List<MetalShaderStructureGenerator.Attribute> {
        return map {
            MetalShaderStructureGenerator.Attribute(
                type = vertexBodyGenerator.typeToString(it.type),
                name = if (it == Output) "position" else it.name,
                attribute = if (it == Output) "position" else null
            )
        }
    }
}

private fun Indenter.addHeaders() {
    // include metal std library
    line("#include <metal_stdlib>")

    // use metal namespace to use std type short name
    line("using namespace metal;")
}
