package korlibs.graphics

import korlibs.datastructure.*
import korlibs.graphics.shader.*
import korlibs.logger.*
import korlibs.memory.*

private val logger by lazy {
    Logger("AGVertexArrayObjectFlattener")
}

fun AGVertexArrayObject.flatten(): AGVertexArrayObject  = AGVertexArrayObject(
    this.list.flatMap { vertexData ->
        when(vertexData.layout.items.size) {
            1 -> listOf(vertexData)
            else -> vertexData.layout.items.map { item ->
                AGVertexData(
                    buffer = vertexData.buffer.extractDataOf(item, vertexData.layout),
                    layout = ProgramLayout(listOf(item))
                )
            }
        }
    }.let { FastArrayList(it) }
)

fun AGBuffer.extractDataOf(attribute: Attribute, layout: VertexLayout): AGBuffer {
    val indexes = sizeInBytes / layout.totalSize
    val offsetOnLayout = layout.offsetOf(attribute)
    return Buffer(attribute.totalBytes * indexes).apply {
        for (index in 0 until indexes) {
            val offset = index * layout.totalSize + offsetOnLayout
            val localOffset = index * attribute.totalBytes
            val data = mem!!.getUnalignedArrayInt8(offset, ByteArray(attribute.totalBytes))
            logger.trace { "extracting data of $attribute from $data" }
            check(data.size == attribute.totalBytes)
            setUnalignedArrayInt8(localOffset, data)
        }
    }.let { AGBuffer().upload(it) }
}

private fun ProgramLayout<Attribute>.offsetOf(attribute: Attribute): Int {
    var offset = 0
    for (item in items) {
        if (item == attribute) return offset
        offset += item.totalBytes
    }
    error("attribute $attribute not found in $this")
}
