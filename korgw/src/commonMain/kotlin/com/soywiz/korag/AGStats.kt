package com.soywiz.korag

import com.soywiz.kmem.unit.*

class AGStats(
    var texturesCount: Int = 0,
    var texturesMemory: ByteUnits = ByteUnits.fromBytes(0),
    var buffersCount: Int = 0,
    var buffersMemory: ByteUnits = ByteUnits.fromBytes(0),
    var renderBuffersCount: Int = 0,
    var renderBuffersMemory: ByteUnits = ByteUnits.fromBytes(0),
    var texturesCreated: Int = 0,
    var texturesDeleted: Int = 0,
    var programCount: Int = 0,
) {
    override fun toString(): String =
        "AGStats(textures[$texturesCount] = $texturesMemory, buffers[$buffersCount] = $buffersMemory, renderBuffers[$renderBuffersCount] = $renderBuffersMemory, programs[$programCount])"
}

