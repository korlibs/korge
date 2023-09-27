package korlibs.graphics.metal.shader

import korlibs.io.util.Indenter

/**
 * Generate structure to be used on Metal Shader like this
 * struct name {
 *     type name [[attribute]];
 *     type name;
 * };
 */
object MetalShaderStructureGenerator {

    data class Attribute(val type: String, val name: String, val attribute: String? = null) {
        internal fun generate(): String {
            return "$type $name${attribute.generateAttribute()};"
        }
    }

    // TODO: when context receiver leave preview, move indenter as extension
    fun generate(indenter: Indenter, name: String, attributes: List<Attribute>) = with(indenter) {
        "struct $name"(expressionSuffix = ";") {
            attributes.map { it.generate()}
                .forEach { +it }

        }
    }
}

private fun String?.generateAttribute(): String = when (this) {
    null -> ""
    else -> " [[$this]]"
}
