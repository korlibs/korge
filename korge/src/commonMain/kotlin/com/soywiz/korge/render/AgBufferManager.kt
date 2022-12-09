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
class AgBufferManager(
    val ag: AG
) {
    private val buffers = FastIdentityMap<AgCachedBuffer, AGBuffer>()
    private val referencedBuffersSinceGC = AgFastSet<AgCachedBuffer>()
    private val bufferPool = Pool {
        //println("CREATE BUFFER")
        ag.createBuffer()
    }

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
