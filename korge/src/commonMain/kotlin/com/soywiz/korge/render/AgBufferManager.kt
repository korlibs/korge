package com.soywiz.korge.render

import com.soywiz.kds.FastIdentityMap
import com.soywiz.kds.Pool
import com.soywiz.kds.getAndRemove
import com.soywiz.kds.getOrPut
import com.soywiz.kds.iterators.fastForEach
import com.soywiz.kmem.*
import com.soywiz.korag.*

/**
 * Class to handle cached buffers, that are freed after a few frames of not being used
 */
class AgBufferManager {
    private val buffers = FastIdentityMap<Buffer, NAGBuffer>()
    private val referencedBuffersSinceGC = AgFastSet<Buffer>()
    private val bufferPool = Pool {
        //println("CREATE BUFFER")
        NAGBuffer()
    }

    fun getBuffer(cached: Buffer): NAGBuffer {
        referencedBuffersSinceGC.add(cached)
        return buffers.getOrPut(cached) {
            bufferPool.alloc().also {
                it.upload(cached)
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

    fun delete(buffer: List<Buffer>) {
        buffer.fastForEach { delete(it) }
    }

    val empty = Buffer(0)

    fun delete(buffer: Buffer) {
        val buf = buffers.getAndRemove(buffer)
        buf?.let {
            it.upload(empty)
            bufferPool.free(buf)
        }
        //buf?.close(list)
    }
}
