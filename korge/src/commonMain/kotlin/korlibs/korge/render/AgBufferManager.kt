package korlibs.korge.render

import korlibs.datastructure.*
import korlibs.datastructure.iterators.fastForEach
import korlibs.memory.*
import korlibs.graphics.*

/**
 * Class to handle cached buffers, that are freed after a few frames of not being used
 */
class AgBufferManager(
    val ag: AG
) {
    private val buffers = FastIdentityMap<AgCachedBuffer, AGBuffer>()
    private val referencedBuffersSinceGC = FastSmallSet<AgCachedBuffer>()
    private val bufferPool = Pool { AGBuffer() }

    fun getBuffer(cached: AgCachedBuffer): AGBuffer {
        referencedBuffersSinceGC.add(cached)
        return buffers.getOrPut(cached) {
            bufferPool.alloc().also {
                it.upload(cached.data)
            }
        }
    }

    var fcount = 0
    var framesBetweenGC = 60

    internal fun afterRender() {
        // Prevent leaks when not referenced anymore
        removeCache()

        fcount++
        if (fcount >= framesBetweenGC) {
            fcount = 0
            gc()
        }
    }

    fun gc() {
        delete(referencedBuffersSinceGC.items)
        referencedBuffersSinceGC.clear()
    }

    private fun removeCache() {
    }

    fun delete(buffer: List<AgCachedBuffer>) {
        buffer.fastForEach { delete(it) }
    }

    val empty = Buffer(0)

    fun delete(buffer: AgCachedBuffer) {
        val buf = buffers.getAndRemove(buffer)
        buf?.let {
            it.upload(empty)
            bufferPool.free(buf)
        }
        //buf?.close(list)
    }
}

//class AgCachedBuffer(val kind: AG.BufferKind, val data: Any, val dataOffset: Int = 0, val dataLen: Int = -1)
class AgCachedBuffer(val data: Buffer)
