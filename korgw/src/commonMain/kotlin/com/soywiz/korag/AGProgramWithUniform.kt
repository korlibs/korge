package com.soywiz.korag

import com.soywiz.kds.*
import com.soywiz.kds.iterators.*
import com.soywiz.korag.shader.*

class AGProgramWithUniforms(val program: Program, val bufferCache: BufferCache = BufferCache()) {
    // Holder to reuse AGRichUniformBlockData and buffers between several programs
    class BufferCache {
        private val newUniformBlocks = FastIdentityMap<NewUniformBlock, NewUniformBlockBuffer>()

        fun reset() {
            newUniformBlocks.fastForEach { key, value -> value.reset() }
        }

        fun uploadUpdatedBuffers() {
            newUniformBlocks.fastForEach { key, value -> value.upload() }
        }

        operator fun get(block: NewUniformBlock): NewUniformBlockBuffer =
            newUniformBlocks.getOrPut(block) { NewUniformBlockBuffer(block) }
    }

    init {
        // Ensure uniformBlock is created
        //program.uniforms.fastForEach { it.typedUniform?.block?.uniformBlock }
    }
    val newUniformLayouts = program.typedUniforms.map { it.block }.distinct()
    //val newUniformLayouts = program.uniforms.map { it.linkedLayout as? UniformBlock? }.distinct().filterNotNull()

    val maxNewLocation = newUniformLayouts.maxOfOrNull { it.fixedLocation + 1 } ?: 0

    //init {
    //    println("program.uniforms=${program.uniforms}")
    //    println("program.uniforms=${program.uniforms.map { it.typedUniform?.block }}")
    //    println("uniformLayouts=$uniformLayouts")
    //    println("newUniformLayouts=$newUniformLayouts")
    //}

    val newUniformsBlocks = Array<NewUniformBlock?>(maxNewLocation) { null }.also {
        for (layout in newUniformLayouts) it[layout.fixedLocation] = layout
    }
    val newUniformsBlocksData = Array<NewUniformBlockBuffer?>(newUniformsBlocks.size) {
        newUniformsBlocks[it]?.let { bufferCache[it] }
    }
    private val agNewUniformBlockDatas = Array(newUniformsBlocks.size) { newUniformsBlocksData[it]?.block?.let { NewUniformBlockBuffer(it) } }
    private val agNewBuffers = Array(newUniformsBlocks.size) { newUniformsBlocksData[it]?.agBuffer }
    private val agNewBufferIndices = IntArray(newUniformsBlocks.size) { 0 }

    fun reset() {
        agNewUniformBlockDatas.fastForEach { it?.reset() }
    }

    fun createNewRef(): AGNewUniformBlocksBuffersRef {
        for (n in agNewBufferIndices.indices) agNewBufferIndices[n] = newUniformsBlocksData[n]?.currentIndex ?: -1
        return AGNewUniformBlocksBuffersRef(agNewUniformBlockDatas, agNewBuffers, agNewBufferIndices.copyOf())
    }

    operator fun get(block: NewUniformBlock): NewUniformBlockBuffer {
        val rblock = newUniformsBlocks.getOrNull(block.fixedLocation)
            ?: error("Can't find block '$block'")
        if (rblock !== block) error("Block $block not used in program")
        return newUniformsBlocksData[block.fixedLocation]!!
    }

    //operator fun get(uniform: Uniform): AGUniformValue = this[(uniform.linkedLayout as UniformBlock)][uniform]
    //operator fun get(uniform: NewTypedUniform<*>): AGUniformValue = this[uniform.block][uniform]
}

/*
@Deprecated("")
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
*/
