package com.soywiz.korag

import com.soywiz.kds.*
import com.soywiz.kds.iterators.*
import com.soywiz.korag.shader.*

class AGProgramWithUniforms(val program: Program, val bufferCache: BufferCache = BufferCache()) {
    // Holder to reuse AGRichUniformBlockData and buffers between several programs
    class BufferCache {
        private val uniformBlocks = FastIdentityMap<UniformBlock, UniformBlockBuffer<*>>()

        fun reset() {
            uniformBlocks.fastForEach { key, value -> value.reset() }
        }

        fun uploadUpdatedBuffers() {
            uniformBlocks.fastForEach { key, value -> value.upload() }
        }

        operator fun <T : UniformBlock> get(block: T): UniformBlockBuffer<T> =
            uniformBlocks.getOrPut(block) { UniformBlockBuffer(block) } as UniformBlockBuffer<T>
    }

    val uniformLayouts = program.typedUniforms.map { it.block }.distinct()
    val maxLocation = uniformLayouts.maxOfOrNull { it.fixedLocation + 1 } ?: 0

    val uniformsBlocks = Array<UniformBlock?>(maxLocation) { null }.also {
        for (layout in uniformLayouts) it[layout.fixedLocation] = layout
    }
    val uniformsBlocksData = Array<UniformBlockBuffer<*>?>(uniformsBlocks.size) {
        uniformsBlocks[it]?.let { bufferCache[it] }
    }
    private val agUniformBlockDatas: Array<UniformBlockBuffer<*>?> = Array(uniformsBlocks.size) { uniformsBlocksData[it]?.block?.let { UniformBlockBuffer(it) } }
    private val agBuffers = Array(uniformsBlocks.size) { uniformsBlocksData[it]?.agBuffer }
    private val agBufferIndices = IntArray(uniformsBlocks.size) { 0 }

    fun reset() {
        agUniformBlockDatas.fastForEach { it?.reset() }
    }

    fun createRef(): UniformBlocksBuffersRef {
        for (n in agBufferIndices.indices) agBufferIndices[n] = uniformsBlocksData[n]?.currentIndex ?: -1
        return UniformBlocksBuffersRef(agUniformBlockDatas, agBuffers, agBufferIndices.copyOf())
    }

    operator fun <T : UniformBlock> get(block: T): UniformBlockBuffer<T> {
        val rblock = uniformsBlocks.getOrNull(block.fixedLocation)
            ?: error("Can't find block '$block'")
        if (rblock !== block) error("Block $block not used in program")
        return uniformsBlocksData[block.fixedLocation]!! as UniformBlockBuffer<T>
    }
}
