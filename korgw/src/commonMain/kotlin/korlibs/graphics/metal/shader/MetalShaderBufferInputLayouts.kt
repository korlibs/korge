package korlibs.graphics.metal.shader

import korlibs.graphics.shader.Attribute
import korlibs.graphics.shader.Uniform
import korlibs.graphics.shader.VariableWithOffset
import korlibs.graphics.shader.VertexLayout
import korlibs.logger.Logger

internal data class MetalShaderBufferInputLayouts(
    var vertexLayouts: List<VertexLayout>,
    var uniforms: List<Uniform>
) {

    private val logger = Logger("MetalShaderBufferInputLayouts")
    private val inputBuffers by computeInputBuffers()

    internal val vertexInputStructure by lazy {
        vertexLayouts
            .mapIndexed { index, attributes -> index to attributes }
            .filter { (_, attributes) -> attributes.items.size >= 2 }
    }

    internal fun computeInputBuffers(): Lazy<MutableList<List<VariableWithOffset>>> = lazy {
        (vertexLayouts.map { it.items } + uniforms.map { listOf(it) })
            .toMutableList()
    }

    internal fun computeFunctionParameter(
        parameters: List<VariableWithOffset>,
        bodyGenerator: MetalShaderBodyGenerator
    ): Lazy<List<String>> = lazy {
        inputBuffers.filterNotIn(parameters)
            .map { it.findDeclarationFromInputBuffer(bodyGenerator) }

    }

    internal fun convertInputBufferToLocalDeclarations(
        parameters: List<VariableWithOffset>
    ): List<String> {
        return inputBuffers.filterNotIn(parameters)
            .filter { buffer -> buffer.any { it is Attribute } }
            .flatMap { it.toLocalDeclarations() }

    }

    private fun List<List<VariableWithOffset>>.filterNotIn(parameters: List<VariableWithOffset>): List<List<VariableWithOffset>> {
        return filter { buffer -> parameters.any { parameter -> parameter in buffer } }
    }

    private fun List<VariableWithOffset>.findDeclarationFromInputBuffer(bodyGenerator: MetalShaderBodyGenerator): String {
        val bufferIndex = inputBuffers.indexOf(this)
        return when {
            isEmpty() -> {
                logger.error { "buffer without layout" }
                ""
            }
            size > 1 -> "device const Buffer$bufferIndex* buffer$bufferIndex [[buffer($bufferIndex)]]"
            else -> {
                val variableWithOffset = first()
                val type = bodyGenerator.typeToString(variableWithOffset.type)
                val name = variableWithOffset.name
                when (variableWithOffset) {
                    is Attribute -> "device const $type* buffer$bufferIndex [[buffer($bufferIndex)]]"
                    else -> "constant $type& $name [[buffer($bufferIndex)]]"
                }
            }
        }
    }
    private fun List<VariableWithOffset>.toLocalDeclarations(): List<String> {
        val bufferIndex = inputBuffers.indexOf(this)

        return map {
            when {
                size > 1 -> "auto ${it.name} = buffer$bufferIndex[vertexId].${it.name};"
                else -> "auto ${it.name} = buffer$bufferIndex[vertexId];"
            }

        }
    }



}


