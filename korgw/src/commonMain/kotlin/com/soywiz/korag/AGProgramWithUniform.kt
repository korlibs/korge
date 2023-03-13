package com.soywiz.korag

import com.soywiz.kds.*
import com.soywiz.kds.iterators.*
import com.soywiz.korag.shader.*

class AGProgramWithUniforms(val program: Program, val bufferCache: BufferCache = BufferCache()) {
    // Holder to reuse AGRichUniformBlockData and buffers between several programs
    class BufferCache {
        private val uniformBlocks = FastIdentityMap<UniformBlock, AGRichUniformBlockData>()

        fun reset() {
            uniformBlocks.fastForEach { key, value -> value.reset() }
        }

        fun uploadUpdatedBuffers() {
            uniformBlocks.fastForEach { key, value -> value.upload() }
        }

        operator fun get(block: UniformBlock): AGRichUniformBlockData =
            uniformBlocks.getOrPut(block) { AGRichUniformBlockData(block) }
    }

    val uniformLayouts = program.uniforms.map { it.linkedLayout as? UniformBlock? }.distinct().filterNotNull()
    val maxLocation = uniformLayouts.maxOf { it?.fixedLocation?.plus(1) ?: -1 }
    val uniformsBlocks = Array<UniformBlock?>(maxLocation) { null }.also {
        for (layout in uniformLayouts) {
            it[layout.fixedLocation] = layout
        }
    }
    val uniformsBlocksData = Array<AGRichUniformBlockData?>(uniformsBlocks.size) {
        uniformsBlocks[it]?.let { bufferCache[it] }
    }
    private val agUniformBlockDatas = Array(uniformsBlocks.size) { uniformsBlocksData[it]?.block?.let { UniformBlockData(it) } }
    private val agBuffers = Array(uniformsBlocks.size) { uniformsBlocksData[it]?.agBuffer }
    private val agBufferIndices = IntArray(uniformsBlocks.size) { 0 }

    fun reset() {
        uniformsBlocksData.fastForEach { it?.reset() }
    }

    fun createRef(): AGUniformBlocksBuffersRef {
        for (n in agBufferIndices.indices) agBufferIndices[n] = uniformsBlocksData[n]?.currentIndex ?: -1
        return AGUniformBlocksBuffersRef(agUniformBlockDatas, agBuffers, agBufferIndices.copyOf())
    }

    operator fun get(block: UniformBlock): AGRichUniformBlockData {
        val rblock = uniformsBlocks.getOrNull(block.fixedLocation) ?: error("Can't find block")
        if (rblock !== block) error("Block $block not used in program")
        return uniformsBlocksData[block.fixedLocation]!!
    }

    operator fun invoke(ublock: UniformBlock, block: (AGRichUniformBlockData) -> Unit) {
        block(this[ublock])
    }

    operator fun get(uniform: Uniform): AGUniformValue = this[(uniform.linkedLayout as UniformBlock)][uniform]
}

open class AGRichUniformBlockData(val block: UniformBlock) {
    val buffer = UniformBlockBuffer(block)
    val data = buffer.data
    val agBuffer = AGBuffer()
    val indexStack = IntStack()
    var currentIndex = -1

    // Called at the start of the frame
    fun reset() {
        currentIndex = -1
        indexStack.clear()
        buffer.reset()
    }

    inline fun push(deduplicate: Boolean = false, block: (AGRichUniformBlockData) -> Unit): Boolean {
        block(this)
        val oldIndex = currentIndex
        currentIndex = buffer.putCurrent(deduplicate = deduplicate)
        if (oldIndex != currentIndex) {
            indexStack.push(oldIndex)
        }
        return oldIndex != currentIndex
    }

    fun pop() {
        currentIndex = indexStack.pop()
    }

    // Called when we are about to use a uniform block buffer to perform a batch
    fun nextBuffer() {
    }

    // @TODO: Upload range from the previous uploaded index to the lastIndex
    fun upload(): AGRichUniformBlockData {
        agBuffer.upload(buffer.buffer, 0, kotlin.math.max(0, (buffer.currentIndex + 1) * buffer.elementSize))
        return this
    }

    operator fun get(uniform: Uniform): AGUniformValue = data[uniform]
}
