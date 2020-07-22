package com.esotericsoftware.spine.rendering

class VertexAttributes {
    object Usage {
        const val Position = 1
        const val ColorUnpacked = 2
        const val ColorPacked = 4
        const val Normal = 8
        const val TextureCoordinates = 16
        const val Generic = 32
        const val BoneWeight = 64
        const val Tangent = 128
        const val BiNormal = 256
    }
}
