package korlibs.graphics.metal.shader

import korlibs.graphics.shader.*
import korlibs.graphics.shader.gl.GlobalsProgramVisitor
import korlibs.io.util.Indenter
import korlibs.korge.internal.*

internal const val vertexMainFunctionName = "vertexMain"
internal const val fragmentMainFunctionName = "fragmentMain"
private const val vertexInputStructureName = "VertexInput"
internal const val vertexInputStructureDeclarationName = "vertexInput"

@KorgeInternal
class MetalShaderGenerator(
    private val vertexShader: VertexShader,
    private val fragmentShader: FragmentShader,
    private val bufferLayouts: MetalShaderBufferInputLayouts
) : BaseMetalShaderGenerator {

    private var attributeIndex = -1
    private val vertexInstructions = vertexShader.stm
    private val fragmentInstructions = fragmentShader.stm
    private val vertexBodyGenerator = MetalShaderBodyGenerator(ShaderType.VERTEX)
    private val fragmentBodyGenerator = MetalShaderBodyGenerator(ShaderType.FRAGMENT)
    private val inputStructure by lazy {
        bufferLayouts.vertexInputStructure
            .map { (index, attributes) -> index to attributes.attributes.toMetalShaderStructureGeneratorAttributes(shouldAddAttributeNumber = true)}
    }
    private val vertexVisitor by lazy {
        GlobalsProgramVisitor()
            .also { it.visit(vertexInstructions) }
    }
    private val fragmentVisitor by lazy {
        GlobalsProgramVisitor()
            .also { it.visit(fragmentInstructions) }
    }
    private val varyings = computeVaryings()
    private val attributes = bufferLayouts.attributes
    private val vertexParameters by bufferLayouts.computeFunctionParameter(
        vertexVisitor.uniforms.toList(),
        vertexBodyGenerator
    )
    private val fragmentParameters by bufferLayouts.computeFunctionParameter(
        fragmentVisitor.uniforms.toList(),
        fragmentBodyGenerator
    )

    @KorgeInternal
    data class Result(
        val result: String,
        val inputBuffers: MetalShaderBufferInputLayouts
    )

    fun generateResult(): Result  {

        val result = Indenter {

            addHeaders()
            declareVertexInputStructure()
            declareVertexOutputStructure()

            listFunctions()
                .also { generationFunctions(it) }

            generateVertexMainFunction()
            generateFragmentMainFunction()

        }.toString()

        return Result(
            result,
            bufferLayouts
        )
    }

    private fun listFunctions() = (vertexShader.functions + fragmentShader.functions)

    private fun Indenter.declareVertexInputStructure() = MetalShaderStructureGenerator.generate(
        indenter = this,
        name = vertexInputStructureName,
        attributes = attributes.toMetalShaderStructureGeneratorAttributes(true)
    )

    private fun Indenter.declareVertexOutputStructure() = MetalShaderStructureGenerator.generate(
        indenter = this,
        name = "v2f",
        attributes = varyings.toMetalShaderStructureGeneratorAttributes()
    )

    private fun Indenter.generateVertexMainFunction() {

        val parameters = listOf(("$vertexInputStructureName $vertexInputStructureDeclarationName [[stage_in]]")) + vertexParameters

        line("vertex v2f $vertexMainFunctionName(")
        indent {
            for ((index, parameter) in parameters.withIndex()) {
                line(if (index == parameters.size - 1) parameter else "$parameter,")
            }
        }
        ")" {
            line("v2f out;")

            bufferLayouts.convertInputBufferToLocalDeclarations(vertexVisitor.attributes.toList())
                .forEach { line(it) }

            vertexBodyGenerator.visit(vertexShader.stm)
            line(vertexBodyGenerator.programIndenter)
            line("return out;")
        }

    }

    private fun Indenter.generateFragmentMainFunction() {

        val parameters = listOf(("v2f in [[stage_in]]"))  + fragmentParameters

        line("fragment float4 $fragmentMainFunctionName(")
        indent {
            for ((index, parameter) in parameters.withIndex()) {
                line(if (index == parameters.size - 1) parameter else "$parameter,")
            }
        }
        line(")") {
            line("float4 out;")
            fragmentBodyGenerator.visit(fragmentShader.stm)
            line(fragmentBodyGenerator.programIndenter)
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
                    line(precisionToString(temp.precision) + typeToString(temp.type) + " " + temp.name + ";")
                }
                line(generator.programIndenter)
            }
        }
    }

    private fun List<Variable>.toMetalShaderStructureGeneratorAttributes(shouldAddAttributeNumber: Boolean = false): List<MetalShaderStructureGenerator.Attribute> {
        return map {
            if (shouldAddAttributeNumber) {
                attributeIndex += 1
            }

            MetalShaderStructureGenerator.Attribute(
                type = vertexBodyGenerator.typeToString(it.type),
                name = if (it == Output) "position" else it.name,
                attribute = if (it == Output) "position" else if (shouldAddAttributeNumber) "attribute($attributeIndex)" else null
            )
        }
    }

    private fun computeVaryings(): List<Varying> {
        return vertexVisitor.varyings.toList()
    }
}

private fun Indenter.addHeaders() {
    // include metal std library
    line("#include <metal_stdlib>")

    // use metal namespace to use std type short name
    line("using namespace metal;")
}
